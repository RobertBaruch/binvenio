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

package org.babbageboole.binvenio.ui.item_scanned

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.databinding.ItemScannedFragmentBinding
import org.babbageboole.binvenio.ui.CommonFragment
import org.babbageboole.binvenio.ui.ViewModelFactory
import timber.log.Timber

class ItemScannedFragment : CommonFragment<ItemScannedViewModel, ItemScannedFragmentBinding>() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: ItemScannedFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.item_scanned_fragment, container, false
        )
        val application = requireNotNull(this.activity).application
        val arguments = ItemScannedFragmentArgs.fromBundle(requireArguments())
        val viewModelFactory = ViewModelFactory(application, arguments.qr)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ItemScannedViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        addStandardObservers()
        addNavigationObserver(
            viewModel.goBackToMain,
            ItemScannedFragmentDirections.actionItemScannedFragmentToMainFragment()
        )

        Timber.i("--- fragment onCreateView called")
        return binding.root
    }
}