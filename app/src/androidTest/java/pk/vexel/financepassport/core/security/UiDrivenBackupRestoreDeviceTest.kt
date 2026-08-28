package pk.vexel.financepassport.core.security

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
 * The existing UtilityBackupRestoreDeviceTest proves the repository/crypto round trip against
 * fixture entities constructed directly, never through the app's own UI. This test registers a
 * utility bill through the real Add Bill dialog (same path a user drives), backs up the live app
 * database the UI just wrote to, deletes it to simulate loss, restores via the production
 * LiveRestoreService, and confirms the exact UI-entered profile survives intact on disk.
 *
 * Rewritten against the utility-tracker shell — the prior version of this file drove the
 * pre-reset Money screen's Add Account dialog, which no longer exists. The SAF file-picker chrome
 * around "Create Encrypted Backup"/"Restore Encrypted Backup" is not driven here (Compose UI
 * tests cannot reach the separate system picker activity without Espresso-Intents, not a
 * dependency of this project) — app-side encryption/decryption and repository wiring are
 * exercised directly instead, using the same FinanceRepository instance the UI dialog itself
 * calls into.
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
    fun uiEnteredUtilityBillSurvivesBackupDeleteAndRestore() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Bills", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-bill-fab", useUnmergedTree = true).performClick()
        val billName = "UiBackup ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("bill-name", useUnmergedTree = true).performTextInput(billName)
        composeRule.onNodeWithTag("provider", useUnmergedTree = true).performTextInput("UiBackup Power Co")
        composeRule.onNodeWithTag("reference-number", useUnmergedTree = true).performTextInput("UI-REF-${UUID.randomUUID().toString().take(6)}")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()

        val application = composeRule.activity.applicationContext as PassportApplication
        val context = composeRule.activity.applicationContext

        runBlocking {
            // Give the fire-and-forget insert (see UtilityPaymentStatusDeviceTest) a moment to
            // land before backing up.
            var attempts = 0
            while (application.repository.database.utilityBillDao().getAll().none { it.name == billName } && attempts < 25) {
                Thread.sleep(200)
                attempts++
            }
            check(application.repository.database.utilityBillDao().getAll().any { it.name == billName }) {
                "UI-entered utility bill never reached the repository"
            }

            val payload = application.repository.createEncryptedBackup(context, "ui-backup-password".toCharArray())

            DatabaseProvider.close()
            val liveFile = context.getDatabasePath("passport.db")
            liveFile.delete()
            File(liveFile.path + "-wal").delete()
            File(liveFile.path + "-shm").delete()

            LiveRestoreService(context).restore(payload, "ui-backup-password".toCharArray())

            val raw = SQLiteDatabase.openDatabase(liveFile.path, null, SQLiteDatabase.OPEN_READONLY)
            raw.rawQuery("SELECT name, provider FROM utility_bill_profiles WHERE name = ?", arrayOf(billName)).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(billName, cursor.getString(0))
                assertEquals("UiBackup Power Co", cursor.getString(1))
            }
            raw.close()

            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, "passport.db")
                .addMigrations(*DatabaseProvider.ALL_MIGRATIONS).build()
            assertEquals(1, reopened.utilityBillDao().getAll().count { it.name == billName })
            reopened.close()
        }
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
        }
        if (composeRule.onAllNodesWithTag("setup-skip-pin").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("setup-skip-pin").performClick()
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
