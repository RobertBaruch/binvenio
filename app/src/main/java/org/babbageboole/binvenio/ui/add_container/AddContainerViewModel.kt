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

package org.babbageboole.binvenio.ui.add_container

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.babbageboole.binvenio.ui.CommonViewModel
import timber.log.Timber

class AddContainerViewModel(
    val qr: String,
    application: Application
) : CommonViewModel(application) {
    var name = MutableLiveData<String>("")
    var qrVal = MutableLiveData<String>(qr)

    fun onAdd() {
        Timber.i("Adding qr ${qrVal.value}: ${name.value}")
        val qr = qrVal.value!!

        uiScope.launch {
            var res = getRes(qr)
            if (res != null) {
                var what = "container"
                if (!res.isContainer) what = "item"
                _showError.value = "QR code already assigned to $what '${res.name}'"
                _goBackToMain.value = true
                return@launch
            }
            res = Res(qr = qr, name = name.value!!, isContainer = true)
            insertRes(res)
            _goBackToMain.value = true
        }
    }

    fun onPrintAndAdd() {
        uiScope.launch {
            var res = getRes(qr)
            if (res != null) {
                var what = "container"
                if (!res.isContainer) what = "item"
                _showError.value = "QR code already assigned to $what '${res.name}'"
                _goBackToMain.value = true
                return@launch
            }
            res = Res(qr = qr, name = name.value!!, isContainer = true)
            insertRes(res)

            // This might result in a search printer dialog, in which case we'll call
            // printSticker again if we find the printer.
            printSticker(qr, name.value!!)
        }
    }

    fun onCancel() {
        Timber.i("Cancelled adding bin")
        _goBackToMain.value = true
    }

    override fun onCancelSearchPrinter() {
        _showError.value = "Printer not found. Container has been saved."
    }

    override fun onPrinterFound() {
        printSticker(qr, name.value!!)
    }

    override fun onPrintComplete() {
        Timber.i("onPrintComplete")
        super.onPrintComplete()
        _goBackToMain.value = true
    }
}
