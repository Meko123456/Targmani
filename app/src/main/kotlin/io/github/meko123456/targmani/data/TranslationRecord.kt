package io.github.meko123456.targmani.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recorded translation. Languages are stored as BCP-47 codes rather than enum ordinals so a
 * future language can be added without a migration invalidating existing rows.
 */
@Entity(tableName = "translations")
data class TranslationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val fromCode: String,
    val toCode: String,
    val timestampMillis: Long,
    val favourite: Boolean = false,
)
