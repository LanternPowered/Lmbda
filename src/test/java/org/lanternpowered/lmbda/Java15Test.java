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
