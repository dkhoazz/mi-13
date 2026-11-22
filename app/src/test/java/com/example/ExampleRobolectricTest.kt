package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppLanguage
import com.example.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `bilingual app name resolves for english and vietnamese`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val enContext = LocaleHelper.localizedContext(base, AppLanguage.EN)
        val viContext = LocaleHelper.localizedContext(base, AppLanguage.VI)

        assertEquals("HyperOS Battery", enContext.getString(R.string.app_name))
        assertEquals("HyperOS Pin & Tối Ưu", viContext.getString(R.string.app_name))
    }
}
