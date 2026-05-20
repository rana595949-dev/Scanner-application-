package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.ScannerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    assertEquals("OmniScanner", appName)
  }

  @Test
  fun testViewModelPresetsInitializationAndCustomPresetSave() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ScannerViewModel(app)
    
    // Check default presets size is initially 4
    val initialPresets = viewModel.scanPresets.value
    assertTrue(initialPresets.size >= 4)
    assertEquals("Standard A4", initialPresets[0].name)
    
    // Save a custom preset
    viewModel.saveCustomPreset("My Office Invoice", 200, "Letter", "Magic Color")
    
    // Check that it was appended
    val updatedPresets = viewModel.scanPresets.value
    assertEquals(initialPresets.size + 1, updatedPresets.size)
    val lastPreset = updatedPresets.last()
    assertEquals("My Office Invoice", lastPreset.name)
    assertEquals(200, lastPreset.dpi)
    assertEquals("Letter", lastPreset.pageSize)
    assertEquals("Magic Color", lastPreset.filterType)
  }

  @Test
  fun testCloudConnectionStatusAndToggles() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ScannerViewModel(app)
    
    // Test GDrive connection and disconnection
    assertNull(viewModel.cloudDriveAccount.value)
    viewModel.connectCloudDrive("test-user@gmail.com")
    assertEquals("test-user@gmail.com", viewModel.cloudDriveAccount.value)
    
    viewModel.disconnectCloudDrive()
    assertNull(viewModel.cloudDriveAccount.value)
    
    // Test Dropbox link state
    assertNull(viewModel.cloudDropboxAccount.value)
    viewModel.connectCloudDropbox("dropboxSyncUser")
    assertEquals("dropboxSyncUser", viewModel.cloudDropboxAccount.value)
    
    viewModel.disconnectCloudDropbox()
    assertNull(viewModel.cloudDropboxAccount.value)
    
    // Test automatic backup toggle state persistence
    assertFalse(viewModel.cloudBackupEnabled.value)
    viewModel.toggleCloudBackup(true)
    assertTrue(viewModel.cloudBackupEnabled.value)
  }
}

