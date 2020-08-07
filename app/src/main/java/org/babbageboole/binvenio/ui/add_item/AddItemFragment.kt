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

package org.babbageboole.binvenio.ui.add_item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.databinding.AddItemFragmentBinding
import org.babbageboole.binvenio.ui.CommonFragment
import org.babbageboole.binvenio.ui.NukeDialogFragment
import org.babbageboole.binvenio.ui.SearchPrinterDialogFragment
import org.babbageboole.binvenio.ui.ViewModelFactory
import timber.log.Timber

class AddItemFragment : CommonFragment<AddItemViewModel, AddItemFragmentBinding>() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: AddItemFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.add_item_fragment, container, false
        )
        val application = requireActivity().application
        val arguments = AddItemFragmentArgs.fromBundle(requireArguments())
        val viewModelFactory = ViewModelFactory(application, arguments.qr)
        viewModel = ViewModelProvider(this, viewModelFactory).get(AddItemViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        addStandardObservers()
        addNavigationObserver(
            viewModel.goBackToMain, AddItemFragmentDirections
                .actionAddItemFragmentToMainFragment()
        )

        Timber.i("--- fragment onCreateView called")
        return binding.root
    }
}