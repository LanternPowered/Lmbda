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

import org.lanternpowered.lmbda.MethodHandlesExtensions
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass

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
