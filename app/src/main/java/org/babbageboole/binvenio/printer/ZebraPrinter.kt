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

package org.babbageboole.binvenio.printer

import android.net.Network
import org.babbageboole.binvenio.BinvenioApplication
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class ZebraPrinter(private val application: BinvenioApplication, private val addr: InetSocketAddress) : Printer {
    private var socket: Socket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null

    override fun canConnect(): Boolean {
        return tryConnect() != null
    }

    override fun print(data: String): Boolean {
        if (outStream == null) tryConnect() ?: return false
        return try {
            Timber.i("sending data to printer")
            outStream!!.write(data.toByteArray())
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun tryConnect(): Socket? {
        return connect(500)?.let {
            return checkHostInformation()?.let { socket }
        }
    }

    private fun connect(timeoutMillis: Int): Socket? {
        close()
        val network = getWifiNetwork() ?: return null
        return try {
            var s: Socket? = network.socketFactory.createSocket() ?: return null
            s!!.connect(addr, timeoutMillis)
            s.soTimeout = 500 /* milliseconds */
            inStream = s.getInputStream()
            outStream = s.getOutputStream()
            socket = s
            Timber.i("Connected to $addr")
            socket
        } catch (ex: Exception) {
            Timber.i("Failed to connect to $addr")
            close()
            null
        }
    }

    private fun checkHostInformation(): String? {
        val stringBuilder = StringBuilder()

        if (inStream == null || outStream == null) return null
        return try {
            outStream!!.write("~HI\n".toByteArray())
            var c = inStream!!.read()
            if (c != 2) {
                Timber.i("Expected 2 (STX) but got $c")
                return null
            }
            while (c != 3 /* ETX */) {
                stringBuilder.append(c.toChar())
                if (inStream!!.available() <= 0) {
                    Timber.i("Failed to end with ETX")
                    break
                }
                c = inStream!!.read()
            }
            Timber.i("Printer sent reply to host info request")
            stringBuilder.deleteCharAt(0).toString()
        } catch (ex: Exception) {
            Timber.i("Failed to read: $ex")
            null
        }
    }

    private fun getWifiNetwork(): Network? {
        return application.getNetwork()
    }

    override fun close() {
        outStream?.close()
        inStream?.close()
        socket?.close()
        inStream = null
        outStream = null
        socket = null
    }
}