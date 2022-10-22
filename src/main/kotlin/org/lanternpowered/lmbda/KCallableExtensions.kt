/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
@file:JvmName(" KFunctionExtensions")
@file:Suppress("NOTHING_TO_INLINE")

package org.lanternpowered.lmbda

import org.lanternpowered.lmbda.mh.privateLookupIn
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaSetter

/**
 * Constructs a lambda for the target [KCallable] and [LambdaType].
 *
 * If the target callables have access restrictions like private accessors, it is necessary to
 * provide a [MethodHandles.Lookup] that has access.
 */
inline fun <reified T : Any> KCallable<*>.createLambda(
  lookup: MethodHandles.Lookup = MethodHandles.lookup()
): T = createLambda(lambdaType(), lookup)

/**
 * Constructs a lambda for the target [KCallable] and [LambdaType].
 *
 * If the target callables have access restrictions like private accessors, it is necessary to
 * provide a [MethodHandles.Lookup] that has access.
 */
inline fun <T : Any> KCallable<*>.createLambda(
  lambdaType: LambdaType<T>,
  lookup: MethodHandles.Lookup = MethodHandles.lookup()
): T = createLambdaWithLookup(lambdaType, lookup)

@PublishedApi
internal fun <T : Any> KCallable<*>.createLambdaWithLookup(
  lambdaType: LambdaType<T>,
  lookup: MethodHandles.Lookup
): T {
  var methodHandle: MethodHandle? = null
  var exception: IllegalAccessException? = null
  try {
    methodHandle = toMethodHandle(lookup)
  } catch (ex: IllegalAccessException) {
    exception = ex
  }
  if (methodHandle == null) {
    try {
      // not enough access, try again with a private lookup in the declaring class,
      // if we have enough access to create a private lookup
      methodHandle = toMethodHandle(lookup.privateLookupIn(toDeclaringClass()))
    } catch (_: IllegalAccessException) {
    }
  }
  if (methodHandle == null)
    throw exception!!
  return methodHandle.createLambda(lambdaType)
}

private fun KCallable<*>.toDeclaringClass(): Class<*> {
  if (this is KProperty.Getter<*>) {
    val declaringClass = property.toGetterDeclaringClass()
    if (declaringClass != null)
      return declaringClass
  } else if (this is KMutableProperty.Setter<*>) {
    val property = property as KMutableProperty<*>
    val javaSetter = property.javaSetter
    if (javaSetter != null)
      return javaSetter.declaringClass
    val javaField = property.javaField
    if (javaField != null)
      return javaField.declaringClass
  } else if (this is KFunction<*>) {
    val javaMethod = javaMethod
    if (javaMethod != null)
      return javaMethod.declaringClass
    val javaConstructor = javaConstructor
    if (javaConstructor != null)
      return javaConstructor.declaringClass
  } else if (this is KProperty<*>) {
    val declaringClass = toGetterDeclaringClass()
    if (declaringClass != null)
      return declaringClass
  }
  throw IllegalStateException("Unable to get declaring Class for KCallable: $this")
}

private fun KProperty<*>.toGetterDeclaringClass(): Class<*>? {
  val javaGetter = javaGetter
  if (javaGetter != null)
    return javaGetter.declaringClass
  val javaField = javaField
  if (javaField != null)
    return javaField.declaringClass
  return null
}

private fun KCallable<*>.toMethodHandle(lookup: MethodHandles.Lookup): MethodHandle {
  if (this is KProperty.Getter<*>) {
    val methodHandle = property.toGetterMethodHandle(lookup)
    if (methodHandle != null)
      return methodHandle
  } else if (this is KMutableProperty.Setter<*>) {
    val property = property as KMutableProperty<*>
    val javaSetter = property.javaSetter
    if (javaSetter != null)
      return lookup.unreflect(javaSetter)
    val javaField = property.javaField
    if (javaField != null)
      return lookup.unreflectSetter(javaField)
  } else if (this is KFunction<*>) {
    val javaMethod = javaMethod
    if (javaMethod != null)
      return lookup.unreflect(javaMethod)
    val javaConstructor = javaConstructor
    if (javaConstructor != null)
      return lookup.unreflectConstructor(javaConstructor)
  } else if (this is KProperty<*>) {
    val methodHandle = toGetterMethodHandle(lookup)
    if (methodHandle != null)
      return methodHandle
  }
  throw IllegalStateException("Unable to get MethodHandle for KCallable: $this")
}

private fun KProperty<*>.toGetterMethodHandle(lookup: MethodHandles.Lookup): MethodHandle? {
  val javaGetter = javaGetter
  if (javaGetter != null)
    return lookup.unreflect(javaGetter)
  val javaField = javaField
  if (javaField != null)
    return lookup.unreflectGetter(javaField)
  return null
}
