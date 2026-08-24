package pk.vexel.financepassport.core.security

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import android.Manifest
import android.os.Build
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.database.AppDatabase
import pk.vexel.financepassport.core.database.DatabaseProvider

/**
 * Phase 10 backup-equivalence item: the existing BackupRestoreDeviceTest cases prove the
 * repository/crypto round trip against fixture entities constructed directly, never through the
 * app's own UI validation. This test creates a record through the real Add-account dialog (same
 * path a user drives), backs up the live app database the UI just wrote to, deletes it to
 * simulate loss, restores via the production LiveRestoreService, and confirms the exact
 * UI-entered record survives intact on disk. The SAF file-picker chrome around "Create encrypted
 * backup"/"Restore encrypted backup" is not driven here (Compose UI tests cannot reach the
 * separate system picker activity without Espresso-Intents, not a dependency of this project) —
 * app-side encryption/decryption and repository wiring are exercised directly instead, using the
 * same FinanceRepository instance the UI dialog itself calls into.
 */
@RunWith(AndroidJUnit4::class)
class UiDrivenBackupRestoreDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun uiEnteredAccountSurvivesBackupDeleteAndRestore() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()
        val accountName = "UiBackup ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput(accountName)
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("77700")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()

        val application = composeRule.activity.applicationContext as PassportApplication
        val context = composeRule.activity.applicationContext

        runBlocking {
            // Give the fire-and-forget insert (see MoneyCaptureDeviceTest) a moment to land before backing up.
            var attempts = 0
            while (application.repository.database.accountDao().getAll().none { it.name == accountName } && attempts < 25) {
                Thread.sleep(200)
                attempts++
            }
            check(application.repository.database.accountDao().getAll().any { it.name == accountName }) {
                "UI-entered account never reached the repository"
            }

            val payload = application.repository.createEncryptedBackup(context, "ui-backup-password".toCharArray())

            DatabaseProvider.close()
            val liveFile = context.getDatabasePath("passport.db")
            liveFile.delete()
            File(liveFile.path + "-wal").delete()
            File(liveFile.path + "-shm").delete()

            LiveRestoreService(context).restore(payload, "ui-backup-password".toCharArray())

            val raw = SQLiteDatabase.openDatabase(liveFile.path, null, SQLiteDatabase.OPEN_READONLY)
            raw.rawQuery("SELECT name, openingBalanceMinor FROM accounts WHERE name = ?", arrayOf(accountName)).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(accountName, cursor.getString(0))
                assertEquals(7_770_000L, cursor.getLong(1))
            }
            raw.close()

            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, "passport.db")
                .addMigrations(DatabaseProvider.MIGRATION_4_5).build()
            assertEquals(1, reopened.accountDao().getAll().count { it.name == accountName })
            reopened.close()
        }
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
        }
        if (composeRule.onAllNodesWithTag("setup-start-empty").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("setup-start-empty").performClick()
        }
    }

    private fun unlockIfNeeded() {
        dismissOnboardingIfPresent()
        if (composeRule.onAllNodesWithText("Create PIN").fetchSemanticsNodes().isNotEmpty()) {
            val fields = composeRule.onAllNodes(hasSetTextAction())
            fields[0].performTextInput("1234")
            fields[1].performTextInput("1234")
            composeRule.onNodeWithText("Create PIN").performClick()
        } else if (composeRule.onAllNodesWithText("Unlock").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
            composeRule.onNodeWithText("Unlock").performClick()
        }
    }
}
