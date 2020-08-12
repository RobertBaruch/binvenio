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

import android.net.Network
import org.babbageboole.binvenio.printer.Printer
import org.babbageboole.binvenio.printer.PrinterFactory
import java.net.InetSocketAddress
import javax.inject.Inject

class TestNetworkGetter @Inject constructor() : NetworkGetter {
    override fun getNetwork(): Network? {
        return null
    }

    override fun hasNetwork(): Boolean {
        return true
    }
}

class TestPrinterFactory @Inject constructor(var networkGetter: NetworkGetter) : PrinterFactory {
    override fun get(): Printer {
        return TestPrinter(networkGetter)
    }
}

class TestPrinter(private val networkGetter: NetworkGetter) : Printer {
    private var printSuccess: Boolean = true

    override fun open(addr: InetSocketAddress): Printer? {
        Thread.sleep(10)
        if (addr.address.address[3] == 100.toByte()) return this
        if (addr.address.address[3] == 101.toByte()) {
            printSuccess = false
            return this
        }
        return null
    }

    override fun canConnect(): Boolean? {
        return true
    }

    override fun print(data: String): Boolean {
        return printSuccess
    }

    override fun close() {
    }
}
