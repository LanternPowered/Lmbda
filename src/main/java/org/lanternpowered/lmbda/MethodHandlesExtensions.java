/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
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
   * Gets a lookup object with full capabilities to emulate all supported bytecode behaviors,
   * including private access, on a target class.
   *
   * <p>If there is a security manager, its checkPermission method is called to check
   * ReflectPermission("suppressAccessChecks").</p>
   *
   * <p>When using Java 9+, see
   * https://docs.oracle.com/javase/9/docs/api/java/lang/invoke/MethodHandles.html#privateLookupIn-java.lang.Class-java.lang.invoke.MethodHandles.Lookup-</p>
   *
   * @param targetClass The target class for which private access should be acquired
   * @param lookup      The caller lookup object
   * @return A lookup object for the target class, with private access
   * @throws IllegalAccessException If the lookup doesn't have private access to the target class
   */
  public static MethodHandles.@NonNull Lookup privateLookupIn(
    final @NonNull Class<?> targetClass,
    final MethodHandles.@NonNull Lookup lookup
  ) throws IllegalAccessException {
    requireNonNull(targetClass, "targetClass");
    requireNonNull(lookup, "lookup");
    return InternalMethodHandles.adapter.privateLookupIn(targetClass, lookup);
  }

  /**
   * Defines a {@link Class} to the same {@link ClassLoader} and in the same runtime package and
   * {@link ProtectionDomain} as the {@link java.lang.invoke.MethodHandles.Lookup}'s lookup class.
   *
   * @param lookup   The lookup of which the target class will be used to define the class in
   * @param byteCode The byte code of the class to define
   * @return The defined class
   * @throws IllegalAccessException If the lookup doesn't have package private access to the
   * target package
   */
  public static @NonNull Class<?> defineClass(
    final MethodHandles.@NonNull Lookup lookup,
    final byte @NonNull [] byteCode
  ) throws IllegalAccessException {
    requireNonNull(lookup, "lookup");
    requireNonNull(byteCode, "byteCode");
    return InternalMethodHandles.adapter.defineClass(lookup, byteCode);
  }

  private MethodHandlesExtensions() {
  }
}
