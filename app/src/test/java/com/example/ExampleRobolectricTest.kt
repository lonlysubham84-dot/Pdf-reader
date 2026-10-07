package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ReaderTheme
import com.example.model.StampType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PDF Studio Pro", appName)
    }

    @Test
    fun `verify stamps and reader themes configured`() {
        val stamps = StampType.values()
        assertTrue(stamps.isNotEmpty())
        assertNotNull(StampType.APPROVED)
        assertNotNull(StampType.CONFIDENTIAL)

        val themes = ReaderTheme.values()
        assertEquals(4, themes.size)
    }
}
