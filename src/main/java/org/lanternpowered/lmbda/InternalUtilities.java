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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;

final class InternalUtilities {

    private static final @NonNull List<@NonNull Class<?>> javaTypeSubclasses = Arrays.asList(
            Class.class, ParameterizedType.class, GenericArrayType.class, WildcardType.class, TypeVariable.class);

    /**
     * Gets a readable class name to the given {@link Type}.
     *
     * @param type The type
     * @return The readable name
     */
    static @NonNull String getTypeClassName(@NonNull Type type) {
        return javaTypeSubclasses.stream()
                .filter(subclass -> subclass.isInstance(type))
                .map(Class::getSimpleName).findFirst()
                .orElseGet(() -> type.getClass().getName());
    }

    /**
     * Gets the package name for the given {@link Class}.
     *
     * @param theClass The class to get the package for
     * @return The package name
     */
    static @NonNull String getPackageName(@NonNull Class<?> theClass) {
        Class<?> target = theClass;
        while (target.isArray()) {
            target = target.getComponentType();
        }
        if (target.isPrimitive()) {
            return "java.lang";
        }
        return getPackageName(target.getName());
    }

    /**
     * Gets the package name for the given class name.
     *
     * @param className The class name
     * @return The package name
     */
    static @NonNull String getPackageName(@NonNull String className) {
        final int index = className.lastIndexOf('.');
        return index == -1 ? "" : className.substring(0, index);
    }

    /**
     * Performs a unchecked action.
     *
     * @param supplier The supplier
     * @param <T> The result type
     * @return The result
     */
    static <@Nullable T> T doUnchecked(@NonNull ThrowableSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            throw throwUnchecked(t);
        }
    }

    /**
     * Represents a supplier that may end with a exception.
     *
     * @param <T> The result type
     */
    @FunctionalInterface
    interface ThrowableSupplier<@Nullable T> {

        T get() throws Throwable;
    }

    /**
     * Throws the {@link Throwable} as an unchecked exception.
     *
     * @param t The throwable to throw
     * @return A runtime exception
     */
    static @NonNull RuntimeException throwUnchecked(@NonNull Throwable t) {
        throwUnchecked0(t);
        throw new AssertionError("Unreachable.");
    }

    @SuppressWarnings("unchecked")
    private static <@NonNull T extends Throwable> void throwUnchecked0(@NonNull Throwable t) throws T {
        throw (T) t;
    }

}
