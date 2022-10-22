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

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SuppressWarnings("unchecked")
final class JavaLambdaFactory {

  /**
   * Creates a new lambda implementation using the java lambda metafactory. Only supports
   * "method" method handles.
   *
   * @param lambdaType   The lambda type of the function interface to implement
   * @param lookup       The caller lookup
   * @param methodHandle The method handle to target
   * @param <T>          The lambda type
   * @return The lambda function instance
   */
  static <@NonNull T> T create(
    final @NonNull LambdaType<T> lambdaType,
    final MethodHandles.@NonNull Lookup lookup,
    final @NonNull MethodHandle methodHandle
  ) throws Throwable {
    // Generate the lambda class
    final CallSite callSite = LambdaMetafactory.metafactory(lookup,
      lambdaType.getMethod().getName(), MethodType.methodType(lambdaType.resolved.functionClass),
      lambdaType.resolved.methodType, methodHandle, methodHandle.type());

    // Create the function
    return (T) callSite.getTarget().invoke();
  }

  private JavaLambdaFactory() {
  }
}
