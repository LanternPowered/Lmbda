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

import org.junit.jupiter.api.Test;
import org.lanternpowered.lmbda.LambdaFactory;
import org.lanternpowered.lmbda.LambdaType;
import org.lanternpowered.lmbda.MethodHandlesExtensions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LambdaAbstractFunctionGetterTest {

  @Test
  void testGetter() throws Exception {
    final MethodHandles.Lookup lookup =
      MethodHandlesExtensions.privateLookupIn(TestObject.class, MethodHandles.lookup());
    final MethodHandle methodHandle = lookup.findGetter(TestObject.class, "data", int.class);

    final MyFunction getter = LambdaFactory.create(
      new LambdaType<MyFunction>() {}.defineClassesWith(MethodHandles.lookup()), methodHandle);

    final TestObject object = new TestObject();
    assertEquals(100, getter.getValue(object));
    object.setData(10000);
    assertEquals(10000, getter.getValue(object));
  }

  public static class TestObject {

    private int data = 100;

    void setData(final int value) {
      this.data = value;
    }
  }

  abstract static class MyFunction {

    public abstract int getValue(TestObject testObject);
  }
}
