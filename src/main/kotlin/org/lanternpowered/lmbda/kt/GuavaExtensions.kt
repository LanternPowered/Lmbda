/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:Suppress("NOTHING_TO_INLINE", "UnstableApiUsage")

package org.lanternpowered.lmbda.kt

import com.google.common.reflect.TypeToken
import org.lanternpowered.lmbda.LambdaFactory
import org.lanternpowered.lmbda.LambdaType
import java.lang.invoke.MethodHandle

/**
 * Attempts to convert this [TypeToken] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> TypeToken<T>.toLambdaType(): LambdaType<T> = LambdaType.of(this.type)

/**
 * Constructs a lambda for the target [MethodHandle] and [TypeToken].
 */
inline fun <T : Any> MethodHandle.createLambda(typeToken: TypeToken<T>): T =
  LambdaFactory.create(typeToken.toLambdaType(), this)
