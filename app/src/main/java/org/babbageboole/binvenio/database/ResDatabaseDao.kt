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

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Defines methods for using the Bin class with Room.
 */
@Dao
interface ResDatabaseDao {
    @Insert
    fun insertRes(res: Res)

    @Update
    fun updateRes(res: Res)

    @Update
    fun updateAllRes(res: List<Res>)

    @Query("DELETE FROM res WHERE qr = :qr")
    fun deleteRes(qr: String)

    @Query("SELECT * FROM res WHERE qr = :qr")
    fun getRes(qr: String): Res?

    @Query("SELECT * FROM res WHERE qr = :qr AND is_container")
    fun getContainer(qr: String): Res?

    @Query("SELECT * FROM res WHERE qr = :qr AND NOT is_container")
    fun getNonContainer(qr: String): Res?

    @Transaction
    @Query("SELECT * FROM res WHERE qr = :qr")
    fun getResWithContainedRes(qr: String): ResWithContainedRes?

    @Transaction
    @Query(
        "SELECT e1.qr AS qr, e1.name AS name, e1.count AS count, e1.is_container AS is_container, e2.name AS container_name" +
                " FROM res e1" +
                " LEFT JOIN res e2 ON e2.qr = e1.loc" +
                " WHERE e1.qr = :qr"
    )
    fun getResWithContainer(qr: String): ResWithContainingRes?

    // Escape % as \% and \ as \\ in the argument for this function.
    @Query("SELECT * FROM res WHERE name LIKE '%' || :n || '%' ESCAPE '\\'")
    fun findResByPartialName(n: String): List<Res>

    // Escape % as \% and \ as \\ in the argument for this function.
    @Query(
        "SELECT e1.qr AS qr, e1.name AS name, e1.count AS count, e1.is_container AS is_container, e2.name AS container_name" +
                " FROM res e1" +
                " LEFT JOIN res e2 ON e2.qr = e1.loc" +
                " WHERE e1.name LIKE '%' || :n || '%' ESCAPE '\\'" +
                " ORDER BY name ASC"
    )
    fun findResWithContainerResByPartialName(n: String): List<ResWithContainingRes>

    // Escape % as \% and \ as \\ in the argument for this function.
    @Query(
        "SELECT e1.qr AS qr, e1.name AS name, e1.count AS count, e1.is_container AS is_container, e2.name AS container_name" +
                " FROM res e1" +
                " LEFT JOIN res e2 ON e2.qr = e1.loc" +
                " WHERE e1.name LIKE '%' || :n || '%' ESCAPE '\\'" +
                "  ORDER BY name ASC"
    )
    fun findResWithContainerResByPartialNameLive(n: String): LiveData<List<ResWithContainingRes>>

    // Escape % as \% and \ as \\ in the argument for this function.
    @Query(
        "SELECT e1.qr AS qr, e1.name AS name, e1.count AS count, e1.is_container AS is_container, e2.name AS container_name" +
                " FROM res e1" +
                " LEFT JOIN res e2 ON e2.qr = e1.loc" +
                " WHERE NOT e1.is_container AND e1.name LIKE '%' || :n || '%' ESCAPE '\\'"
    )
    fun findItemsWithContainerResByPartialNameLive(n: String): LiveData<List<ResWithContainingRes>>

    @Query("SELECT * FROM res WHERE loc = :loc ORDER BY name ASC")
    fun getContents(loc: String): List<Res>

    @Query("SELECT * FROM res WHERE loc = :loc ORDER BY name ASC")
    fun getContentsLiveView(loc: String): LiveData<List<Res>>
}