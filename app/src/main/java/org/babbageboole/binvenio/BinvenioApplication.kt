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

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.net.InetSocketAddress

interface PrinterAddressHolder {
    fun getPrinterAddr(): InetSocketAddress?
    fun setPrinterAddr(addr: InetSocketAddress?)
}

class RealPrinterAddressHolder : PrinterAddressHolder {
    private var addr: InetSocketAddress? = null

    override fun getPrinterAddr(): InetSocketAddress? {
        return addr
    }

    override fun setPrinterAddr(addr: InetSocketAddress?) {
        this.addr = addr
    }
}

interface NetworkGetter {
    fun getNetwork(): Network?
}

class RealNetworkGetter(private val connectivityMonitor: ConnectivityMonitor) : NetworkGetter {
    override fun getNetwork(): Network? {
        Timber.i("Getting network from connectivity monitor")
        return connectivityMonitor.getNetwork()
    }
}

interface ConnectivityMonitor {
    fun startMonitoring()
    fun stopMonitoring()
    fun getNetwork(): Network?
}

class RealConnectivityMonitor(private val applicationContext: Context) : ConnectivityMonitor {
    private var _network = MutableLiveData<Network?>()
    val network: LiveData<Network?>
        get() = _network

    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(n: Network) {
            super.onAvailable(n)
            synchronized(this) {
                // Cannot use _network.value because this is not in the main thread.
                _network.postValue(n)
                Timber.i("Network is up")
            }
        }

        override fun onLost(n: Network) {
            super.onLost(n)
            synchronized(this) {
                _network.postValue(null)
                Timber.i("Network is down")
            }
        }
    }

    override fun startMonitoring() {
        Timber.i("ConnectivityMonitor: starting monitoring")
        val connMgr =
            ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)
        val netRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connMgr?.registerNetworkCallback(netRequest, networkCallback)
    }

    override fun stopMonitoring() {
        Timber.i("ConnectivityMonitor: stopping monitoring")
        val connMgr =
            ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)
        connMgr?.unregisterNetworkCallback(networkCallback)
        synchronized(this) {
            _network.postValue(null)
        }
    }

    override fun getNetwork(): Network? {
        synchronized(this) {
            return _network.value
        }
    }
}

@HiltAndroidApp
class BinvenioApplication : Application() {
    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
    }
}