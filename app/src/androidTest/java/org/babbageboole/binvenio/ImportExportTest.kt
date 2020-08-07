package org.babbageboole.binvenio

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@LargeTest
class ImportExportTest {
    @get:Rule
    var intentsTestRule = IntentsTestRule(MainActivity::class.java, false, false)

    private lateinit var db: ResDatabase
    private lateinit var resDao: ResDatabaseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()

        val intent = Intent(context, MainActivity::class.java)
        ResDatabase.setIsTest(true)

        db = ResDatabase.getInstance(context)
        resDao = db.resDatabaseDao
        db.clearAllTables()

        intentsTestRule.launchActivity(intent)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        Timber.i("Tearing down test")
        val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()

        db.clearAllTables()
        ResDatabase.closeInstance()
        context.deleteDatabase(ResDatabase.getName())

        val file = File(context.filesDir, ResDatabase.getSaveName())
        file.delete()
        intentsTestRule.finishActivity()
    }

    private fun stubSaveFileIntent(filename: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/gzip"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        Intents.intending(IntentMatchers.filterEquals(intent)).respondWithFunction {
            val resultData = Intent()
            val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()
            val file = File(context.filesDir, filename)
            resultData.data = Uri.fromFile(file)
            Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        }
    }

    private fun stubLoadFileIntent(filename: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/gzip"
        }
        Intents.intending(IntentMatchers.filterEquals(intent)).respondWithFunction {
            val resultData = Intent()
            val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()
            val file = File(context.filesDir, filename)
            resultData.data = Uri.fromFile(file)
            Timber.i("Responding in stubLoadFileIntent with OK: ${resultData.data}")
            Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        }
    }

    @Test
    fun exportDb() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )

        stubSaveFileIntent(ResDatabase.getSaveName())
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        // The View representing the menu item doesn't have the ID of the item :(
        Espresso.onView(ViewMatchers.withText(R.string.export_item)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("exported"))))
    }

    @Test
    fun importDb() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )

        val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()

        stubSaveFileIntent(ResDatabase.getSaveName())
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        // The View representing the menu item doesn't have the ID of the item :(
        Espresso.onView(ViewMatchers.withText(R.string.export_item)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("exported"))))

        db = ResDatabase.getInstance(context)
        resDao = db.resDatabaseDao
        resDao.deleteRes("containerQR")

        stubLoadFileIntent(ResDatabase.getSaveName())
        Espresso.openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        // The View representing the menu item doesn't have the ID of the item :(
        Espresso.onView(ViewMatchers.withText(R.string.import_item)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString("imported"))))

        db = ResDatabase.getInstance(context)
        resDao = db.resDatabaseDao
        val res = resDao.getRes("containerQR")
        Truth.assertThat(res).isNotNull()
    }

}
