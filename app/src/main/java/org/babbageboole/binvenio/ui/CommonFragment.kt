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

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import timber.log.Timber

// CommonFragment is a fragment that can do snackbar messages and errors, and can scan.
// It can also be set to observe its ViewModel's Boolean or String observables for navigation cues.
abstract class CommonFragment<VM : CommonViewModel, DB : ViewDataBinding> : Fragment(),
    SearchPrinterDialogFragment.SearchPrinterDialogListener,
    WarnAboutDHCPDialogFragment.DialogListener {

    protected lateinit var binding: DB
    protected lateinit var viewModel: VM

    fun addStandardObservers() {
        viewModel.showError.observe(viewLifecycleOwner, Observer {
            it?.let { doSnackbar(it, true) }
        })
        viewModel.showMsg.observe(viewLifecycleOwner, Observer {
            it?.let { doSnackbar(it, false) }
        })
        viewModel.bringUpScanner.observe(viewLifecycleOwner, Observer {
            if (it) {
                bringUpScanner()
                viewModel.scannerBroughtUp()
            }
        })
        viewModel.printComplete.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.onPrintComplete()
            }
        })
        viewModel.showPrintSearch.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.showPrintSearchShown()
                val dialog = SearchPrinterDialogFragment()
                // childFragmentManager is for showing fragments inside this fragment.
                // Plain old fragmentManager/supportFragmentManager is for fragments inside
                // the activity. We use the child fragment manager so that this fragment
                // gets the dialog callbacks.
                dialog.show(childFragmentManager, "print_search")
            }
        })
        viewModel.showDHCPWarning.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.dhcpWarningShown()
                val dialog = WarnAboutDHCPDialogFragment()
                dialog.show(childFragmentManager, "dhcp_warning")
            }
        })
    }

    fun addNavigationObserver(observable: LiveData<Boolean>, where: NavDirections) {
        observable.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                findNavController().navigate(where)
                viewModel.onNavigationComplete()
            }
        })
    }

    fun addNavigationStringObserver(
        observable: LiveData<String?>,
        where: (String) -> NavDirections
    ) {
        observable.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(where(it))
                viewModel.onNavigationComplete()
            }
        })
    }

    private fun bringUpScanner() {
        Timber.i("fragment bringing up scanner")
        // TODO: hack so that it conforms to new registration for activity result and activity contract.
        IntentIntegrator.forSupportFragment(this).initiateScan()
    }

    // Called after scanner returns.
    // TODO: hack so that it conforms to new registration for activity result and activity contract.
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode != IntentIntegrator.REQUEST_CODE) {
            return
        }

        if (resultCode == Activity.RESULT_CANCELED) {
            Timber.i("fragment onActivityResult: cancelled")
            return
        }

        Timber.i("onActivityResult, requestCode $requestCode, resultCode $resultCode, data $resultData")
        var result: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, resultData)
        if (result == null) {
            super.onActivityResult(requestCode, resultCode, resultData)
            Timber.i("fragment onActivityResult: no result")
            return
        }
        val qr = result.contents
        var format = result.formatName
        Timber.i("fragment onActivityResult: $result")
        Timber.i("fragment onActivityResult: contents = ${result.contents}")
        viewModel.onScanned(format, qr)
    }

    private fun doSnackbar(msg: String, isErr: Boolean) {
        val s = Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            msg,
            Snackbar.LENGTH_SHORT // How long to display the message.
        )
        if (isErr) s.setBackgroundTint(Color.RED)
        else s.setBackgroundTint(Color.GREEN)
        s.show()
        if (isErr) {
            viewModel.onErrorShown()
        } else {
            viewModel.onMsgShown()
        }
    }

    override fun onPrinterFound(dialog: DialogFragment) {
        viewModel.onPrinterFound()
    }

    override fun onCancelSearchPrinter(dialog: DialogFragment) {
        viewModel.onCancelSearchPrinter()
    }

    override fun onPrinterHasDHCP(dialog: DialogFragment) {
        viewModel.onPrinterHasDHCP()
    }

    override fun onDHCPAcknowledged(dialog: DialogFragment) {
        viewModel.onPrinterFound()
    }
}