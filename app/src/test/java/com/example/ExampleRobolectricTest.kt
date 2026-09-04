package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.JournalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ReflectAI", appName)
  }

  @Test
  fun `create JournalViewModel using AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(JournalViewModel::class.java)
    assertNotNull(viewModel)
    assertNotNull(viewModel.uiState.value)
    assertEquals(com.example.data.model.AiListenerPersona.MAYA, viewModel.uiState.value.selectedPersona)

    viewModel.selectPersona(com.example.data.model.AiListenerPersona.LEO)
    assertEquals(com.example.data.model.AiListenerPersona.LEO, viewModel.uiState.value.selectedPersona)
  }

  @Test
  fun `verify recap stats calculation`() {
    val period = com.example.data.model.RecapPeriod(
      type = com.example.data.model.RecapType.MONTHLY,
      year = 2026,
      month = 9
    )
    val entries = listOf(
      com.example.data.model.JournalInteraction(
        id = "1",
        title = "Reflection 1",
        prompt = "Deep thoughts about work and growth",
        category = "Deep Reflection",
        timestamp = java.util.Calendar.getInstance().apply {
          set(2026, java.util.Calendar.SEPTEMBER, 1, 10, 0)
        }.timeInMillis
      )
    )
    val stats = com.example.data.model.RecapAnalysis.buildInitialStats(period, entries)
    assertEquals(1, stats.totalEntries)
    assertEquals(6, stats.totalWords)
    assertEquals("Deep Reflection", stats.categoryBreakdown.first().category)
  }
}
