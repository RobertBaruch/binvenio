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

import org.babbageboole.binvenio.NetworkGetter
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class ZebraPrinterFactory @Inject constructor(var networkGetter: NetworkGetter) : PrinterFactory {
    override fun get(): Printer {
        return ZebraPrinter(networkGetter)
    }
}

class ZebraPrinter(private val networkGetter: NetworkGetter) : Printer {
    private var socket: Socket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null
    private lateinit var addr: InetSocketAddress

    override fun open(addr: InetSocketAddress): Printer? {
        this.addr = addr
        if (tryConnect() != null) return this
        return null
    }

    override fun canConnect(): Boolean {
        return tryConnect() != null
    }

    override fun print(data: String): Boolean {
        // It could take a second or so to print the label, so give it more than enough time.
        val rsp = sendAndAwaitResponse(data, 3000)
        return rsp != null && rsp == "OK:printed"
    }

    override fun isDHCPEnabled(): Boolean? {
        val rsp = sendAndAwaitResponse("""! U1 getvar "ip.dhcp.enable"\n""", 500)
        return rsp?.let { return rsp == """"on"""" }
    }

    private fun tryConnect(): Socket? {
        return connect(500)?.let {
            return checkHostInformation()?.let { socket }
        }
    }

    private fun checkHostInformation(): String? {
        return sendAndAwaitResponse("~HI\n", 500)
    }

    private fun connect(timeoutMillis: Int): Socket? {
        val network = networkGetter.getNetwork() ?: return null
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

    private fun sendAndAwaitResponse(data: String, timeoutMillis: Int): String? {
        if (inStream == null || outStream == null) return null
        return try {
            outStream!!.write(data.toByteArray())
            val reader = BufferedReader(InputStreamReader(inStream))
            socket!!.soTimeout = timeoutMillis
            val line = reader.readLine()
            Timber.i("Printer sent reply: $line")
            line
        } catch (ex: Exception) {
            Timber.i("Failed to read: $ex")
            null
        }
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