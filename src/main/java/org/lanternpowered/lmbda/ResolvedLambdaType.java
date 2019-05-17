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

import java.lang.invoke.MethodType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Represents a resolved lambda type.
 *
 * @param <T> The type
 */
@SuppressWarnings({"unchecked", "ConstantConditions"})
final class ResolvedLambdaType<@NonNull T> {

    final Class<T> functionClass;
    final @Nullable ParameterizedType genericFunctionType;

    final Method method;
    final MethodType methodType;

    ResolvedLambdaType(Type type) {
        final Class<T> interfClass;
        final ParameterizedType genericFunctionType;
        if (type instanceof Class<?>) {
            genericFunctionType = null;
            interfClass = (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            genericFunctionType = (ParameterizedType) type;
            interfClass = (Class<T>) genericFunctionType.getRawType();
        } else {
            final String name;
            if (type instanceof GenericArrayType) {
                name = "GenericArrayType";
            } else if (type instanceof WildcardType) {
                name = "WildcardType";
            } else if (type instanceof TypeVariable) {
                name = "TypeVariable";
            } else {
                name = type.getClass().getName();
            }
            throw new IllegalStateException("A " + name + " can't be a LambdaType.");
        }
        if (!interfClass.isInterface()) throw new IllegalStateException("functionalInterface must be a interface");
        Method validMethod = null;
        for (Method method : interfClass.getMethods()) {
            // Ignore default and static methods
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // Only one non implemented method may be present
            if (validMethod != null) {
                throw new IllegalStateException("Found multiple non-default methods in: " +
                        interfClass.getClass().getName());
            }
            validMethod = method;
        }
        if (validMethod == null) {
            throw new IllegalStateException("Couldn't find a non-default method in: " +
                    interfClass.getClass().getName());
        }
        this.functionClass = interfClass;
        this.genericFunctionType = genericFunctionType;
        this.method = validMethod;
        this.methodType = MethodType.methodType(validMethod.getReturnType(), validMethod.getParameterTypes());
    }

    @Override
    public String toString() {
        final String typeName = this.genericFunctionType != null ? this.genericFunctionType.getTypeName() : this.functionClass.getName();
        return String.format("LambdaType[type=%s,method=%s]",
                typeName, this.method.getName() + this.methodType);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LambdaType)) {
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
