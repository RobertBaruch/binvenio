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
import android.content.Context
import android.net.Network
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.*
import org.babbageboole.binvenio.NetworkGetter
import org.babbageboole.binvenio.PrinterAddressHolder
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.printer.Printer
import org.babbageboole.binvenio.printer.PrinterFactory
import timber.log.Timber
import java.net.InetSocketAddress
import java.security.SecureRandom

abstract class CommonViewModel(
    application: Application
) : AndroidViewModel(application) {
    companion object {
        const val printables =
            "`1234567890-=!@#$%&*()_+abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ[]{}|;:'\",<.>/?`\\"
    }

    protected var database = ResDatabase.getInstance(application).resDatabaseDao

    protected var _bringUpScanner = MutableLiveData<Boolean>()
    val bringUpScanner: LiveData<Boolean>
        get() = _bringUpScanner

    protected var _showPrintSearch = MutableLiveData<Boolean>()
    val showPrintSearch: LiveData<Boolean>
        get() = _showPrintSearch

    protected var _showDHCPWarning = MutableLiveData<Boolean>()
    val showDHCPWarning: LiveData<Boolean>
        get() = _showDHCPWarning

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

    // This is how you inject things into classes that are not directly supported for injection
    // by Hilt. This works with the Module installed from BinvenioModule to return a concrete
    // implementation of PrinterFactory.
    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    interface PrinterFactoryEntryPoint {
        fun printerFactory(): PrinterFactory
        fun networkGetter(): NetworkGetter
        fun printerAddressHolder(): PrinterAddressHolder
    }

    protected fun getPrinterFactory(appContext: Context): PrinterFactory {
        Timber.i("Getting a printer factory via entry point")
        // Use fromApplication because the InstallIn above was to the application component.
        val entryPoint =
            EntryPointAccessors.fromApplication(appContext, PrinterFactoryEntryPoint::class.java)
        return entryPoint.printerFactory()
    }

    protected fun getNetworkGetter(appContext: Context): NetworkGetter {
        val entryPoint =
            EntryPointAccessors.fromApplication(appContext, PrinterFactoryEntryPoint::class.java)
        return entryPoint.networkGetter()
    }

    protected fun getPrinterAddressHolder(appContext: Context): PrinterAddressHolder {
        val entryPoint =
            EntryPointAccessors.fromApplication(appContext, PrinterFactoryEntryPoint::class.java)
        return entryPoint.printerAddressHolder()
    }

    protected fun generateRandomQR(): String {
        val rand = SecureRandom()
        return IntRange(
            0,
            9
        ).joinToString(separator = "") { "${printables[rand.nextInt(printables.length)]}" }
    }

    /**
     * Called when the ViewModel is dismantled.
     * At this point, we want to cancel all coroutines;
     * otherwise we end up with processes that have nowhere to return to
     * using memory and resources.
     */
    override fun onCleared() {
        super.onCleared()
        Timber.i("onCleared called")
        viewModelJob.cancel()
        Timber.i("viewModelJob cancelled")
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

    protected fun getNetwork(): Network? {
        return getNetworkGetter(getApplication<Application>().applicationContext).getNetwork()
    }

    protected fun hasNetwork(): Boolean {
        return getNetworkGetter(getApplication<Application>().applicationContext).hasNetwork()
    }

    protected fun getPrinterAddr(): InetSocketAddress? {
        return getPrinterAddressHolder(getApplication<Application>().applicationContext).getPrinterAddr()
    }

    protected fun setPrinterAddr(addr: InetSocketAddress?) {
        getPrinterAddressHolder(getApplication<Application>().applicationContext).setPrinterAddr(
            addr
        )
    }

    // The x coordinate depends on the paper width.
    // This is good only for a 200dpi printer,
    // 2.25" wide (i.e. 450 dots across),
    // on 1.5" x 0.5" paper (300 x 100 centered on x=225,
    // so x=75 to x=375).
    // With an 10 dot wide font and 2 dot gap, only 16 characters
    // work starting from x=165 (192 dots, to x=357)
    // The format of qr may be xAABBCC[more hex digits...] in
    // which case it will be converted to x+binary.
    fun printSticker(qr: String, name: String): Boolean {
        if (!hasNetwork()) {
            _showError.value = "No wireless connectivity"
            return false
        }

        val addr = getPrinterAddr()
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

        uiScope.launch {
            if (printSticker(addr, qr, lines)) {
                _printComplete.value = true
            } else {
                _showError.value = "Print failed."
                setPrinterAddr(null)
            }
        }
        return true
    }

    open fun onPrinterFound() {}

    open fun onCancelSearchPrinter() {}

    fun onPrinterHasDHCP() {
        _showDHCPWarning.value = true
    }

    fun dhcpWarningShown() {
        _showDHCPWarning.value = false
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

    protected suspend fun canConnect(addr: InetSocketAddress): Printer? {
        return withContext(Dispatchers.IO) {
            Timber.i("canConnect to $addr?")
            val printerFactory = getPrinterFactory(getApplication<Application>().applicationContext)
            return@withContext printerFactory.get().open(addr)
        }
    }

    private suspend fun printSticker(addr: InetSocketAddress, qr: String, lines: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            val printerFactory = getPrinterFactory(getApplication<Application>().applicationContext)
            printerFactory.get().open(addr)?.use { printer ->
                val qrCmd = "^BQN,2,3^FDQA,$qr^FS"
                var zero = printer.getTopLeftCoords()

                val str = """
                    ^XA
                    ^FO${zero.x},${zero.y}
                    $qrCmd
                    ^FO${zero.x+75},${zero.y+5}
                    ^ADN,18,10
                    ^FD${lines[0]}^FS
                    ^FO${zero.x+75},${zero.y+25}
                    ^ADN,18,10
                    ^FD${lines[1]}^FS
                    ^FO${zero.x+75},${zero.y+45}
                    ^ADN,18,10
                    ^FD${lines[2]}^FS
                    ^FO${zero.x+75},${zero.y+65}
                    ^ADN,18,10
                    ^FD${lines[3]}^FS
                    ^FN0^FD^FS
                    ^FH_^HV0,8,OK:,_0D_0A,L^FS
                    ^XZ
                """.trimIndent()
                Timber.i("To printer: $str")
                return@withContext printer.print(str)
            }
            false
        }
    }
}