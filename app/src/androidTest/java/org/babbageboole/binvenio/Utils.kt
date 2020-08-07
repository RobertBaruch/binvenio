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

package org.babbageboole.binvenio

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import timber.log.Timber

// See: https://stackoverflow.com/questions/31394569/how-to-assert-inside-a-recyclerview-in-espresso
fun atPosition(position: Int, viewMatcher: Matcher<View>): BoundedMatcher<View, RecyclerView> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        public override fun matchesSafely(view: RecyclerView?): Boolean {
            val viewHolder = view?.findViewHolderForAdapterPosition(position) ?: return false
            return viewMatcher.matches(viewHolder.itemView)
        }

        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            viewMatcher.describeTo(description)
        }
    }
}