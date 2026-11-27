package com.example

import com.example.ui.dialogs.allowsLegacySimulatedPhotoScan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QaPhotoAvailabilityTest {
    @Test
    fun flutterQaNeverUsesLegacySimulatedPhotoScan() {
        assertFalse(allowsLegacySimulatedPhotoScan("com.example.waleadsaver.flutterqa"))
        assertTrue(allowsLegacySimulatedPhotoScan("com.example.waleadsaver"))
    }
}
