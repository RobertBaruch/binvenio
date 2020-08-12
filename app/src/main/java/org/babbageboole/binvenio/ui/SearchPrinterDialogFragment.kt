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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import org.babbageboole.binvenio.BinvenioApplication
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.databinding.MainFragmentBinding
import org.babbageboole.binvenio.databinding.PrintDialogFragmentBinding
import org.babbageboole.binvenio.printer.PrinterFactory
import org.babbageboole.binvenio.ui.add_item.AddItemViewModel
import timber.log.Timber
import java.net.InetSocketAddress
import javax.inject.Inject

class SearchPrinterDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    internal lateinit var listener: SearchPrinterDialogListener
    private lateinit var viewModel: SearchPrinterViewModel

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface SearchPrinterDialogListener {
        fun onPrinterFound(dialog: DialogFragment)
        fun onCancelSearchPrinter(dialog: DialogFragment)
        fun onPrinterHasDHCP(dialog: DialogFragment)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NukeDialogListener so we can send events to the host.
            // Important: you want to use parentFragment, not context, because context is
            // the main activity. Also, when showing the dialog, pass in the fragment's
            // childFragmentManager, not the activity's supportFragmentManager.
            listener = parentFragment as SearchPrinterDialogListener
        } catch (e: ClassCastException) {
            // The fragment doesn't implement the interface, throw exception
            throw ClassCastException("$context must implement SearchPrinterDialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val binding: PrintDialogFragmentBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context), R.layout.print_dialog_fragment, null, false
            )
            val application = requireActivity().application
            val viewModelFactory = ViewModelFactory(application)
            viewModel = ViewModelProvider(this, viewModelFactory).get(SearchPrinterViewModel::class.java)
            binding.viewModel = viewModel
            binding.lifecycleOwner = this

            val builder = AlertDialog.Builder(it)
            builder
                .setView(binding.root)
                .setTitle("Searching for printer")
                .setNegativeButton(R.string.cancel) { dialog, id ->
                    listener.onCancelSearchPrinter(this)
                }

            viewModel.success.observe(this, Observer {
                it?.let {
                    dialog?.dismiss()
                    when (it) {
                        SearchPrinterViewModel.OK -> listener.onPrinterFound(this)
                        SearchPrinterViewModel.NO_PRINTER -> listener.onCancelSearchPrinter(this)
                        SearchPrinterViewModel.NO_NETWORK -> listener.onCancelSearchPrinter(this)
                        SearchPrinterViewModel.HAS_DHCP -> listener.onPrinterHasDHCP(this)
                    }
                }
            })

            viewModel.onSearch()

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
