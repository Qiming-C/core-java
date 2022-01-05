/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.model;

import io.spine.core.ContractFor;
import io.spine.reflect.J2Kt;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;
import static kotlin.reflect.KVisibility.INTERNAL;

/**
 * The predicate for {@linkplain Modifier access modifiers} of {@linkplain Method methods}.
 */
public final class AccessModifier implements Predicate<Method> {

    public static final AccessModifier PUBLIC =
            onJavaMethod(Modifier::isPublic, "public");

    public static final AccessModifier PROTECTED =
            onJavaMethod(Modifier::isProtected, "protected");

    public static final AccessModifier PACKAGE_PRIVATE =
            onJavaMethod(methodModifier ->
                                 !(isPublic(methodModifier)
                                         || isProtected(methodModifier)
                                         || isPrivate(methodModifier)),
                         "package-private");

    public static final AccessModifier PRIVATE =
            onJavaMethod(Modifier::isPrivate, "private");

    public static final AccessModifier KOTLIN_INTERNAL = new AccessModifier(m -> {
        var kotlinMethod = J2Kt.findKotlinMethod(m);
        if (kotlinMethod.isEmpty()) {
            return false;
        }
        return kotlinMethod.get().getVisibility() == INTERNAL;
    }, "Kotlin internal");

    /**
     * A protected method which overrides a method from a superclass.
     *
     * <p>The method must be declared in a parent class.
     *
     * <p>The purpose of this modifier is to allow inheritance for
     * {@linkplain ContractFor contract methods} without discouraging users with warning logs.
     */
    public static final AccessModifier PROTECTED_CONTRACT = new AccessModifier(
            m -> PROTECTED.test(m) && derivedFromContract(m),
            "protected with @Override"
    );

    /**
     * The predicate which determines if the method has a matching modifier or not.
     */
    private final Predicate<Method> delegate;

    /**
     * The name of the access modifier.
     *
     * <p>Serves for pretty printing.
     */
    private final String name;

    private AccessModifier(Predicate<Method> delegate, String name) {
        this.delegate = delegate;
        this.name = name;
    }

    private static AccessModifier onJavaMethod(IntPredicate flagPredicate, String name) {
        Predicate<Method> predicate = m -> flagPredicate.test(m.getModifiers());
        return new AccessModifier(predicate, name);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    private static boolean derivedFromContract(Method method) {
        var cls = method.getDeclaringClass().getSuperclass();
        while (!cls.equals(Object.class)) {
            var methods = cls.getDeclaredMethods();
            for (var m : methods) {
                if (inheritedMethod(method, m)) {
                    validateContract(m, method);
                    return true;
                }
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    private static void validateContract(Method contract, Method implementation) {
        var annotation = contract.getAnnotation(ContractFor.class);
        if (annotation == null) {
            throw new ModelError(
                    "Handler method `%s` overrides `%s` which is not marked with `@ContractFor`.",
                    implementation, contract
            );
        }
        var target = annotation.handler();
        if (!implementation.isAnnotationPresent(target)) {
            throw new ModelError(
                    "Handler method `%s` overrides the contract `%s` but is not marked with `@%s`.",
                    implementation, contract, target.getSimpleName()
            );
        }
    }

    @SuppressWarnings("RedundantExplicitVariableType" /* Due to the regression bug in PMD.
                                                    See https://github.com/pmd/pmd/issues/2976, */)
    private static boolean inheritedMethod(Method child, Method parent) {
        if (!parent.getName().equals(child.getName())) {
            return false;
        }
        Class<?>[] parentParams = parent.getParameterTypes();
        Class<?>[] childParams = child.getParameterTypes();
        if (parentParams.length != childParams.length) {
            return false;
        }
        for (var i = 0; i < parentParams.length; i++) {
            var parentParam = parentParams[i];
            var childParam = childParams[i];
            if (!parentParam.isAssignableFrom(childParam)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Obtains the access modifier of the given method.
     *
     * @param method
     *         the method to analyze
     * @return the access modifier of the given method
     */
    static AccessModifier fromMethod(Method method) {
        checkNotNull(method);
        var matchedModifier = Stream
                .of(PRIVATE,
                    PACKAGE_PRIVATE,
                    PROTECTED_CONTRACT,
                    PROTECTED,
                    KOTLIN_INTERNAL,
                    PUBLIC)
                .filter(modifier -> modifier.test(method))
                .findFirst()
                .orElseThrow(() -> newIllegalArgumentException(
                        "Could not determine the access level of the method `%s`.",
                        method
                ));
        return matchedModifier;
    }

    /**
     * Checks whether the method is of the modifier determined by {@code this} instance.
     *
     * @param method
     *         the method to check
     * @return {@code true} if the method is declared with the expected modifier,
     *         {@code false} otherwise.
     */
    @Override
    public boolean test(Method method) {
        return delegate.test(method);
    }

    @Override
    public String toString() {
        return name;
    }
}
