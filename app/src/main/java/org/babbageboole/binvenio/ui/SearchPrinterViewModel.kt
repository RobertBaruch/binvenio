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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.babbageboole.binvenio.printer.Printer
import timber.log.Timber
import java.net.InetAddress
import java.net.InetSocketAddress

class SearchPrinterViewModel(application: Application) : CommonViewModel(application) {
    companion object {
        const val NO_PRINTER = 0
        const val HAS_DHCP = 1
        const val OK = 2
        const val NO_NETWORK = 3
    }

    private var _addr = MutableLiveData<InetSocketAddress?>(null)
    val addr: LiveData<InetSocketAddress?>
        get() = _addr
    val addrStr = Transformations.map(_addr) {
        if (it != null) it.address.hostAddress
        else ""
    }
    private var _progress = MutableLiveData<Int>(0)
    val progress: LiveData<Int>
        get() = _progress

    private var _success = MutableLiveData<Int?>(null)
    val success: LiveData<Int?>
        get() = _success


    fun onSearch() {
        setPrinterAddr(null)
        Timber.i("Checking if we have a network")
        if (!hasNetwork()) {
            _success.value = NO_NETWORK
            return
        }
        val bs = ByteArray(4)
        bs[0] = 192.toByte()
        bs[1] = 168.toByte()
        bs[2] = 1
        uiScope.launch {
            try {
                for (b in 1.rangeTo(255)) {
                    bs[3] = b.toByte()
                    val addr = InetSocketAddress(InetAddress.getByAddress(bs), 6101)
                    _addr.value = addr
                    Timber.i("Checking $addr")

                    canConnect(addr)?.use {
                        setPrinterAddr(addr)
                        if (isPrinterDHCPEnabled(it) != false) {
                            _success.value = HAS_DHCP
                        } else {
                            _success.value = OK
                        }
                        return@launch
                    }
                    _progress.value = (100 * b) / 255
                }
                _success.value = NO_PRINTER
            } catch (ex: Exception) {
                // During testing you can get a CancelledException, or an IllegalStateException
                // because "The component was not created. Check that you have added the HiltAndroidRule" --
                // probably when a test shuts down, from EntryPointAccessors.fromApplication.
                Timber.i("Caught exception: $ex")
                Timber.e(ex)
            }
        }
        Timber.i("Returning from onSearch")
    }

    private suspend fun isPrinterDHCPEnabled(printer: Printer): Boolean? {
        return withContext(Dispatchers.IO) {
            printer.isDHCPEnabled()
        }
    }
}