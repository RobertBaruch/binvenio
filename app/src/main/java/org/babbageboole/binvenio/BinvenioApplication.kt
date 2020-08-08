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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.net.InetSocketAddress

class BinvenioApplication : Application() {
    var printerAddr: InetSocketAddress? = null
    private var network: Network? = null
    private var networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(n: Network) {
            super.onAvailable(n)
            synchronized(this) {
                network = n
                Timber.i("Network is up")
            }
        }

        override fun onLost(n: Network) {
            super.onLost(n)
            synchronized(this) {
                network = null
                Timber.i("Network is down")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        watchForWifiNetwork()
    }

    override fun onTerminate() {
        val connMgr =
            ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)
        connMgr?.unregisterNetworkCallback(networkCallback)
        super.onTerminate()
    }

    private fun watchForWifiNetwork() {
        val connMgr =
            ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)
        val netRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connMgr?.registerNetworkCallback(netRequest, networkCallback)
    }

    fun getNetwork(): Network? {
        synchronized(networkCallback) {
            return network
        }
    }
}
