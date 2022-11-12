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

object TestSingleton {
  var data = 100

  fun increase() {
    data++
  }

  @Suppress("RedundantSuspendModifier")
  suspend fun increaseSuspend() {
    increase()
  }
}
