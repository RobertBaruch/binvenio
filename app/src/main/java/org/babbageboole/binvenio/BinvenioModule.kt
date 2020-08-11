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

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.babbageboole.binvenio.printer.PrinterFactory
import org.babbageboole.binvenio.printer.ZebraPrinterFactory
import timber.log.Timber
import javax.inject.Singleton

// This module provides all the "real" stuff the application depends on. It can be
// uninstalled and replaced by a module that provides test versions in an instrumented test.
// Note that if the test module doesn't provide all the same things this module does, you'll
// get a Dagger/MissingBinding error when compiling the test.
@Module
@InstallIn(ApplicationComponent::class)
object BinvenioModule {
    @Provides
    fun providePrinterFactory(networkGetter: NetworkGetter): PrinterFactory {
        Timber.i("Got a ZebraPrinterFactory")
        return ZebraPrinterFactory(networkGetter)
    }

    @Provides
    @Singleton
    fun provideConnectivityMonitor(@ApplicationContext appContext: Context): ConnectivityMonitor {
        return RealConnectivityMonitor(appContext)
    }

    @Provides
    @Singleton
    fun provideNetworkGetter(connectivityMonitor: ConnectivityMonitor): NetworkGetter {
        return RealNetworkGetter(connectivityMonitor)
    }

    @Provides
    @Singleton
    fun providePrinterAddressHolder() : PrinterAddressHolder {
        return RealPrinterAddressHolder()
    }
}
