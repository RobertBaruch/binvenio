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

package org.babbageboole.binvenio.ui.bin_scanned

import android.widget.TextView
import androidx.databinding.BindingAdapter
import org.babbageboole.binvenio.R
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResWithContainingRes

@BindingAdapter("resCountFormatted")
fun TextView.setResCountFormatted(res: Res?) {
    res?.let {
        text = when (res.isContainer) {
            false -> context.resources.getString(R.string.res_count_format, res.count)
            true -> ""
        }
    }
}

@BindingAdapter("resCountFormatted")
fun TextView.setResCountFormatted(res: ResWithContainingRes?) {
    res?.let {
        text = when (res.isContainer) {
            false -> context.resources.getString(R.string.res_count_format, res.count)
            true -> ""
        }
    }
}

@BindingAdapter("resNameFormatted")
fun TextView.setResNameFormatted(res: Res?) {
    res?.let {
        text = context.resources.getString(R.string.res_name_format, res.name)
    }
}

@BindingAdapter("resNameFormatted")
fun TextView.setResNameFormatted(res: ResWithContainingRes?) {
    res?.let {
        text = context.resources.getString(R.string.res_name_format, res.name)
    }
}

@BindingAdapter("resLocFormatted")
fun TextView.setResLocFormatted(res: Res?) {
    res?.let {
        text = res.locationQR ?: ""
    }
}


@BindingAdapter("resContainerNameFormatted")
fun TextView.setResContainerNameFormatted(res: ResWithContainingRes?) {
    res?.let {
        text = res.containerName ?: ""
    }
}
