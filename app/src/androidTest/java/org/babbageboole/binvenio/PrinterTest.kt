package org.babbageboole.binvenio

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.util.Printer
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
import org.babbageboole.binvenio.sticker.Printing
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
class PrinterTest {
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

    @Test
    fun findPrinter() {
        val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()
        val p = Printing(context)
        p.searchForPrinter()
        p.print("49a12ca7-1f0c-41cf-a87f-32d9dab496ec", "Book: Babbage and Lovelace (Padua)")
    }
}
