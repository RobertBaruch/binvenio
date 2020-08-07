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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.databinding.ListItemResBinding
import timber.log.Timber

class ContentsAdapter : ListAdapter<Res, ContentsAdapter.ViewHolder>(ContentsDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Timber.i("Bind a view holder in position $position")
        var item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.from(parent)

    class ViewHolder private constructor(val binding: ListItemResBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(res: Res) {
            binding.res = res
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                Timber.i("Inflate a view holder")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemResBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class ContentsDiffCallback : DiffUtil.ItemCallback<Res>() {
    override fun areItemsTheSame(oldItem: Res, newItem: Res): Boolean {
        Timber.i("areItemsTheSame, old ${oldItem.qr}, new ${newItem.qr}")
        return oldItem.qr == newItem.qr
    }

    override fun areContentsTheSame(oldItem: Res, newItem: Res): Boolean {
        Timber.i("areContentsTheSame, old ${oldItem.qr}, new ${newItem.qr}")
        return oldItem == newItem
    }
}
