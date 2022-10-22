/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package org.lanternpowered.lmbda.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.lanternpowered.lmbda.MethodHandlesExtensions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.ToIntFunction;

@SuppressWarnings("UnstableApiUsage")
class LambdaGetterTest {

  @Test
  void test() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findGetter(TestObject.class, "data", int.class);

    final ToIntFunction<TestObject> getter = LambdaFactory.create(
      new LambdaType<ToIntFunction<TestObject>>() {}, methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, getter.applyAsInt(object));
    object.setData(10000);
    assertEquals(10000, getter.applyAsInt(object));
  }

  @Test
  void testGenericSignature() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findGetter(TestObject.class, "data", int.class);

    final ToIntFunction<TestObject> getter = LambdaFactory.create(
      new LambdaType<ToIntFunction<TestObject>>() {}, methodHandle);

    final TypeToken<?> paramType = TypeToken.of(getter.getClass())
      .resolveType(ToIntFunction.class.getTypeParameters()[0]);
    assertEquals(paramType.getRawType(), TestObject.class);
  }

  public static class TestObject {

    private int data = 100;

    void setData(int value) {
      this.data = value;
    }
  }
}
