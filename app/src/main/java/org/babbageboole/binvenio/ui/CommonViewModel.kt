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

package org.babbageboole.binvenio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.babbageboole.binvenio.BinvenioApplication
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.zebra.Printer
import timber.log.Timber
import java.net.InetSocketAddress

abstract class CommonViewModel(
    application: Application
) : AndroidViewModel(application) {
    protected var database = ResDatabase.getInstance(application).resDatabaseDao

    protected var _bringUpScanner = MutableLiveData<Boolean>()
    val bringUpScanner: LiveData<Boolean>
        get() = _bringUpScanner
    protected var _why = MutableLiveData<String?>()

    protected var _showPrintSearch = MutableLiveData<Boolean>()
    val showPrintSearch: LiveData<Boolean>
        get() = _showPrintSearch

    protected var _printComplete = MutableLiveData<Boolean>()
    val printComplete: LiveData<Boolean>
        get() = _printComplete

    protected var _goBackToMain = MutableLiveData<Boolean>()
    val goBackToMain: LiveData<Boolean>
        get() = _goBackToMain

    protected var _showError = MutableLiveData<String?>()
    val showError: LiveData<String?>
        get() = _showError

    protected var _showMsg = MutableLiveData<String?>()
    val showMsg: LiveData<String?>
        get() = _showMsg

    /**
     * viewModelJob allows us to cancel all coroutines started by this ViewModel.
     */
    private var viewModelJob = Job()

    /**
     * A [CoroutineScope] keeps track of all coroutines started by this ViewModel.
     *
     * Because we pass it [viewModelJob], any coroutine started in this uiScope can be cancelled
     * by calling `viewModelJob.cancel()`
     *
     * By default, all coroutines started in uiScope will launch in [Dispatchers.Main] which is
     * the main thread on Android. This is a sensible default because most coroutines started by
     * a [ViewModel] update the UI after performing some processing.
     */
    protected val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    /**
     * Called when the ViewModel is dismantled.
     * At this point, we want to cancel all coroutines;
     * otherwise we end up with processes that have nowhere to return to
     * using memory and resources.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    open fun onNavigationComplete() {
        Timber.i("Navigation complete")
        _showMsg.value = null
        _showError.value = null
        _goBackToMain.value = false
    }

    fun onErrorShown() {
        _showError.value = null
    }

    fun onMsgShown() {
        _showMsg.value = null
    }

    fun onScan() {
        _why.value = null
        _bringUpScanner.value = true
    }

    fun scannerBroughtUp() {
        _bringUpScanner.value = false
    }

    open fun onScanned(format: String, content: String) {
    }

    fun showPrintSearchShown() {
        _showPrintSearch.value = false
    }

    // The x coordinate depends on the paper width.
    // This is good only for a 200dpi printer,
    // 2.25" wide (i.e. 450 dots across),
    // on 1.5" x 0.5" paper (300 x 100 centered on x=225,
    // so x=75 to x=375).
    // With an 10 dot wide font and 2 dot gap, only 16 characters
    // work starting from x=165 (192 dots, to x=357)
    fun printSticker(qr: String, name: String): Boolean {
        val app = getApplication<BinvenioApplication>()

        val network = app.getNetwork()
        if (network == null) {
            _showError.value = "No wireless connectivity"
            return false
        }

        val addr = app.printerAddr
        if (addr == null) {
            Timber.i("Need to find printer")
            _showPrintSearch.value = true
            return false
        }

        _showMsg.value = "Printing sticker..."

        val lines = mutableListOf("", "", "", "")
        val chunks = name.chunked(16)
        for ((index, chunk) in chunks.withIndex()) {
            if (index < lines.size) lines[index] = chunk
        }
        val str = """
            ^XA
            ^FO90,20
            ^BQN,2,2
            ^FDQA,$qr^FS
            ^FO165,25
            ^ADN,18,10
            ^FD${lines[0]}^FS
            ^FO165,45
            ^ADN,18,10
            ^FD${lines[1]}^FS
            ^FO165,65
            ^ADN,18,10
            ^FD${lines[2]}^FS
            ^FO165,85
            ^ADN,18,10
            ^FD${lines[3]}^FS
            ^XZ
        """.trimIndent()

        uiScope.launch {
            if (print(addr, str)) {
                _printComplete.value = true
            }
        }
        return true
    }

    open fun onPrinterFound() {
    }

    open fun onCancelSearchPrinter() {
    }

    open fun onPrintComplete() {
        _printComplete.value = false
        _showMsg.value = "Sticker printed!"
    }

    protected suspend fun getRes(qr: String): Res? {
        return withContext(Dispatchers.IO) {
            database.getRes(qr)
        }
    }

    protected suspend fun insertRes(res: Res) {
        withContext(Dispatchers.IO) {
            database.insertRes(res)
        }
    }

    protected suspend fun getContainer(qr: String): Res? {
        return withContext(Dispatchers.IO) {
            database.getContainer(qr)
        }
    }

    protected suspend fun getNonContainer(qr: String): Res? {
        return withContext(Dispatchers.IO) {
            database.getNonContainer(qr)
        }
    }

    protected suspend fun updateRes(res: Res) {
        return withContext(Dispatchers.IO) {
            database.updateRes(res)
        }
    }

    protected suspend fun deleteRes(qr: String) {
        return withContext(Dispatchers.IO) {
            val contents = database.getResWithContainedRes(qr)!!
            for (c in contents.containedRes) {
                c.locationQR = null
            }
            database.updateAllRes(contents.containedRes)
            database.deleteRes(qr)
        }
    }

    protected suspend fun canConnect(addr: InetSocketAddress): Boolean {
        return withContext(Dispatchers.IO) {
            Printer(getApplication(), addr).use { printer ->
                return@withContext printer.canConnect()
            }
        }
    }

    protected suspend fun print(addr: InetSocketAddress, str: String): Boolean {
        return withContext(Dispatchers.IO) {
            Printer(getApplication(), addr).use { printer ->
                return@withContext printer.print(str)
            }
        }
    }
}