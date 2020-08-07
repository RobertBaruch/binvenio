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

package org.babbageboole.binvenio.database

import androidx.room.*

/**
 * A Res, Latin for "thing", (the Latin plural is also "res", but we will use "reses") is something
 * that may or may not contain other Reses. An example of a Res is a battery, which is a
 * non-container res because it will not contain anything. An example of a container Res is a bin.
 * A shelf can be a container Res. A room is also a container Res.
 *
 * The QR codes for Reses are always unique.
 *
 * A non-container Res can have a count. This is just the number of those res that are together
 * in the same place. For example, a bag of hairpins would be a non-container Res with a count,
 * because you wouldn't put a QR code on each individual hairpin.
 *
 * Of course, you could put the same QR code on multiple non-container Reses. For example, a box
 * with a USB stick in it might be a non-container Res, and you might have many of those in a
 * container.
 */
@Entity(tableName = "res")
data class Res(
    @PrimaryKey
    var qr: String,
    var name: String,
    var count: Long = 0,

    @ColumnInfo(name = "is_container")
    var isContainer: Boolean = false,

    @ColumnInfo(name = "loc")
    var locationQR: String? = null
)

data class ResWithContainedRes(
    @Embedded val res: Res,
    @Relation(
        parentColumn = "qr",
        entityColumn = "loc"
    )
    val containedRes: List<Res>
)

// This is to represent the result of a join between a res and its containing res.
data class ResWithContainingRes(
    var qr: String,
    var name: String,
    var count: Long = 0,
    @ColumnInfo(name = "is_container")
    var isContainer: Boolean = false,
    @ColumnInfo(name = "container_name")
    var containerName: String? = null
)