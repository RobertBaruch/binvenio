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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import org.babbageboole.binvenio.BuildConfig
import org.babbageboole.binvenio.R
import timber.log.Timber

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.about_fragment, container, false)

        val mailtoItem = Element("robert.c.baruch@gmail.com", R.drawable.ic_baseline_email_24).apply {
            // intent = mailtoIntent
            onClickListener = View.OnClickListener { view -> onClickEmail() }
        }

        val aboutView = AboutPage(context)
            .isRTL(false)
            .setDescription("Binvenio: organize your stuff")
            // .addEmail("robert.c.baruch@gmail.com")
            .addItem(Element("Version ${BuildConfig.VERSION_NAME}", R.drawable.ic_baseline_info_24))
            .addItem(mailtoItem)
            .addGitHub("RobertBaruch/binvenio")
            .create()

        val layout = view.findViewById<LinearLayout>(R.id.about_contents)
        layout.addView(aboutView)

        return view
    }

    private fun onClickEmail() {
        Timber.i("Email clicked")
        val mailtoIntent = Intent(Intent.ACTION_SENDTO).apply {
            type = "*/*"
            data = Uri.parse("mailto:") // only email apps allowed to handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf("robert.c.baruch@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Question about Binvenio")
        }
        startActivity(mailtoIntent)
    }
}

