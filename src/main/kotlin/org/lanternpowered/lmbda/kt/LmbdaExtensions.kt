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
@file:Suppress("unused", "NOTHING_TO_INLINE")

package org.lanternpowered.lmbda.kt

import org.lanternpowered.lmbda.LambdaFactory
import org.lanternpowered.lmbda.LambdaType
import org.lanternpowered.lmbda.MethodHandlesX
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field

/**
 * Constructs a new [LambdaType].
 */
inline fun <reified T> lambdaType() = object: LambdaType<T>() {}

/**
 * See [MethodHandlesX.findFinalStaticSetter].
 */
inline fun MethodHandles.Lookup.findFinalStaticSetter(target: Class<*>, fieldName: String, fieldType: Class<*>): MethodHandle
        = MethodHandlesX.findFinalStaticSetter(this, target, fieldName, fieldType)

/**
 * See [MethodHandlesX.findFinalSetter].
 */
inline fun MethodHandles.Lookup.findFinalSetter(target: Class<*>, fieldName: String, fieldType: Class<*>): MethodHandle
        = MethodHandlesX.findFinalSetter(this, target, fieldName, fieldType)

/**
 * See [MethodHandlesX.unreflectFinalSetter].
 */
inline fun MethodHandles.Lookup.unreflectFinalSetter(field: Field): MethodHandle
        = MethodHandlesX.unreflectFinalSetter(this, field)

/**
 * See [MethodHandlesX.defineClass].
 */
inline fun MethodHandles.Lookup.defineClass(bytecode: ByteArray): Class<*>
        = MethodHandlesX.defineClass(this, bytecode)

/**
 * See [MethodHandlesX.privateLookupIn].
 */
inline fun MethodHandles.Lookup.privateLookupIn(target: Class<*>): MethodHandles.Lookup
        = MethodHandlesX.privateLookupIn(target, this)

/**
 * Constructs a lambda for for the target [MethodHandle] and [LambdaType].
 */
inline fun <T> MethodHandle.createLambda(lambdaType: LambdaType<T>): T = LambdaFactory.create(lambdaType, this)

/**
 * Constructs a lambda for for the target [MethodHandle] and [LambdaType].
 */
inline fun <reified T> MethodHandle.createLambda(): T = LambdaFactory.create(lambdaType<T>(), this)
