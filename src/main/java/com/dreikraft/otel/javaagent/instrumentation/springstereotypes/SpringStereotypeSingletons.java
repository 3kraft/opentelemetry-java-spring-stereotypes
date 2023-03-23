package com.dreikraft.otel.javaagent.instrumentation.springstereotypes;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.lang.reflect.Method;

/**
 * @author florianbruckner
 */
public final class SpringStereotypeSingletons {

  private static final String INSTRUMENTATION_NAME =
      "com.dreikraft.opentelemetry.javaagent.instrumentation.springstereotypes-5.0";

  private static final Instrumenter<Method, Object> INSTRUMENTER = createInstrumenter();

  public static Instrumenter<Method, Object> instrumenter() {
    return INSTRUMENTER;
  }

  private static Instrumenter<Method, Object> createInstrumenter() {
    return Instrumenter.builder(GlobalOpenTelemetry.get(),
            INSTRUMENTATION_NAME,
            SpringStereotypeSingletons::spanNameFromMethod)
        // .addAttributesExtractor(CodeAttributesExtractor.create(MethodCodeAttributesGetter.INSTANCE))
        .buildInstrumenter(SpringStereotypeSingletons::spanKindFromMethod);
  }

  private static SpanKind spanKindFromMethod(Method method) {
    return SpanKind.INTERNAL;
  }

  private static String spanNameFromMethod(Method method) {
    return SpringStereotypeInstrumentation.SpanNames.fromMethod(method);
  }
  
  private SpringStereotypeSingletons() {}
}
