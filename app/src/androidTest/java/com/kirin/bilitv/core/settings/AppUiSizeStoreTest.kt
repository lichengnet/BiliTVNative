package com.kirin.bilitv.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUiSizeStoreTest {
  @Test fun savingAndReopeningRetainsSizeAndUnrelatedSettings() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = context.filesDir.resolve("ui-size-test-").resolve(UUID.randomUUID().toString()+".preferences_pb")
    val job = SupervisorJob()
    val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file })
    val store = AppSettingsStore(context, data)
    assertEquals(AppUiSize.Medium, store.settings.first().uiSize)
    store.setUiSize(AppUiSize.Large)
    store.setShowClock(false)
    assertEquals(AppUiSize.Large, store.settings.first().uiSize)
    job.cancelAndJoin()
    val newJob = SupervisorJob()
    val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + newJob), produceFile = { file })
    val restored = AppSettingsStore(context, reopened).settings.first()
    assertEquals(AppUiSize.Large, restored.uiSize)
    assertEquals(false, restored.showClock)
    newJob.cancelAndJoin()
  }

  @Test fun invalidSavedValueFallsBackToMedium() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val job = SupervisorJob()
    val data = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job),
      produceFile = { context.filesDir.resolve("ui-size-invalid-"+UUID.randomUUID()+".preferences_pb") })
    data.edit { it[stringPreferencesKey("ui_size")] = "invalid" }
    assertEquals(AppUiSize.Medium, AppSettingsStore(context, data).settings.first().uiSize)
    job.cancelAndJoin()
  }
}
