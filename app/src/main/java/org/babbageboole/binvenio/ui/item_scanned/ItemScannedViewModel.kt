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

package org.babbageboole.binvenio.ui.item_scanned

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.babbageboole.binvenio.ui.CommonViewModel
import timber.log.Timber

class ItemScannedViewModel(
    val qr: String,
    application: Application
) : CommonViewModel(application) {
    var name = MutableLiveData<String>()
    private var _loc = MutableLiveData<String>()
    private var _context = application.applicationContext
    var count = MutableLiveData<Long>()
    val locStr = Transformations.map(_loc) { loc ->
        loc ?: _context.resources.getString(R.string.location_text_none)
    }
    val addRemoveButtonStr = Transformations.map(_loc) { loc ->
        when (loc) {
            null -> _context.resources.getString(R.string.add_item_to_loc_button_text)
            else -> _context.resources.getString(R.string.remove_item_from_loc_button_text)
        }
    }

    init {
        uiScope.launch {
            val item = getNonContainer(qr)!!
            name.value = item.name
            count.value = item.count
            _loc.value = null
            if (item.locationQR != null) {
                val container = getContainer(item.locationQR!!)!!
                _loc.value = container.name
            }
        }
    }

    fun onAddRemove() {
        Timber.i("Removing qr ${qr}: ${name.value} from container")
        if (_loc.value != null) {
            uiScope.launch {
                val item = getNonContainer(qr)!!
                item.locationQR = null
                updateRes(item)
                _showMsg.value = "Don't forget to take it out of container '${_loc.value}'!"
                _goBackToMain.value = true
            }
            return
        }
        _bringUpScanner.value = true
    }

    fun onDelete() {
        // TODO: Show dialog for are you sure
        uiScope.launch {
            deleteRes(qr)
            _showMsg.value = "Item has been deleted!"
            _goBackToMain.value = true
        }
    }

    fun onModifyCount(amount: Int) {
        var v = count.value!!
        v += amount
        if (v < 0) v = 0
        count.value = v
    }

    fun onSaveChanges() {
        uiScope.launch {
            val item = getNonContainer(qr)!!
            item.name = name.value!!
            item.count = count.value!!
            updateRes(item)
            _showMsg.value = "Changes saved!"
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
                _showError.value = "You didn't scan a known container."
                return@launch
            }
            val item = getNonContainer(qr)!!
            item.locationQR = binQR
            updateRes(item)
            _showMsg.value = "Don't forget to put it in container '${container.name}'!"
            _goBackToMain.value = true
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
