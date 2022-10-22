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
    Class.class, ParameterizedType.class, GenericArrayType.class, WildcardType.class,
    TypeVariable.class);

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
   * @param <T>      The result type
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
  private static <@NonNull T extends Throwable> void throwUnchecked0(
    @NonNull Throwable t
  ) throws T {
    throw (T) t;
  }

}
