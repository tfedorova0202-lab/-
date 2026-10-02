package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun debtPlanAmount_calculationCorrect() {
    val fact = 150000.0
    val penalty = 35000.0
    val insurance = 15000.0
    val plan = maxOf(0.0, fact - penalty - insurance)
    assertEquals(100000.0, plan, 0.001)
  }
}
