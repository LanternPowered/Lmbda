/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:Suppress("unused", "NOTHING_TO_INLINE", "DeprecatedCallableAddReplaceWith")

package org.lanternpowered.lmbda.kt

import org.lanternpowered.lmbda.LambdaFactory
import org.lanternpowered.lmbda.LambdaType
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

/**
 * Constructs a lambda for the target [MethodHandle] and [LambdaType].
 */
inline fun <T : Any> MethodHandle.createLambda(lambdaType: LambdaType<T>): T =
  LambdaFactory.create(lambdaType, this)

/**
 * Constructs a lambda for the target [MethodHandle] and [LambdaType].
 */
inline fun <reified T : Any> MethodHandle.createLambda(): T =
  LambdaFactory.create(lambdaType(), this)

/**
 * Constructs a lambda for the target [MethodHandle] and [LambdaType].
 */
inline fun <reified T : Any> MethodHandle.createLambda(defineLookup: MethodHandles.Lookup): T =
  LambdaFactory.create(lambdaType<T>().defineClassesWith(defineLookup), this)
