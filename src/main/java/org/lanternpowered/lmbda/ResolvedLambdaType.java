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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a resolved lambda type.
 *
 * @param <T> The type
 */
@SuppressWarnings("unchecked")
final class ResolvedLambdaType<@NonNull T> {

    final @NonNull Class<T> functionClass;
    final @Nullable ParameterizedType genericFunctionType;

    final @NonNull Method method;
    final @NonNull MethodType methodType;

    ResolvedLambdaType(@NonNull Type type) {
        final Class<T> functionClass;
        final ParameterizedType genericFunctionType;
        if (type instanceof Class<?>) {
            genericFunctionType = null;
            functionClass = (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            genericFunctionType = (ParameterizedType) type;
            functionClass = (Class<T>) genericFunctionType.getRawType();
        } else {
            throw new IllegalStateException("A " + InternalUtilities.getTypeClassName(type) + " can't be a LambdaType.");
        }
        this.method = validateClassAndFindMethod(functionClass);
        this.methodType = MethodType.methodType(this.method.getReturnType(), this.method.getParameterTypes());
        this.functionClass = functionClass;
        this.genericFunctionType = genericFunctionType;
    }

    /**
     * Validates the function class and attempts to find a valid
     * {@link Method} that can be implemented it.
     *
     * @param functionClass The function class
     * @return The function method
     */
    private static @NonNull Method validateClassAndFindMethod(@NonNull Class<?> functionClass) {
        if (Modifier.isPrivate(functionClass.getModifiers())) {
            throw new IllegalStateException("A function class may not be private.");
        }
        if (functionClass.isInterface()) {
            return findInterfaceMethod(functionClass);
        } else if (Modifier.isAbstract(functionClass.getModifiers())) {
            if (functionClass.getEnclosingClass() != null && !Modifier.isStatic(functionClass.getModifiers())) {
                throw new IllegalStateException("A abstract function class may not be a non-static inner class.");
            }
            validateConstructors(functionClass);
            return findAbstractClassMethod(functionClass);
        } else {
            throw new IllegalStateException("The function type must be a interface or a abstract class.");
        }
    }

    /**
     * Validates the {@link Constructor}s of the function class.
     *
     * @param functionClass The function class
     */
    private static void validateConstructors(@NonNull Class<?> functionClass) {
        // For a abstract class is at least a package private constructor required
        final Constructor<?>[] constructors = functionClass.getDeclaredConstructors();
        boolean found = false;
        for (Constructor<?> constructor : constructors) {
            // No arguments for this constructor
            if (constructor.getParameterCount() == 0) {
                if (Modifier.isPrivate(constructor.getModifiers())) {
                    throw new IllegalStateException("The zero arg constructor of a abstract function class must be at least package-private.");
                }
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalStateException("A abstract function class must have a zero arg constructor.");
        }
    }

    /**
     * Attempts to find a valid {@link Method} that can be
     * implemented for the abstract function class.
     *
     * @param functionClass The function class
     * @return The function method
     */
    private static @NonNull Method findAbstractClassMethod(@NonNull Class<?> functionClass) {
        final Map<String, Method> foundMethods = new HashMap<>();
        findClassMethods(functionClass, foundMethods);

        final List<Method> methods = foundMethods.values().stream()
                .filter(method -> !Modifier.isStatic(method.getModifiers()) && Modifier.isAbstract(method.getModifiers()))
                .collect(Collectors.toList());

        if (methods.size() > 1) {
            throw new IllegalStateException("Found multiple abstract methods in: " +
                    functionClass.getName());
        } else if (methods.isEmpty()) {
            throw new IllegalStateException("Couldn't find a abstract method in: " +
                    functionClass.getName());
        }

        return methods.get(0);
    }

    /**
     * Converts the {@link Method} to a string with its unique descriptor and name.
     *
     * @param method The method
     * @return The string
     */
    private static @NonNull String toKey(@NonNull Method method) {
        return method.getName() + ';' + org.objectweb.asm.Type.getMethodDescriptor(method);
    }

    /**
     * Finds all the methods that are defined in the function class and its superclasses or
     * interfaces. Methods with the same signature and name which are defined in a superclass
     * will be skipped. Only one {@link Method} with a specific signature and name will be
     * present in the resulting map.
     *
     * @param functionClass The function class
     * @param methods The method map
     */
    private static void findClassMethods(@NonNull Class<?> functionClass, @NonNull Map<String, Method> methods) {
        for (Method method : functionClass.getDeclaredMethods()) {
            methods.putIfAbsent(toKey(method), method);
        }
        for (Class<?> interf : functionClass.getInterfaces()) {
            findClassMethods(interf, methods);
        }
        final Class<?> superclass = functionClass.getSuperclass();
        if (superclass != Object.class) {
            findClassMethods(superclass, methods);
        }
    }

    /**
     * Attempts to find a valid {@link Method} that can be
     * implemented for the function interface.
     *
     * @param functionClass The function class
     * @return The function method
     */
    private static @NonNull Method findInterfaceMethod(@NonNull Class<?> functionClass) {
        Method validMethod = null;
        for (Method method : functionClass.getMethods()) {
            // Ignore default and static methods
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // Only one non implemented method may be present
            if (validMethod != null) {
                throw new IllegalStateException("Found multiple non-default methods in: " +
                        functionClass.getName());
            }
            validMethod = method;
        }
        if (validMethod == null) {
            throw new IllegalStateException("Couldn't find a non-default method in: " +
                    functionClass.getName());
        }
        return validMethod;
    }

    /**
     * Gets the function type.
     *
     * @return The function type
     */
    @NonNull Type getFunctionType() {
        return this.genericFunctionType != null ? this.genericFunctionType : this.functionClass;
    }

    /**
     * Gets a copy of the {@link Method} object.
     *
     * <p>This way we can prevent modified access through {@link Method#setAccessible(boolean)}
     * to be shared by everything that accesses the method.</p>
     *
     * @return The method copy
     */
    @NonNull Method getMethodCopy() {
        // Find the same method object in the declaring class
        final Class<?>[] parameters = this.method.getParameterTypes();
        for (Method method : this.method.getDeclaringClass().getDeclaredMethods()) {
            if (method.getName().equals(this.method.getName()) &&
                    method.getReturnType().equals(this.method.getReturnType()) &&
                    method.getParameterCount() == parameters.length &&
                    Arrays.equals(method.getParameterTypes(), parameters)) {
                return method;
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public @NonNull String toString() {
        return String.format("LambdaType[type=%s,method=%s]", getFunctionType().getTypeName(), this.method.getName() + this.methodType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ResolvedLambdaType)) {
            return false;
        }
        final ResolvedLambdaType<?> that = (ResolvedLambdaType<?>) obj;
        return that.method.equals(this.method) && that.functionClass == this.functionClass &&
                Objects.equals(this.genericFunctionType, that.genericFunctionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.functionClass, this.method, this.genericFunctionType);
    }
}
