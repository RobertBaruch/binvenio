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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.babbageboole.binvenio.database.Res
import org.babbageboole.binvenio.database.ResWithContainingRes
import org.babbageboole.binvenio.databinding.ListFoundResBinding

class ContentsAdapter(private val listener: (ResWithContainingRes) -> Unit ) :
    ListAdapter<ResWithContainingRes, ContentsAdapter.ViewHolder>(ContentsDiffCallback()) {
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { listener(item) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder.from(parent)

    class ViewHolder private constructor(val binding: ListFoundResBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(res: ResWithContainingRes) {
            binding.res = res
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListFoundResBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class ContentsDiffCallback : DiffUtil.ItemCallback<ResWithContainingRes>() {
    override fun areItemsTheSame(
        oldItem: ResWithContainingRes,
        newItem: ResWithContainingRes
    ): Boolean {
        return oldItem.qr == newItem.qr
    }

    override fun areContentsTheSame(
        oldItem: ResWithContainingRes,
        newItem: ResWithContainingRes
    ): Boolean {
        return oldItem == newItem
    }
}
