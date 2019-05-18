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
@file:Suppress("unused", "NOTHING_TO_INLINE", "DeprecatedCallableAddReplaceWith", "UNCHECKED_CAST")

package org.lanternpowered.lmbda.kt

import org.lanternpowered.lmbda.LambdaFactory
import org.lanternpowered.lmbda.LambdaType
import org.lanternpowered.lmbda.MethodHandlesExtensions
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Constructs a new [LambdaType].
 */
inline fun <reified T> lambdaType() = object : LambdaType<T>() {}

/**
 * See [MethodHandlesExtensions.defineClass].
 */
@Throws(IllegalAccessException::class)
inline fun MethodHandles.Lookup.defineClass(bytecode: ByteArray): Class<*>
        = MethodHandlesExtensions.defineClass(this, bytecode)

/**
 * See [MethodHandlesExtensions.privateLookupIn].
 */
@Throws(IllegalAccessException::class)
inline fun MethodHandles.Lookup.privateLookupIn(target: Class<*>): MethodHandles.Lookup
        = MethodHandlesExtensions.privateLookupIn(target, this)

/**
 * See [MethodHandlesExtensions.privateLookupIn].
 */
@Throws(IllegalAccessException::class)
inline fun MethodHandles.Lookup.privateLookupIn(target: KClass<*>): MethodHandles.Lookup
        = MethodHandlesExtensions.privateLookupIn(target.java, this)

/**
 * See [MethodHandlesExtensions.privateLookupIn].
 */
@Throws(IllegalAccessException::class)
inline fun <reified T> MethodHandles.Lookup.privateLookupIn(): MethodHandles.Lookup
        = MethodHandlesExtensions.privateLookupIn(T::class.java, this)

/**
 * Constructs a lambda for for the target [MethodHandle] and [LambdaType].
 */
inline fun <T> MethodHandle.createLambda(lambdaType: LambdaType<T>): T = LambdaFactory.create(lambdaType, this)

/**
 * Constructs a lambda for for the target [MethodHandle] and [LambdaType].
 */
inline fun <reified T> MethodHandle.createLambda(): T = LambdaFactory.create(lambdaType<T>(), this)

/**
 * Constructs a lambda for for the target [MethodHandle] and [LambdaType].
 */
inline fun <reified T> MethodHandle.createLambda(defineLookup: MethodHandles.Lookup): T
        = LambdaFactory.create(lambdaType<T>().defineClassesWith(defineLookup), this)

/**
 * Attempts to convert this [Type] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T> Type.toLambdaType(): LambdaType<T> = LambdaType.of(this)

/**
 * Attempts to convert this [Class] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T> Class<T>.toLambdaType(): LambdaType<T> = LambdaType.of(this)

/**
 * Attempts to convert this [KType] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> KType.toLambdaType(): LambdaType<T>
        = (this.classifier as? KClass<T>)?.toLambdaType() ?: throw IllegalStateException("The classifier must be a KClass.")

/**
 * Attempts to convert this [KClass] into [LambdaType].
 *
 * @see LambdaType.of
 */
inline fun <T : Any> KClass<T>.toLambdaType(): LambdaType<T> = LambdaType.of(this.java)

@Deprecated(message = "GenericArrayType isn't supported.")
inline fun <T> GenericArrayType.toLambdaType(): LambdaType<T>
        = throw UnsupportedOperationException("A GenericArrayType can't be a LambdaType.")

@Deprecated(message = "WildcardType isn't supported.")
inline fun <T> WildcardType.toLambdaType(): LambdaType<T>
        = throw UnsupportedOperationException("A WildcardType can't be a LambdaType.")

@Deprecated(message = "TypeVariable isn't supported.")
inline fun <T> TypeVariable<*>.toLambdaType(): LambdaType<T>
        = throw UnsupportedOperationException("A TypeVariable can't be a LambdaType.")
