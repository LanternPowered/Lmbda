/*
 * Lmbda
 *
 * Copyright (c) LanternPowered <https://www.lanternpowered.org>
 * Copyright (c) contributors
 *
 * This work is licensed under the terms of the MIT License (MIT). For
 * a copy, see 'LICENSE.txt' or <https://opensource.org/licenses/MIT>.
 */
package org.lanternpowered.lmbda;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Java15Test {

  @Test
  @EnabledForJreRange(min = JRE.JAVA_15)
  void testDefineHiddenClassExists() {
    assertNotNull(InternalMethodHandles.findDefineHiddenClassMethodHandle());
  }
}
