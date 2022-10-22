/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:Suppress("unused", "NOTHING_TO_INLINE", "DeprecatedCallableAddReplaceWith", "UNCHECKED_CAST")

package org.lanternpowered.lmbda.kt

import org.lanternpowered.lmbda.LambdaType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Constructs a new [LambdaType].
 */
inline fun <reified T : Any> lambdaType() = object : LambdaType<T>() {}

/**
 * Attempts to convert this [Type] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> Type.toLambdaType(): LambdaType<T> = LambdaType.of(this)

/**
 * Attempts to convert this [Class] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> Class<T>.toLambdaType(): LambdaType<T> = LambdaType.of(this)

/**
 * Attempts to convert this [KType] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> KType.toLambdaType(): LambdaType<T> =
  (this.classifier as? KClass<T>)?.toLambdaType()
    ?: throw IllegalStateException("The classifier must be a KClass.")

/**
 * Attempts to convert this [KClass] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> KClass<T>.toLambdaType(): LambdaType<T> = LambdaType.of(this.java)

/**
 * It's not supported to convert a [GenericArrayType] to a [LambdaType].
 */
@Deprecated(message = "GenericArrayType isn't supported.")
inline fun <T : Any> GenericArrayType.toLambdaType(): LambdaType<T> =
  throw UnsupportedOperationException("A GenericArrayType can't be a LambdaType.")

/**
 * It's not supported to convert a [WildcardType] to a [LambdaType].
 */
@Deprecated(message = "WildcardType isn't supported.")
inline fun <T : Any> WildcardType.toLambdaType(): LambdaType<T> =
  throw UnsupportedOperationException("A WildcardType can't be a LambdaType.")

/**
 * It's not supported to convert a [TypeVariable] to a [LambdaType].
 */
@Deprecated(message = "TypeVariable isn't supported.")
inline fun <T : Any> TypeVariable<*>.toLambdaType(): LambdaType<T> =
  throw UnsupportedOperationException("A TypeVariable can't be a LambdaType.")
