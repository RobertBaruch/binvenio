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

package org.babbageboole.binvenio.ui.add_item

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.ui.CommonViewModel
import timber.log.Timber

class AddItemViewModel(
    val qr: String,
    application: Application
) : CommonViewModel(application) {
    var name = MutableLiveData<String>("")
    var countStr = MutableLiveData<String>("0")
    val queue = Volley.newRequestQueue(application.applicationContext)
    private val requestTag = "add-item-isbn-lookup"


    fun onAdd() {
        Timber.i("Adding $qr, name: $name, count: $countStr")

        uiScope.launch {
            val res = getRes(qr)
            if (res != null) {
                var what = "container"
                if (!res.isContainer) what = "item"
                _showError.value = "QR code already assigned to $what '${res.name}'."
                _goBackToMain.value = true
                return@launch
            }
            insertRes(Res(qr = qr, name = name.value!!, count = countStr.value!!.toLong()))
            _goBackToMain.value = true
        }
    }

    fun onCancel() {
        Timber.i("Cancelled adding binnable")
        _goBackToMain.value = true
    }

    override fun onCleared() {
        super.onCleared()
        queue.cancelAll(requestTag)
    }

    fun onScanISBN() {
        _bringUpScanner.value = true
    }

    override fun onScanned(format: String, content: String) {
        if (format != "EAN_13" && format != "EAN_8") {
            _showError.value = "That was not recognized as an ISBN."
            return
        }
        onISBNScanned(content)
    }

    private fun onISBNScanned(isbn: String) {
        Timber.i("Scanned $isbn")
        val req = StringRequest(Request.Method.GET,
            "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn",
            Response.Listener<String> { onReceivedData(it) },
            Response.ErrorListener { onFailedData() })
        req.tag = requestTag
        queue.add(req)
    }

    private fun onReceivedData(data: String) {
        Timber.i("Data was received")
        var gson = Gson()
        val root = gson.fromJson(data, JsonObject::class.java)
        val totalItems = root.get("totalItems").asInt
        if (totalItems == 0) {
            _showError.value = "That ISBN was not found."
            return
        }
        val item = root.getAsJsonArray("items")[0].asJsonObject
        val volumeInfo = item.getAsJsonObject("volumeInfo")
        val title = volumeInfo.get("title").asString
        Timber.i("Title: $title")
        val authors = volumeInfo.getAsJsonArray("authors")
        val authorsStr: String = when {
            authors != null -> " (${authors.map { it.asString }.joinToString(", ")})"
            else -> ""
        }
        Timber.i("Authors: $authorsStr")

        if (authorsStr != "") name.value = "Book: $title$authorsStr"
        else name.value = "Book: $title"

        if (countStr.value == "0") countStr.value = "1"
    }

    private fun onFailedData() {
        Timber.i("Data failed was received")
        _showError.value = "ISBN lookup service failed."
    }

    fun onPrintAndAdd() {
        uiScope.launch {
            val res = getRes(qr)
            if (res != null) {
                var what = "container"
                if (!res.isContainer) what = "item"
                _showError.value = "QR code already assigned to $what '${res.name}'."
                _goBackToMain.value = true
                return@launch
            }
            insertRes(Res(qr = qr, name = name.value!!, count = countStr.value!!.toLong()))

            // This might result in a search printer dialog, in which case we'll call
            // printSticker again if we find the printer.
            printSticker(qr, name.value!!)
        }
    }

    override fun onCancelSearchPrinter() {
        _showError.value = "Printer not found. Item has been saved."
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
