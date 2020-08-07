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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.babbageboole.binvenio.ui.add_container.AddContainerViewModel
import org.babbageboole.binvenio.ui.add_item.AddItemViewModel
import org.babbageboole.binvenio.ui.bin_scanned.BinScannedViewModel
import org.babbageboole.binvenio.ui.finder.FinderViewModel
import org.babbageboole.binvenio.ui.item_scanned.ItemScannedViewModel
import org.babbageboole.binvenio.ui.main.MainViewModel

class ViewModelFactory(
    private val application: Application,
    private val qr: String = ""
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(FinderViewModel::class.java) -> return FinderViewModel(qr, application) as T
            modelClass.isAssignableFrom(AddItemViewModel::class.java) -> return AddItemViewModel(qr, application) as T
            modelClass.isAssignableFrom(AddContainerViewModel::class.java) -> return AddContainerViewModel(qr, application) as T
            modelClass.isAssignableFrom(BinScannedViewModel::class.java) -> return BinScannedViewModel(qr, application) as T
            modelClass.isAssignableFrom(ItemScannedViewModel::class.java) -> return ItemScannedViewModel(qr, application) as T
            modelClass.isAssignableFrom(SearchPrinterViewModel::class.java) -> return SearchPrinterViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
