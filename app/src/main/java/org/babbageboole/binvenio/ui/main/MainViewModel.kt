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

package org.babbageboole.binvenio.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.ui.CommonViewModel
import timber.log.Timber
import java.io.*
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class MainViewModel(
    application: Application
) : CommonViewModel(application) {

    private var _showNukeConfirm = MutableLiveData<Boolean>()
    val showNukeConfirm: LiveData<Boolean>
        get() = _showNukeConfirm

    private var _showSearchPrinter = MutableLiveData<Boolean>()
    val showSearchPrinter: LiveData<Boolean>
        get() = _showSearchPrinter

    private var _navigateToAddContainer = MutableLiveData<String?>()
    val navigateToAddContainer: LiveData<String?>
        get() = _navigateToAddContainer

    private var _navigateToAddItem = MutableLiveData<String?>()
    val navigateToAddItem: LiveData<String?>
        get() = _navigateToAddItem

    private var _navigateToContainerScanned = MutableLiveData<String?>()
    val navigateToContainerScanned: LiveData<String?>
        get() = _navigateToContainerScanned

    private var _navigateToItemScanned = MutableLiveData<String?>()
    val navigateToItemScanned: LiveData<String?>
        get() = _navigateToItemScanned

    private var _navigateToFinder = MutableLiveData<Boolean>()
    val navigateToFinder: LiveData<Boolean>
        get() = _navigateToFinder

    private var _pickFileSaveLocation = MutableLiveData<String?>()
    val pickFileSaveLocation: LiveData<String?>
        get() = _pickFileSaveLocation

    private var _pickFileLoadLocation = MutableLiveData<Boolean>()
    val pickFileLoadLocation: LiveData<Boolean>
        get() = _pickFileLoadLocation

    override fun onNavigationComplete() {
        super.onNavigationComplete()
        _navigateToAddItem.value = null
        _navigateToAddContainer.value = null
        _navigateToContainerScanned.value = null
        _navigateToItemScanned.value = null
        _navigateToFinder.value = false
    }

    fun onFind() {
        _navigateToFinder.value = true
    }

    fun onAddItem() {
        //_why.value = "add-item"
        //_bringUpScanner.value = true
        _navigateToAddItem.value = UUID.randomUUID().toString()
    }

    fun onAddContainer() {
//        _why.value = "add-bin"
//        _bringUpScanner.value = true
        _navigateToAddContainer.value = UUID.randomUUID().toString()
    }

    fun onNuke() {
        _showNukeConfirm.value = true
    }

    fun nukeConfirmShown() {
        _showNukeConfirm.value = false
    }

    fun onNukeConfirmed() {
        uiScope.launch {
            nuke()
            _showError.value = "The database has been cleared!"
        }
    }

    fun onSearchPrinter() {
        _showSearchPrinter.value = true
    }

    fun searchPrinterShown() {
        _showSearchPrinter.value = false
    }

    fun onExport() {
        // Close database. We can do this because when the file picker activity is started, our activity is
        // stopped, and when our app picks up again, our fragment's onCreateView gets called, thus re-opening
        // an instance.
        _pickFileSaveLocation.value = ResDatabase.getSaveName()
    }

    fun onImport() {
        // Close database. We can do this because when the file picker activity is started, our activity is
        // stopped, and when our app picks up again, our fragment's onCreateView gets called, thus re-opening
        // an instance.
        Timber.i("onImport")
        _pickFileLoadLocation.value = true
    }

    fun onPickFileSaveLocationLaunched() {
        _pickFileSaveLocation.value = null
    }

    fun onPickFileSaveLocationDone(uri: Uri) {
        uiScope.launch {
            exportDb(uri)
            _showMsg.value = "Database exported."
        }
    }

    fun onPickFileLoadLocationLaunched() {
        Timber.i("pick file load launched")
        _pickFileLoadLocation.value = false
    }

    fun onPickFileLoadLocationDone(uri: Uri) {
        uiScope.launch {
            importDb(uri)
            _showMsg.value = "Database imported."
        }
    }

    override fun onScanned(format: String, content: String) {
        if (format != "QR_CODE") {
            _showError.value = "That was not recognized as a QR code."
            return
        }
        onQRScanned(content)
    }

    private fun onQRScanned(qr: String) {
        Timber.i("QR scanned: $qr, why: ${_why.value ?: "null"}")

        uiScope.launch {
            val res = getRes(qr)

            if (_why.value != null) {
                when {
                    res != null -> _showError.value = "That QR is already registered."
                    _why.value == "add-item" -> _navigateToAddItem.value = qr
                    else -> _navigateToAddContainer.value = qr
                }
                return@launch
            }

            when {
                res == null -> _showError.value = "That QR was not found."
                res.isContainer -> _navigateToContainerScanned.value = qr
                else -> _navigateToItemScanned.value = qr
            }
        }
    }

    private suspend fun nuke() {
        return withContext(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext

            ResDatabase.getInstance(context).clearAllTables()
        }
    }

    private suspend fun exportDb(uri: Uri) {
        withContext(Dispatchers.IO) {
            ResDatabase.closeInstance()
            Timber.i("Closed database for export")

            val context = getApplication<Application>().applicationContext
            val file = ResDatabase.getPath(context)
            val contentResolver = context.contentResolver

            contentResolver.openFileDescriptor(uri, "w")?.use { wr ->
                GZIPOutputStream(FileOutputStream(wr.fileDescriptor)).use { out ->
                    FileInputStream(file).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            origin.copyTo(out, 1024)
                        }
                    }
                }
            }
        }
        Timber.i("Done export")
    }

    private suspend fun importDb(uri: Uri) {
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            Timber.i("importDB: file")
            val file = File(context.cacheDir, "tmp.db")
            file.deleteOnExit()

            val contentResolver = context.contentResolver
            contentResolver.openFileDescriptor(uri, "r")?.use { rd ->
                GZIPInputStream(FileInputStream(rd.fileDescriptor)).use { data ->
                    FileOutputStream(file).use { wr ->
                        BufferedOutputStream(wr).use { dest ->
                            data.copyTo(dest, 1024)
                        }
                    }
                }
            }
            Timber.i("importDB: importing from ${file.path}")
            val db = ResDatabase.import(context, file)
            file.delete()
            database = db.resDatabaseDao
        }
    }

    override fun onPrinterFound() {
        _showMsg.value = "Printer found!"
    }

    override fun onCancelSearchPrinter() {
        _showError.value = "Printer not found."
    }
}