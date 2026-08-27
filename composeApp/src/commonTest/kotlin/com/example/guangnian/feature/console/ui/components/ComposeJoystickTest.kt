package com.example.guangnian.feature.console.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeJoystickTest {
    @Test
    fun hapticBandChangesAtConfiguredLevels() {
        val expectedBands = mapOf(
            0 to 0,
            2 to 0,
            3 to 1,
            5 to 1,
            6 to 2,
            8 to 2,
            9 to 3,
            10 to 3
        )

        expectedBands.forEach { (level, expectedBand) ->
            assertEquals(expectedBand, joystickHapticBand(level), "level=$level")
        }
    }
}
