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

package org.babbageboole.binvenio.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContract
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.ResDatabase
import org.babbageboole.binvenio.databinding.MainFragmentBinding
import org.babbageboole.binvenio.ui.CommonFragment
import org.babbageboole.binvenio.ui.NukeDialogFragment
import org.babbageboole.binvenio.ui.SearchPrinterDialogFragment
import timber.log.Timber

const val CREATE_FILE = 3971

class ChooseZipFileLocation : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/gzip"
            putExtra(Intent.EXTRA_TITLE, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return intent?.data
    }
}

class ChooseZipFileLocationForLoad : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        Timber.i("Creating intent for ChooseZipFileLocationForLoad")
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/gzip"
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        Timber.i("Result code $resultCode, intent $intent")
        if (resultCode != Activity.RESULT_OK) {
            Timber.i("result failed!")
            return null
        }
        return intent?.data
    }
}

class MainFragment : CommonFragment<MainViewModel, MainFragmentBinding>(),
    NukeDialogFragment.NukeDialogListener {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get a reference to the binding object and inflate the fragment views.
        val binding: MainFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.main_fragment, container, false
        )
        val application = this.requireActivity().application
        val viewModelFactory = MainViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        binding.mainViewModel = viewModel
        binding.lifecycleOwner = this

        addStandardObservers()
        addNavigationStringObserver(viewModel.navigateToAddContainer) {
            MainFragmentDirections.actionMainFragmentToAddContainerFragment(it)
        }
        addNavigationStringObserver(viewModel.navigateToAddItem) {
            MainFragmentDirections.actionMainFragmentToAddItemFragment(it)
        }
        addNavigationStringObserver(viewModel.navigateToContainerScanned) {
            MainFragmentDirections.actionMainFragmentToContainerScannedFragment(it)
        }
        addNavigationStringObserver(viewModel.navigateToItemScanned) {
            MainFragmentDirections.actionMainFragmentToItemScannedFragment(it)
        }
        addNavigationObserver(
            viewModel.navigateToFinder,
            MainFragmentDirections.actionMainFragmentToFinderFragment()
        )
        setHasOptionsMenu(true)

        viewModel.showNukeConfirm.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.nukeConfirmShown()
                val confirmDialog = NukeDialogFragment()
                // childFragmentManager is for showing fragments inside this fragment.
                // Plain old fragmentManager/supportFragmentManager is for fragments inside
                // the activity. We use the child fragment manager so that this fragment
                // gets the dialog callbacks.
                confirmDialog.show(childFragmentManager, "nuke")
            }
        })

        viewModel.showSearchPrinter.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.searchPrinterShown()
                val dialog = SearchPrinterDialogFragment()
                dialog.show(childFragmentManager, "search_printer")
            }
        })

        val exportDbSaveLocation = registerForActivityResult(ChooseZipFileLocation()) {
            if (it != null) {
                viewModel.onPickFileSaveLocationDone(it)
            }
        }

        val importDbLoadLocation = registerForActivityResult(ChooseZipFileLocationForLoad()) {
            if (it != null) {
                Timber.i("picked load file")
                viewModel.onPickFileLoadLocationDone(it)
            }
        }

        viewModel.pickFileSaveLocation.observe(viewLifecycleOwner, Observer {
            it?.let {
                exportDbSaveLocation.launch(it)
                viewModel.onPickFileSaveLocationLaunched()
            }
        })

        viewModel.pickFileLoadLocation.observe(viewLifecycleOwner, Observer {
            if (it) {
                Timber.i("Launch importDbLoadLocation")
                importDbLoadLocation.launch(null)
                viewModel.onPickFileLoadLocationLaunched()
            }
        })

        Timber.i("--- fragment onCreateView called")
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nuke -> {
                viewModel.onNuke()
                true
            }
            R.id.export_db -> {
                viewModel.onExport()
                true
            }
            R.id.import_db -> {
                viewModel.onImport()
                true
            }
            R.id.search_printer -> {
                viewModel.onSearchPrinter()
                true
            }
            else -> NavigationUI.onNavDestinationSelected(
                item,
                requireView().findNavController()
            ) || super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.i("fragment onActivityCreated called")
    }

    override fun onResume() {
        super.onResume()
        Timber.i("fragment onResume called")
    }

    override fun onPause() {
        super.onPause()
        Timber.i("fragment onPause called")
    }

    override fun onStop() {
        super.onStop()
        Timber.i("fragment onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("fragment onDestroy")
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        viewModel.onNukeConfirmed()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        // Do nothing.
    }
}