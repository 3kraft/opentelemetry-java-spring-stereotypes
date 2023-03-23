/*
 * Copyright The OpenTelemetry Authors
 * Copyright 3kraft IT GmbH & Co KG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dreikraft.otel.javaagent.instrumentation.springstereotypes;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * This is a demo instrumentation instrumenting @Component or @Service annotated
 * classes.
 */
@AutoService(InstrumentationModule.class)
public final class SpringStereotypeInstrumentationModule extends InstrumentationModule {

    public SpringStereotypeInstrumentationModule() {
        super("spring-stereotypes", "spring-stereotypes-5.0");
    }

    @Override
    public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
        return hasClassesNamed("org.springframework.stereotype.Service")
                .or(hasClassesNamed("org.springframework.stereotype.Component"));
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return singletonList(new SpringStereotypeInstrumentation());
    }

    @Override
    public List<String> getAdditionalHelperClassNames() {
        return Collections.singletonList("com.dreikraft.otel.javaagent.instrumentation.springstereotypes.SpringStereotypeSingletons");
    }

    @Override
    public boolean isHelperClass(String className) {
        return className.startsWith("com.dreikraft.otel.javaagent.instrumentation.springstereotypes");
    }

}
