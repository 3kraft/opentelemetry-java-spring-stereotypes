/*
 * Copyright The OpenTelemetry Authors
 * Copyright 3kraft IT GmbH & Co KG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dreikraft.otel.javaagent.instrumentation.springstereotypes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class SpringStereotypeInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return ElementMatchers.inheritsAnnotation(hasSuperType(named("org.springframework.stereotype.Service")))
        .or(ElementMatchers.inheritsAnnotation(hasSuperType(named("org.springframework.stereotype.Component"))));
  }

  @Override
  public void transform(TypeTransformer typeTransformer) {
    System.out.println("transform " + typeTransformer);
    typeTransformer.applyAdviceToMethod(
        ElementMatchers.isMethod().and(ElementMatchers.isPublic()), this.getClass().getName() + "$SpringStereotypeAdvice");
  }

  @SuppressWarnings("unused")
  public static class SpringStereotypeAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method originMethod,
        @Advice.Local("otelMethod") Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      // Every usage of @Advice.Origin Method is replaced with a call to Class.getMethod, copy it
      // to local variable so that there would be only one call to Class.getMethod.
      method = originMethod;

      final Instrumenter<Method, Object> instrumenter = SpringStereotypeSingletons.instrumenter();
      final Context current = Java8BytecodeBridge.currentContext();

      if (instrumenter.shouldStart(current, method)) {
        context = instrumenter.start(current, method);
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Local("otelMethod") Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returnValue,
        @Advice.Thrown Throwable throwable) {
      if (scope == null) {
        return;
      }
      scope.close();
      SpringStereotypeSingletons.instrumenter().end(context, method, returnValue, throwable);
    }
  }

  public static class MethodRequest {
    private final Method method;
    private final Object[] args;

    public MethodRequest(Method method, Object[] args) {
      this.method = method;
      this.args = args;
    }

    public Method method() {
      return this.method;
    }

    public Object[] args() {
      return this.args;
    }
  }

  public static class SpanNames {

    private static final Cache<Class<?>, Map<String, String>> spanNameCaches = Cache.weak();

    /**
     * This method is used to generate a span name based on a method. Anonymous classes are named
     * based on their parent.
     */
    public static String fromMethod(Method method) {
      return fromMethod(method.getDeclaringClass(), method.getName());
    }

    /**
     * This method is used to generate a span name based on a method. Anonymous classes are named
     * based on their parent.
     */
    public static String fromMethod(Class<?> clazz, String methodName) {
      // the cache (ConcurrentHashMap) is naturally bounded by the number of methods in a class
      Map<String, String> spanNameCache =
          spanNameCaches.computeIfAbsent(clazz, c -> new ConcurrentHashMap<>());

      // not using computeIfAbsent, because it would require a capturing (allocating) lambda
      String spanName = spanNameCache.get(methodName);
      if (spanName != null) {
        return spanName;
      }
      spanName = ClassNames.simpleName(clazz) + "." + methodName;
      spanNameCache.put(methodName, spanName);
      return spanName;
    }

    private SpanNames() {}
  }

  public static class ClassNames {

    private static final Cache<Class<?>, String> simpleNames = Cache.weak();

    /**
     * Returns a simple class name based on a given class reference, e.g. for use in span names and
     * attributes. Anonymous classes are named based on their parent.
     */
    public static String simpleName(Class<?> type) {
      return simpleNames.computeIfAbsent(type, ClassNames::computeSimpleName);
    }

    private static String computeSimpleName(Class<?> type) {
      if (!type.isAnonymousClass()) {
        return type.getSimpleName();
      }
      String className = type.getName();
      if (type.getPackage() != null) {
        String pkgName = type.getPackage().getName();
        if (!pkgName.isEmpty()) {
          className = className.substring(pkgName.length() + 1);
        }
      }
      return className;
    }

    private ClassNames() {}
  }
}
