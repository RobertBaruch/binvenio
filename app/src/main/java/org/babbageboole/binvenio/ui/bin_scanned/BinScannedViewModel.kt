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

package org.babbageboole.binvenio.ui.bin_scanned

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.babbageboole.binvenio.ui.CommonViewModel
import timber.log.Timber

class BinScannedViewModel(
    val qr: String,
    application: Application
) : CommonViewModel(application) {
    var name = MutableLiveData<String>()
    private var _loc = MutableLiveData<String>()
    private var _context = application.applicationContext
    val locStr = Transformations.map(_loc) { loc ->
        loc ?: _context.resources.getString(R.string.location_text_none)
    }
    val addRemoveButtonStr = Transformations.map(_loc) { loc ->
        when (loc) {
            null -> _context.resources.getString(R.string.add_to_loc_button_text)
            else -> _context.resources.getString(R.string.remove_from_loc_button_text)
        }
    }
    var contents = database.getContentsLiveView(qr)

    init {
        uiScope.launch {
            val container = getContainer(qr)!!
            name.value = container.name
            _loc.value = null
            if (container.locationQR != null) {
                val location = getContainer(container.locationQR!!)!!
                _loc.value = location.name
            }
        }
    }

    fun onAddRemove() {
        Timber.i("Adding or removing qr ${qr}: ${name.value} for location")
        if (_loc.value != null) {
            uiScope.launch {
                val container = getContainer(qr)!!
                container.locationQR = null
                updateRes(container)
                _showMsg.value = "Don't forget to remove it!"
                _goBackToMain.value = true
            }
            return
        }
        _bringUpScanner.value = true
    }

    fun onSaveChanges() {
        uiScope.launch {
            val container = getContainer(qr)!!
            container.name = name.value!!
            updateRes(container)
            _showMsg.value = "Changes saved!"
        }
    }

    fun onDelete() {
        // TODO: Show dialog for are you sure
        uiScope.launch {
            deleteRes(qr)
            _showError.value = "Container has been deleted! Any contents now have no location!"
            _goBackToMain.value = true
        }
    }

    override fun onScanned(format: String, content: String) {
        if (format != "QR_CODE") {
            _showError.value = "That was not recognized as a QR code."
            return
        }
        onQRScanned(content)
    }

    private fun onQRScanned(binQR: String) {
        uiScope.launch {
            val container = getContainer(binQR)
            if (container == null) {
                _showError.value = "You didn't scan a location (container)."
                return@launch
            }
            val loc = container.locationQR
            if (loc != null && cycleDetected(loc)) {
                _showError.value = "That is already inside the container!"
                return@launch
            }
            val item = getContainer(qr)!!
            item.locationQR = binQR
            updateRes(item)
            _showMsg.value = "Don't forget to put it in location '${item.name}'!"
            _goBackToMain.value = true
        }
    }

    // Ensures that you're not about to put qr into the given location where there is already a
    // link of locations from the given location to qr.
    private suspend fun cycleDetected(locationQR: String?) : Boolean {
        return withContext(Dispatchers.IO) {
            var locQR = locationQR
            while (locQR != null) {
                if (locQR == qr) {
                    return@withContext true
                }
                var container = database.getContainer(locQR)
                locQR = container?.locationQR
            }
            false
        }
    }

    fun onPrintSticker() {
        printSticker(qr, name.value!!)
    }

    override fun onCancelSearchPrinter() {
        _showError.value = "Printer not found."
    }

    override fun onPrinterFound() {
        printSticker(qr, name.value!!)
    }
}
