// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.babbageboole.binvenio

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.IOException
import com.google.common.truth.Truth.assertThat
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.ui.bin_scanned.ContentsAdapter
import org.hamcrest.Matchers.*
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainTest {
//    @get:Rule
//    var activityRule = ActivityTestRule(MainActivity::class.java)

    // Apparently IntentsTestRule subclasses ActivityTestRule, so you don't have to
    // declare that.
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
        val context = ApplicationProvider.getApplicationContext<BinvenioApplication>()

        db.clearAllTables()
        ResDatabase.closeInstance()
        context.deleteDatabase(ResDatabase.getName())

        intentsTestRule.finishActivity()
    }

    // Stub an intent to return the given qr string.
    private fun stubIntents(qr: String) {
        intending(hasAction("com.google.zxing.client.android.SCAN")).respondWithFunction {
            val resultData = Intent()
            resultData.putExtra(com.google.zxing.client.android.Intents.Scan.RESULT, qr)
            resultData.putExtra(com.google.zxing.client.android.Intents.Scan.RESULT_FORMAT, "QR_CODE")
            Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        }
    }

    private fun checkTitle(resourceId: Int) {
        onView(withId(R.id.binvenio_toolbar)).check(matches(hasDescendant(withText(resourceId))))
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertThat(appContext.packageName).isEqualTo("org.babbageboole.binvenio")
        onView(withId(R.id.binvenio_toolbar)).check(matches(isDisplayed()))
        checkTitle(R.string.app_name)
    }

    @Test
    fun addItem() {
        stubIntents("12345AB")

        onView(withId(R.id.add_item_button)).perform(click())

        intended(hasAction("com.google.zxing.client.android.SCAN"))

        checkTitle(R.string.add_item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("12345AB")))
        onView(withId(R.id.name_edit)).perform(typeText("Diode")).perform(closeSoftKeyboard())
        onView(withId(R.id.count_edit)).perform(typeText("102")).perform(closeSoftKeyboard())
        onView(withId(R.id.add_button)).perform(click())

        val item = resDao.getNonContainer("12345AB")
        assertThat(item).isNotNull()
        assertThat(item!!.name).isEqualTo("Diode")
        assertThat(item.count).isEqualTo(102)
    }

    @Test
    fun addItem_cancel() {
        stubIntents("12345AB")

        onView(withId(R.id.add_item_button)).perform(click())

        checkTitle(R.string.add_item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("12345AB")))
        onView(withId(R.id.name_edit)).perform(typeText("Diode")).perform(closeSoftKeyboard())
        onView(withId(R.id.count_edit)).perform(typeText("102")).perform(closeSoftKeyboard())
        onView(withId(R.id.cancel_button)).perform(click())
        onView(withId(R.id.binvenio_title)).check(matches(isDisplayed()))
        Espresso.pressBackUnconditionally()
        assertThat(intentsTestRule.activity.isDestroyed).isTrue()

        val item = resDao.getNonContainer("12345AB")
        assertThat(item).isNull()
    }

    @Test
    fun addItem_back() {
        stubIntents("12345AB")

        onView(withId(R.id.add_item_button)).perform(click())

        checkTitle(R.string.add_item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("12345AB")))
        onView(withId(R.id.name_edit)).perform(typeText("Diode")).perform(closeSoftKeyboard())
        onView(withId(R.id.count_edit)).perform(typeText("102")).perform(closeSoftKeyboard())
        pressBack()
        onView(withId(R.id.binvenio_title)).check(matches(isDisplayed()))

        val item = resDao.getNonContainer("12345AB")
        assertThat(item).isNull()
    }

    @Test
    fun addItem_duplicate() {
        resDao.insertRes(
            Res(
                qr = "12345AB",
                name = "preexisting item",
                count = 1
            )
        )
        stubIntents("12345AB")

        onView(withId(R.id.add_item_button)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("already registered"))))
    }

    @Test
    fun addItem_negativeCount() {
        stubIntents("12345AB")

        onView(withId(R.id.add_item_button)).perform(click())

        checkTitle(R.string.add_item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("12345AB")))
        onView(withId(R.id.name_edit)).perform(typeText("Diode")).perform(closeSoftKeyboard())
        onView(withId(R.id.count_edit)).perform(clearText()).perform(typeText("-1"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.add_button)).perform(click())

        val item = resDao.getNonContainer("12345AB")
        assertThat(item).isNotNull()
        assertThat(item!!.name).isEqualTo("Diode")
        // Since we can't hit the minus sign in the keyboard, this just was 1.
        assertThat(item.count).isEqualTo(1)
    }

    @Test
    fun addContainer() {
        stubIntents("12345AC")

        onView(withId(R.id.add_container_button)).perform(click())

        intended(hasAction("com.google.zxing.client.android.SCAN"))

        checkTitle(R.string.add_container_title)

        onView(withId(R.id.qr_text)).check(matches(withText("12345AC")))
        onView(withId(R.id.name_edit)).perform(typeText("BIN #12345AC"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.add_button)).perform(click())

        val bin = resDao.getContainer("12345AC")
        assertThat(bin).isNotNull()
        assertThat(bin!!.name).isEqualTo("BIN #12345AC")
    }

    @Test
    fun addContainer_cancel() {
        stubIntents("12345AC")

        onView(withId(R.id.add_container_button)).perform(click())

        checkTitle(R.string.add_container_title)

        onView(withId(R.id.qr_text)).check(matches(withText("12345AC")))
        onView(withId(R.id.name_edit)).perform(typeText("BIN #12345AC"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.cancel_button)).perform(click())
        onView(withId(R.id.binvenio_title)).check(matches(isDisplayed()))
        Espresso.pressBackUnconditionally()
        assertThat(intentsTestRule.activity.isDestroyed).isTrue()

        val bin = resDao.getContainer("12345AC")
        assertThat(bin).isNull()
    }

    @Test
    fun addContainer_duplicate() {
        resDao.insertRes(Res(qr = "12345AC", name = "preexisting container", isContainer = true))
        stubIntents("12345AC")

        onView(withId(R.id.add_container_button)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("already registered"))))
    }

    @Test
    fun deleteItem() {
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                count = 1
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.item_title)

        onView(withId(R.id.delete_item_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("deleted"))))

        val item = resDao.getNonContainer("itemQR")
        assertThat(item).isNull()
    }

    @Test
    fun deleteContainer() {
        resDao.insertRes(
            Res(
                qr = "binQR",
                name = "bin",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(qr = "res1QR", name = "thing1", locationQR = "binQR")
        )
        resDao.insertRes(
            Res(qr = "res2QR", name = "thing2", locationQR = "binQR")
        )
        stubIntents("binQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.container_title)

        onView(withId(R.id.delete_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("deleted"))))

        val item = resDao.getContainer("binQR")
        assertThat(item).isNull()
        var content = resDao.getNonContainer("res1QR")!!
        assertThat(content.locationQR).isNull()
        content = resDao.getNonContainer("res2QR")!!
        assertThat(content.locationQR).isNull()
    }

    @Test
    fun addItemToContainer() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                count = 1,
                isContainer = false
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.item_title)

        stubIntents("containerQR")
        onView(withId(R.id.add_remove_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("put it in container"))))

        val item = resDao.getNonContainer("itemQR")
        assertThat(item!!.locationQR).isEqualTo("containerQR")
    }

    @Test
    fun addItemToItem() {
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                count = 1,
                isContainer = false
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.item_title)

        stubIntents("itemQR")
        onView(withId(R.id.add_remove_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("You didn't scan a"))))
        checkTitle(R.string.item_title)

        val item = resDao.getNonContainer("itemQR")
        assertThat(item!!.locationQR).isNull()
    }

    @Test
    fun scanItem_inContainer() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                isContainer = false,
                locationQR = "containerQR",
                count = 1
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())

        intended(hasAction("com.google.zxing.client.android.SCAN"))

        checkTitle(R.string.item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("itemQR")))
        onView(withId(R.id.name_edit)).check(matches(withText("preexisting item")))
        onView(withId(R.id.count_text)).check(matches(withText("1")))
        onView(withId(R.id.location_text)).check(matches(withText("preexisting container")))

        onView(withId(R.id.plus_1_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("2")))

        onView(withId(R.id.minus_1_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("1")))

        onView(withId(R.id.plus_10_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("11")))

        onView(withId(R.id.minus_10_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("1")))

        onView(withId(R.id.plus_100_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("101")))

        onView(withId(R.id.minus_100_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("1")))

        onView(withId(R.id.minus_100_button)).perform(click())
        onView(withId(R.id.count_text)).check(matches(withText("0")))

        onView(withId(R.id.name_edit))
            .perform(clearText())
            .perform(typeText("something"))
            .perform(closeSoftKeyboard())

        pressBack()
        onView(withId(R.id.binvenio_title)).check(matches(isDisplayed()))

        val item = resDao.getNonContainer("itemQR")
        assertThat(item!!.count).isEqualTo(1)
        assertThat(item.name).isEqualTo("preexisting item")
    }

    @Test
    fun scanItem_unlocated() {
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                isContainer = false,
                count = 1
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())

        checkTitle(R.string.item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("itemQR")))
        onView(withId(R.id.name_edit)).check(matches(withText("preexisting item")))
        onView(withId(R.id.count_text)).check(matches(withText("1")))
        onView(withId(R.id.location_text)).check(matches(withText("None")))
    }

    @Test
    fun editItem() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                isContainer = false,
                locationQR = "containerQR",
                count = 1
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())

        checkTitle(R.string.item_title)

        onView(withId(R.id.qr_text)).check(matches(withText("itemQR")))
        onView(withId(R.id.name_edit)).check(matches(withText("preexisting item")))
        onView(withId(R.id.count_text)).check(matches(withText("1")))
        onView(withId(R.id.location_text)).check(matches(withText("preexisting container")))

        onView(withId(R.id.plus_100_button)).perform(click())
        onView(withId(R.id.plus_10_button)).perform(click())

        onView(withId(R.id.name_edit))
            .perform(clearText())
            .perform(typeText("something"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.save_changes_button)).perform(click())

        val item = resDao.getNonContainer("itemQR")
        assertThat(item!!.count).isEqualTo(111)
        assertThat(item.name).isEqualTo("something")
    }

    @Test
    fun removeItemFromContainer() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "preexisting item",
                isContainer = false,
                locationQR = "containerQR",
                count = 1
            )
        )
        stubIntents("itemQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.item_title)

        onView(withId(R.id.add_remove_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("take it out of container"))))

        val item = resDao.getNonContainer("itemQR")
        assertThat(item!!.locationQR).isNull()
    }

    @Test
    fun showContents_noLocation() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = "Funky fresh item that is extremely long and is broken into several lines",
                locationQR = "containerQR",
                count = 13
            )
        )
        resDao.insertRes(
            Res(
                qr = "binQR",
                name = "a bin",
                isContainer = true,
                locationQR = "containerQR"
            )
        )
        stubIntents("containerQR")

        val found = resDao.getContents("containerQR")
        assertThat(found).isNotEmpty()

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.container_title)

        onView(withId(R.id.qr_text)).check(matches(withText("containerQR")))
        onView(withId(R.id.name_edit)).check(matches(withText("preexisting container")))
        onView(withId(R.id.location_text)).check(matches(withText("None")))

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        onView(withId(R.id.add_remove_button)).check(
            matches(
                withText(
                    appContext.resources.getString(
                        R.string.add_to_loc_button_text
                    )
                )
            )
        )

        onView(withId(R.id.content_list)).perform(
            RecyclerViewActions.scrollToPosition<ContentsAdapter.ViewHolder>(0)
        )
        onView(withText("13x")).check(matches(isDisplayed()))
        onView(withText("Funky fresh item that is extremely long and is broken into several lines")).check(
            matches(isDisplayed())
        )
        onView(withText("a bin")).check(matches(isDisplayed()))
    }

    @Test
    fun editContainer() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        stubIntents("containerQR")

        onView(withId(R.id.scan_button)).perform(click())

        checkTitle(R.string.container_title)

        onView(withId(R.id.qr_text)).check(matches(withText("containerQR")))
        onView(withId(R.id.name_edit)).check(matches(withText("preexisting container")))
        onView(withId(R.id.location_text)).check(matches(withText("None")))

        onView(withId(R.id.name_edit))
            .perform(clearText())
            .perform(typeText("something"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.save_changes_button)).perform(click())

        val item = resDao.getContainer("containerQR")!!
        assertThat(item.name).isEqualTo("something")
    }

    @Test
    fun putContainerOnShelf() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "shelfQR",
                name = "shelf",
                isContainer = true
            )
        )
        stubIntents("containerQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.container_title)

        stubIntents("shelfQR")
        onView(withId(R.id.add_remove_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("put it in location"))))

        val item = resDao.getContainer("containerQR")
        assertThat(item!!.locationQR).isEqualTo("shelfQR")
    }

    @Test
    fun takeContainerOffShelf() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true,
                locationQR = "shelfQR"
            )
        )
        resDao.insertRes(
            Res(
                qr = "shelfQR",
                name = "Shelf",
                isContainer = true
            )
        )
        stubIntents("containerQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.container_title)

        onView(withId(R.id.qr_text)).check(matches(withText("containerQR")))
        onView(withId(R.id.name_edit)).check(matches(withText("preexisting container")))
        onView(withId(R.id.location_text)).check(matches(withText("Shelf")))

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        onView(withId(R.id.add_remove_button)).check(
            matches(
                withText(
                    appContext.resources.getString(
                        R.string.remove_from_loc_button_text
                    )
                )
            )
        )

        onView(withId(R.id.add_remove_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("remove it"))))

        val item = resDao.getContainer("containerQR")
        assertThat(item!!.locationQR).isNull()
    }

    @Test
    fun putShelfIntoContainer_fails() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "preexisting container",
                isContainer = true,
                locationQR = "shelfQR"
            )
        )
        resDao.insertRes(
            Res(
                qr = "shelfQR",
                name = "shelf",
                isContainer = true
            )
        )
        stubIntents("shelfQR")

        onView(withId(R.id.scan_button)).perform(click())
        checkTitle(R.string.container_title)

        stubIntents("containerQR")
        onView(withId(R.id.add_remove_button)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
            .check(matches(withText(containsString("already inside"))))

        val item = resDao.getContainer("shelfQR")
        assertThat(item!!.locationQR).isNull()
    }

    @Test
    fun findItem() {
        val longText = "Funky fresh item that is extremely long and is broken into several lines"

        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container2QR",
                name = "sack",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container3QR",
                name = "plonkerbox",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = longText,
                locationQR = "containerQR",
                count = 13
            )
        )
        resDao.insertRes(
            Res(
                qr = "thingQR",
                name = "long things",
                locationQR = "container2QR",
                count = 1
            )
        )

        onView(withId(R.id.find_button)).perform(click())
        checkTitle(R.string.find_title)

        onView(withId(R.id.name_edit)).perform(typeText("lon"))
            .perform(closeSoftKeyboard())

        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.scrollToPosition<ContentsAdapter.ViewHolder>(
                0
            )
        )
            .check(matches(atPosition(0, hasDescendant(withText("13x")))))
            .check(matches(atPosition(0, hasDescendant(withText(longText)))))
            .check(matches(atPosition(0, hasDescendant(withText("bin")))))
            .check(matches(atPosition(1, hasDescendant(withText("1x")))))
            .check(matches(atPosition(1, hasDescendant(withText("long things")))))
            .check(matches(atPosition(1, hasDescendant(withText("sack")))))
            .check(matches(atPosition(2, hasDescendant(withText("plonkerbox")))))

        onView(withId(R.id.name_edit))
            .perform(clearText())
            .perform(typeText("long t"))
            .perform(closeSoftKeyboard())

        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.scrollToPosition<ContentsAdapter.ViewHolder>(
                0
            )
        )
            .check(matches(atPosition(0, hasDescendant(withText("1x")))))
            .check(matches(atPosition(0, hasDescendant(withText("long things")))))
            .check(matches(atPosition(0, hasDescendant(withText("sack")))))
        onView(withText("13x")).check(doesNotExist())

        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<ContentsAdapter.ViewHolder>(0, click())
        )
        checkTitle(R.string.item_title)
        onView(withId(R.id.qr_text)).check(matches(withText("thingQR")))
    }

    @Test
    fun findContainer() {
        val longText = "Funky fresh item that is extremely long and is broken into several lines"

        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container2QR",
                name = "sack",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container3QR",
                name = "plonkerbox",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = longText,
                locationQR = "containerQR",
                count = 13
            )
        )
        resDao.insertRes(
            Res(
                qr = "thingQR",
                name = "long things",
                locationQR = "container2QR",
                count = 1
            )
        )

        onView(withId(R.id.find_button)).perform(click())
        checkTitle(R.string.find_title)

        onView(withId(R.id.name_edit)).perform(typeText("lon"))
            .perform(closeSoftKeyboard())

        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.scrollToPosition<ContentsAdapter.ViewHolder>(
                0
            )
        )
            .check(matches(atPosition(0, hasDescendant(withText("13x")))))
            .check(matches(atPosition(0, hasDescendant(withText(longText)))))
            .check(matches(atPosition(0, hasDescendant(withText("bin")))))
            .check(matches(atPosition(1, hasDescendant(withText("1x")))))
            .check(matches(atPosition(1, hasDescendant(withText("long things")))))
            .check(matches(atPosition(1, hasDescendant(withText("sack")))))
            .check(matches(atPosition(2, hasDescendant(withText("plonkerbox")))))

        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<ContentsAdapter.ViewHolder>(2, click())
        )
        checkTitle(R.string.container_title)
        onView(withId(R.id.qr_text)).check(matches(withText("container3QR")))
    }

    @Test
    fun goBackFromFindItem() {
        onView(withId(R.id.find_button)).perform(click())
        checkTitle(R.string.find_title)

        pressBack()
        checkTitle(R.string.app_name)
    }

    @Test
    fun goBackFromSelectedFindItem() {
        val longText = "Funky fresh item that is extremely long and is broken into several lines"

        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container2QR",
                name = "sack",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container3QR",
                name = "plonkerbox",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = longText,
                locationQR = "containerQR",
                count = 13
            )
        )
        resDao.insertRes(
            Res(
                qr = "thingQR",
                name = "long things",
                locationQR = "container2QR",
                count = 1
            )
        )

        onView(withId(R.id.find_button)).perform(click())
        checkTitle(R.string.find_title)
        onView(withId(R.id.name_edit)).perform(typeText("long things"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<ContentsAdapter.ViewHolder>(0, click())
        )
        checkTitle(R.string.item_title)

        Espresso.pressBack()
        checkTitle(R.string.find_title)
    }

    @Test
    fun goBackFromSelectedFindContainer() {
        val longText = "Funky fresh item that is extremely long and is broken into several lines"

        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container2QR",
                name = "sack",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "container3QR",
                name = "plonkerbox",
                isContainer = true
            )
        )
        resDao.insertRes(
            Res(
                qr = "itemQR",
                name = longText,
                locationQR = "containerQR",
                count = 13
            )
        )
        resDao.insertRes(
            Res(
                qr = "thingQR",
                name = "long things",
                locationQR = "container2QR",
                count = 1
            )
        )

        onView(withId(R.id.find_button)).perform(click())
        checkTitle(R.string.find_title)
        onView(withId(R.id.name_edit)).perform(typeText("plonker"))
            .perform(closeSoftKeyboard())
        onView(withId(R.id.found_res_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<ContentsAdapter.ViewHolder>(0, click())
        )
        checkTitle(R.string.container_title)

        pressBack()
        checkTitle(R.string.find_title)
    }

    @Test
    fun nuke_cancel() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        // The View representing the menu item doesn't have the ID of the item :(
        onView(withText(R.string.nuke_item)).perform(click())
        onView(withText(R.string.nuke_database_message)).check(matches(isDisplayed()))
        onView(withText(R.string.cancel)).perform(click())

        assertThat(resDao.getRes("containerQR")).isNotNull()
    }

    @Test
    fun nuke_confirm() {
        resDao.insertRes(
            Res(
                qr = "containerQR",
                name = "bin",
                isContainer = true
            )
        )

        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        // The View representing the menu item doesn't have the ID of the item :(
        onView(withText(R.string.nuke_item)).perform(click())
        onView(withText(R.string.nuke_database_message)).check(matches(isDisplayed()))
        onView(withText(R.string.nuke)).perform(click())

        assertThat(resDao.getRes("containerQR")).isNull()
    }

}

