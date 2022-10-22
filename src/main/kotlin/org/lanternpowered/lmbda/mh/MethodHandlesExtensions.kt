/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:JvmName(">MethodHandlesExtensions")
@file:Suppress("unused", "NOTHING_TO_INLINE", "DeprecatedCallableAddReplaceWith")

package org.lanternpowered.lmbda.mh

import org.lanternpowered.lmbda.MethodHandlesExtensions
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass

/**
 * See [MethodHandlesExtensions.defineClass].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Throws(IllegalAccessException::class)
inline fun MethodHandles.Lookup.defineClass(bytecode: ByteArray): Class<*> =
  MethodHandlesExtensions.defineClass(this, bytecode)

/**
 * See [MethodHandlesExtensions.privateLookupIn].
 */
@Throws(IllegalAccessException::class)
inline fun MethodHandles.Lookup.privateLookupIn(target: Class<*>): MethodHandles.Lookup =
  MethodHandlesExtensions.privateLookupIn(target, this)

/**
 * See [MethodHandlesExtensions.privateLookupIn].
 */
@Throws(IllegalAccessException::class)
inline fun MethodHandles.Lookup.privateLookupIn(target: KClass<*>): MethodHandles.Lookup =
  MethodHandlesExtensions.privateLookupIn(target.java, this)

/**
 * See [MethodHandlesExtensions.privateLookupIn].
 */
@Throws(IllegalAccessException::class)
inline fun <reified T> MethodHandles.Lookup.privateLookupIn(): MethodHandles.Lookup =
  MethodHandlesExtensions.privateLookupIn(T::class.java, this)
