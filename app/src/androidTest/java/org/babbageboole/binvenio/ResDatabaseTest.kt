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

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.babbageboole.binvenio.database.*
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ResDatabaseTest {
    private lateinit var dao: ResDatabaseDao
    private lateinit var db: ResDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // This will make a fresh database for every test
        db = Room.inMemoryDatabaseBuilder(context, ResDatabase::class.java)
            // Allow main thread queries just for testing
            .allowMainThreadQueries()
            .build()
        dao = db.resDatabaseDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetRes() {
        val res = Res(qr = "12345", name = "bin1", count = 11, isContainer = true, locationQR = "mouse")
        dao.insertRes(res)

        val got = dao.getRes("12345")
        assertThat(got).isNotNull()
        assertThat(got!!).isEqualTo(res)
    }

    @Test
    @Throws(Exception::class)
    fun deleteRes() {
        val res = Res(qr = "12345", name = "bin1", count = 11, isContainer = true, locationQR = "mouse")
        dao.insertRes(res)
        dao.deleteRes("12345")

        val got = dao.getRes("12345")
        assertThat(got).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetContainer() {
        val res = Res(qr = "12345", name = "bin1", isContainer = true)
        dao.insertRes(res)

        val got = dao.getContainer("12345")
        assertThat(got).isNotNull()
        assertThat(got!!).isEqualTo(res)
    }

    @Test
    @Throws(Exception::class)
    fun containerNotFound() {
        val res = Res(qr = "12345", name = "bin1", isContainer = false)
        dao.insertRes(res)

        val got = dao.getContainer("12345")
        assertThat(got).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetNonContainer() {
        val res = Res(qr = "12345", name = "bin1", isContainer = false)
        dao.insertRes(res)

        val got = dao.getNonContainer("12345")
        assertThat(got).isNotNull()
        assertThat(got!!).isEqualTo(res)
    }

    @Test
    @Throws(Exception::class)
    fun nonContainerNotFound() {
        val res = Res(qr = "12345", name = "bin1", isContainer = true)
        dao.insertRes(res)

        val got = dao.getNonContainer("12345")
        assertThat(got).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun cannotInsertRes_nonuniqueQR() {
        val res = Res(qr = "12345", name = "bin1")
        dao.insertRes(res)
        val res2 = Res(qr = "12345", name = "bin2")

        try {
            dao.insertRes(res2)
            fail("Expected second insert of bin to generate exception")
        } catch (ex: Exception) {
        }
    }

    @Test
    @Throws(Exception::class)
    fun insertResIntoRes() {
        val res = Res(qr = "12345", name = "bin1", isContainer = true)
        dao.insertRes(res)

        val shelf = Res(qr = "SHELF#1", name = "shelf #1", isContainer = true)
        dao.insertRes(shelf)

        res.locationQR = shelf.qr
        dao.updateRes(res)

        val resWithContainedRes = dao.getResWithContainedRes(shelf.qr)
        assertThat(resWithContainedRes).isNotNull()
        assertThat(resWithContainedRes!!.res).isEqualTo(shelf)
        assertThat(resWithContainedRes.containedRes).containsExactly(res)
    }

    @Test
    @Throws(Exception::class)
    fun findResByName_one() {
        val res = Res(qr = "00000", name = "diodes")
        dao.insertRes(res)

        val found = dao.findResByPartialName("diode")
        assertThat(found).containsExactly(res)
    }

    @Test
    @Throws(Exception::class)
    fun findBinnableByName_two() {
        val res = Res(qr = "00000", name = "diodes")
        dao.insertRes(res)
        val res2 = Res(qr = "00001", name = "1N4004 diode")
        dao.insertRes(res2)

        val found = dao.findResByPartialName("diode")
        assertThat(found).containsExactly(res, res2)
    }

    @Test
    @Throws(Exception::class)
    fun findBinnableByName_notFound() {
        val res = Res(qr = "00000", name = "diodes")
        dao.insertRes(res)

        val found = dao.findResByPartialName("resistor")
        assertThat(found).isEmpty()
    }

    @Test
    @Throws(Exception::class)
    fun getContents() {
        val res = Res(qr = "12345", name = "bin_Z", isContainer = true, locationQR = "SHELF#1")
        dao.insertRes(res)
        val res2 = Res(qr = "123452", name = "bin_A", isContainer = true, locationQR = "SHELF#1")
        dao.insertRes(res2)
        val shelf = Res(qr = "SHELF#1", name = "shelf #1", isContainer = true)
        dao.insertRes(shelf)

        val found = dao.getContents("SHELF#1")
        assertThat(found).containsExactly(res, res2)
    }

    @Test
    @Throws(Exception::class)
    fun getResWithContainer() {
        val res = Res(qr = "12345", name = "hairpin", isContainer = false, count = 371, locationQR = "BIN#1")
        dao.insertRes(res)
        val shelf = Res(qr = "BIN#1", name = "bin of hairpins", isContainer = true)
        dao.insertRes(shelf)

        val found = dao.getResWithContainer("12345")
        assertThat(found).isNotNull()
        assertThat(found!!.qr).isEqualTo(res.qr)
        assertThat(found.name).isEqualTo(res.name)
        assertThat(found.count).isEqualTo(res.count)
        assertThat(found.isContainer).isEqualTo(res.isContainer)
        assertThat(found.containerName).isEqualTo(shelf.name)
    }

    @Test
    @Throws(Exception::class)
    fun getResWithContainer_noContainer() {
        val res = Res(qr = "12345", name = "hairpin", isContainer = false, count = 371)
        dao.insertRes(res)

        val found = dao.getResWithContainer("12345")
        assertThat(found).isNotNull()
        assertThat(found!!.qr).isEqualTo(res.qr)
        assertThat(found.name).isEqualTo(res.name)
        assertThat(found.count).isEqualTo(res.count)
        assertThat(found.isContainer).isEqualTo(res.isContainer)
        assertThat(found.containerName).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun findResWithContainer() {
        val res = Res(qr = "12345", name = "hairpin", count = 371, locationQR = "BIN#1")
        dao.insertRes(res)
        val res2 = Res(qr = "123456", name = "pincushion", count = 101, locationQR = "BIN#1")
        dao.insertRes(res2)
        val res3 = Res(qr = "1234567", name = "pinbin", isContainer = true)
        dao.insertRes(res3)
        val shelf = Res(qr = "BIN#1", name = "bin of sewing supplies", isContainer = true)
        dao.insertRes(shelf)

        val found = dao.findResWithContainerResByPartialName("pin")
        assertThat(found).hasSize(3)
        val expectedRes = ResWithContainingRes(qr = res.qr, name = res.name, count = res.count, containerName = shelf.name)
        val expectedRes2 = ResWithContainingRes(qr = res2.qr, name = res2.name, count = res2.count, containerName = shelf.name)
        val expectedRes3 = ResWithContainingRes(qr = res3.qr, name = res3.name, isContainer = true)
        assertThat(found).containsExactly(expectedRes, expectedRes2, expectedRes3)
    }

    @Test
    @Throws(Exception::class)
    fun sqlInjectionThwarted() {
        val res = Res(qr = "12345", name = "bin_Z", isContainer = true)
        dao.insertRes(res)
        val inj = dao.getRes("12345; DROP TABLE res")
        assertThat(inj).isNull()

        var found = dao.getRes("12345")
        assertThat(found).isNotNull()

        found = dao.getRes("12345; DROP TABLE res")
        assertThat(found).isNull()

        found = dao.getRes("12345")
        assertThat(found).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun findWithEscapes() {
        val res = Res(qr = "12345", name = "bin_Z 10% resistors", isContainer = true)
        dao.insertRes(res)
        val res2 = Res(qr = "123456", name = "10 ohm resistors", isContainer = true)
        dao.insertRes(res2)
        val res3 = Res(qr = "1234563", name = """\ohm resistors""", isContainer = true)
        dao.insertRes(res3)

        var found = dao.findResByPartialName("10%")
        assertThat(found).hasSize(2)

        found = dao.findResByPartialName("""10\%""")
        assertThat(found).hasSize(1)

        found = dao.findResByPartialName("""\\ohm""")
        assertThat(found).hasSize(1)
    }
}