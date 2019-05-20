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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.invoke.MethodHandles;
import java.security.ProtectionDomain;

/**
 * A class with extension methods related to {@link MethodHandles}.
 */
public final class MethodHandlesExtensions {

    /**
     * Gets a lookup object with full capabilities to emulate all supported bytecode
     * behaviors, including private access, on a target class.
     *
     * <p>If there is a security manager, its checkPermission method is called
     * to check ReflectPermission("suppressAccessChecks").</p>
     *
     * <p>When using Java 9+, see
     * https://docs.oracle.com/javase/9/docs/api/java/lang/invoke/MethodHandles.html#privateLookupIn-java.lang.Class-java.lang.invoke.MethodHandles.Lookup-</p>
     *
     * @param targetClass The target class for which private access should be acquired
     * @param lookup The caller lookup object
     * @return A lookup object for the target class, with private access
     * @throws IllegalAccessException If the lookup doesn't have private access to the target class
     */
    public static MethodHandles.@NonNull Lookup privateLookupIn(@NonNull Class<?> targetClass, MethodHandles.@NonNull Lookup lookup)
            throws IllegalAccessException {
        requireNonNull(targetClass, "targetClass");
        requireNonNull(lookup, "lookup");
        return InternalMethodHandles.adapter.privateLookupIn(targetClass, lookup);
    }

    /**
     * Defines a {@link Class} to the same {@link ClassLoader} and in the same runtime package
     * and {@link ProtectionDomain} as the {@link java.lang.invoke.MethodHandles.Lookup}'s lookup class.
     *
     * @param lookup The lookup of which the target class will be used to define the class in
     * @param byteCode The byte code of the class to define
     * @return The defined class
     * @throws IllegalAccessException If the lookup doesn't have package private access to the target package
     */
    public static @NonNull Class<?> defineClass(MethodHandles.@NonNull Lookup lookup, byte @NonNull[] byteCode) throws IllegalAccessException {
        requireNonNull(lookup, "lookup");
        requireNonNull(byteCode, "byteCode");
        return InternalMethodHandles.adapter.defineClass(lookup, byteCode);
    }

    private MethodHandlesExtensions() {
    }
}
