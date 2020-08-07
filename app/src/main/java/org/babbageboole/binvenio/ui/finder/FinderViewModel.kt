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

package org.babbageboole.binvenio.ui.finder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.*
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResDatabaseDao
import org.babbageboole.binvenio.database.ResWithContainingRes
import org.babbageboole.binvenio.ui.CommonViewModel
import timber.log.Timber

class FinderViewModel(
    qr: String,
    application: Application
) : CommonViewModel(application) {
    var name = MutableLiveData<String>()
    private var _loc = MutableLiveData<String>()
    private var _context = application.applicationContext

    val queryTerm = MutableLiveData<String>()
    val contents = Transformations.switchMap(queryTerm) { q ->
        database.findResWithContainerResByPartialNameLive(q)
    }

    private var _navigateToItemSelected = MutableLiveData<String?>()
    val navigateToItemSelected: LiveData<String?>
        get() = _navigateToItemSelected

    private var _navigateToContainerSelected = MutableLiveData<String?>()
    val navigateToContainerSelected: LiveData<String?>
        get() = _navigateToContainerSelected

    fun onItemClicked(res: ResWithContainingRes) {
        Timber.i("Res clicked: ${res.name}")
        when (res.isContainer) {
            false -> _navigateToItemSelected.value = res.qr
            else -> _navigateToContainerSelected.value = res.qr
        }
    }

    override fun onNavigationComplete() {
        super.onNavigationComplete()
        _navigateToItemSelected.value = null
        _navigateToContainerSelected.value = null
    }
}
