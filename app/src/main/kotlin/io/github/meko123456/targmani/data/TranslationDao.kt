package io.github.meko123456.targmani.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    @Query("SELECT * FROM translations ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translations WHERE favourite = 1 ORDER BY timestampMillis DESC")
    fun observeFavourites(): Flow<List<TranslationRecord>>

    /** The newest entry — what the history policy compares a new translation against. */
    @Query("SELECT * FROM translations ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun head(): TranslationRecord?

    @Query("SELECT * FROM translations")
    suspend fun all(): List<TranslationRecord>

    @Insert
    suspend fun insert(record: TranslationRecord): Long

    @Update
    suspend fun update(record: TranslationRecord)

    @Query("UPDATE translations SET favourite = :favourite WHERE id = :id")
    suspend fun setFavourite(id: Long, favourite: Boolean)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Clear history but keep what the user starred. */
    @Query("DELETE FROM translations WHERE favourite = 0")
    suspend fun clearUnfavourited()
}
