/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package org.lanternpowered.lmbda.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.lanternpowered.lmbda.createLambda
import java.lang.invoke.MethodHandles
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

class LambdaKFunctionTest {

  @Test fun testGetter() {
    val kFunction = TestObject::data
    val get = kFunction.createLambda<TestObject.() -> Int>()

    val testObject = TestObject()
    assertEquals(100, testObject.get())
    testObject.data = 200
    assertEquals(200, testObject.get())

    val getGetter = kFunction.getter.createLambda<TestObject.() -> Int>()
    assertEquals(200, testObject.getGetter())
  }

  @Test fun testSetter() {
    val kFunction = TestObject::data.setter
    val set = kFunction.createLambda<TestObject.(Int) -> Unit>()

    val testObject = TestObject()
    testObject.set(200)
    assertEquals(200, testObject.data)
  }

  @Test fun testFunction() {
    val kFunction = TestObject::increase
    val up = kFunction.createLambda<TestObject.() -> Unit>()

    val testObject = TestObject()
    testObject.up()
    assertEquals(101, testObject.data)
  }

  @Test fun testConstructor() {
    val kFunction = ::TestObject
    val construct = kFunction.createLambda<() -> TestObject>()

    val testObject = construct()
    assertEquals(100, testObject.data)
  }

  @Test fun testPrivateFieldGetter() {
    val kProperty = TestObject::class.memberProperties
      .find { it.name == "privateData" } as KMutableProperty<*>
    val get = kProperty.createLambda<TestObject.() -> Int>()

    val testObject = TestObject()
    assertEquals(0, testObject.get())
    testObject.increasePrivateData()
    assertEquals(1, testObject.get())
  }

  @Test fun testPrivateFieldSetter() {
    val kProperty = TestObject::class.memberProperties
      .find { it.name == "privateData" } as KMutableProperty<*>
    val set = kProperty.setter.createLambda<TestObject.(Int) -> Unit>()

    val testObject = TestObject()
    testObject.set(100)
    assertEquals(100, testObject.fetchPrivateData())
  }

  @Test fun testPrivateFieldWithoutAccess() {
    val kProperty = TestObject::class.memberProperties
      .find { it.name == "privateData" } as KMutableProperty<*>
    assertThrows<IllegalAccessException> {
      kProperty.createLambda<TestObject.() -> Int>(MethodHandles.publicLookup())
    }
  }
}

class TestObject {
  var data = 100
  private var privateData = 0

  fun increase() {
    data++
  }

  fun fetchPrivateData() = privateData

  fun increasePrivateData() {
    privateData++
  }
}
