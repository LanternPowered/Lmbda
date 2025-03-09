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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;

final class InternalUtilities {

  private static final List<Class<?>> javaTypeSubclasses = Arrays.asList(
    Class.class, ParameterizedType.class, GenericArrayType.class, WildcardType.class,
    TypeVariable.class);

  /**
   * Gets a readable class name to the given {@link Type}.
   *
   * @param type The type
   * @return The readable name
   */
  static String getTypeClassName(Type type) {
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
  static String getPackageName(Class<?> theClass) {
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
  static String getPackageName(String className) {
    int index = className.lastIndexOf('.');
    return index == -1 ? "" : className.substring(0, index);
  }
}
