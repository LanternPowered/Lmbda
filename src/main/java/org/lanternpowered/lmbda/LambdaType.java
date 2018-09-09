/*
 * This file is part of Lmbda, licensed under the MIT License (MIT).
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.lanternpowered.lmbda;

import static java.util.Objects.requireNonNull;

import java.lang.invoke.MethodType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Represents a {@link FunctionalInterface}
 * that can be be implemented by a generated function.
 */
public abstract class LambdaType<T> {

    /**
     * Attempts to find a function method in the given functional interface class.
     * <p>A functional interface doesn't need to be annotated with
     * {@link FunctionalInterface}, but only one non default method may
     * be present.
     *
     * @param functionalInterface The functional interface
     * @param <T> The type of the functional interface
     * @return The functional method
     * @throws IllegalArgumentException If no valid functional method could be found
     */
    public static <T> LambdaType<T> of(Class<T> functionalInterface) {
        return new LambdaType<T>(functionalInterface) {};
    }

    /**
     * Attempts to find a function method in the given functional interface type.
     * <p>A functional interface doesn't need to be annotated with
     * {@link FunctionalInterface}, but only one non default method may
     * be present.
     *
     * @param functionalInterfaceType The functional interface type
     * @param <T> The type of the functional interface
     * @return The functional method
     * @throws IllegalArgumentException If no valid functional method could be found
     */
    public static <T> LambdaType<T> of(Type functionalInterfaceType) {
        return new LambdaType<T>(functionalInterfaceType) {};
    }

    private Class<T> functionClass;
    private Method method;

    MethodType classType;
    MethodType methodType;

    /**
     * Constructs a new {@link LambdaType}.
     *
     * <p>The generic signature from the extended class will be
     * used to determine the functional interface to implement.
     * If it's not resolved, a {@link IllegalStateException} can
     * be expected.</p>
     */
    @SuppressWarnings("unchecked")
    public LambdaType() {
        final Class<?> theClass = getClass();
        final Class<?> superClass = theClass.getSuperclass();
        if (superClass != LambdaType.class) {
            throw new IllegalStateException("Only direct subclasses of LambdaType are allowed.");
        }
        final Type superType = theClass.getGenericSuperclass();
        if (!(superType instanceof ParameterizedType)) {
            throw new IllegalStateException("Direct subclasses of LambdaType must be a parameterized type.");
        }
        final ParameterizedType parameterizedType = (ParameterizedType) superType;
        init(parameterizedType.getActualTypeArguments()[0]);
    }

    private LambdaType(Class<T> functionalInterface) {
        init(functionalInterface);
    }

    private LambdaType(Type functionalInterfaceType) {
        init(functionalInterfaceType);
    }

    @SuppressWarnings("unchecked")
    private void init(Type type) {
        final Class<T> interfClass;
        if (type instanceof Class<?>) {
            interfClass = (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            interfClass = (Class<T>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof GenericArrayType) {
            throw new IllegalStateException("The FunctionalInterface type cannot be a GenericArrayType.");
        } else {
            throw new IllegalStateException("The FunctionalInterface type cannot be a TypeVariable.");
        }
        init(interfClass);
    }

    private void init(Class<T> functionalInterface) {
        requireNonNull(functionalInterface, "functionalInterface");
        if (!functionalInterface.isInterface()) throw new IllegalStateException("functionalInterface must be a interface");
        Method validMethod = null;
        for (Method method : functionalInterface.getMethods()) {
            // Ignore default and static methods
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // Only one non implemented method may be present
            if (validMethod != null) {
                throw new IllegalStateException("Found multiple non-default methods in: " +
                        functionalInterface.getClass().getName());
            }
            validMethod = method;
        }
        if (validMethod == null) {
            throw new IllegalStateException("Couldn't find a non-default method in: " +
                    functionalInterface.getClass().getName());
        }
        this.functionClass = functionalInterface;
        this.classType = MethodType.methodType(functionalInterface);
        this.method = validMethod;
        this.methodType = MethodType.methodType(validMethod.getReturnType(), validMethod.getParameterTypes());
    }

    /**
     * Gets the function class that will be implemented.
     *
     * @return The function class
     */
    public Class<T> getFunctionClass() {
        return this.functionClass;
    }

    /**
     * Gets the {@link Method} that will be implemented when
     * generating a function for this {@link LambdaType}.
     *
     * @return The method
     */
    public Method getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return String.format("LambdaType[class=%s,method=%s]",
                this.functionClass.getName(), this.method.getName() + this.methodType);
    }
}
