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

import android.util.JsonReader
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.babbageboole.binvenio.NetworkGetter
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.StringBuilder
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
        val rsp = sendAndAwaitResponseJSON("{}{\"ip.dhcp.enable\":null}\n",500)
        val v = rsp?.get("ip.dhcp.enable")?.asString
        return v?.let { return v == "on" }
    }

    private fun tryConnect(): Socket? {
        return connect(500)?.let {
            return checkHostInformation()?.let { socket }
        }
    }

    private fun checkHostInformation(): String? {
        return sendAndAwaitResponse("~HI\n", 3000)
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

    private fun sendAndAwaitResponseJSON(data: String, timeoutMillis: Int): JsonObject? {
        if (inStream == null || outStream == null) return null
        return try {
            outStream!!.write(data.toByteArray())
            socket!!.soTimeout = timeoutMillis
            val s = readUnbufferedJSON() ?: return null
            val gson = Gson()
            gson.fromJson(s, JsonObject::class.java)
        } catch (ex: Exception) {
            Timber.i("Failed to read: $ex")
            Timber.e(ex)
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

    // Keeps track of curly brace level, except inside strings. When curly brace level
    // reaches zero, we've read an object. We have to do this because all the other
    // JSON readers (GSON, JSONReader) buffer characters or wait until end of stream.
    //
    // Within JSON strings may be curly braces, but they do not have to be escaped.
    // The double-quote character does need to be escaped.
    private fun readUnbufferedJSON(): String? {
        val instr = inStream ?: return null
        val s = StringBuilder()
        var c = instr.read()
        if (c == -1 || c != '{'.toInt()) return null

        var braceLvl = 1
        var quoteLvl = 0
        var escapeLvl = 0

        s.append(c.toChar())
        while (braceLvl > 0) {
            c = instr.read()
            if (escapeLvl == 0) {
                when (c) {
                    -1 -> return null
                    '"'.toInt() -> quoteLvl = 1 - quoteLvl
                    '{'.toInt() -> if (quoteLvl == 0) braceLvl++
                    '}'.toInt() -> if (quoteLvl == 0) braceLvl--
                    '\\'.toInt() -> escapeLvl++
                }
            } else {
                when (c) {
                    -1 -> return null
                    else -> escapeLvl--
                }
            }
            s.append(c.toChar())
        }
        return s.toString()
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