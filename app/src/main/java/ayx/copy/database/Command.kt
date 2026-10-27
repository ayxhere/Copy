package ayx.copy.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commands")
data class Command(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val command: String,        // e.g. ".upi"
    val replacementText: String, // e.g. "UPI ID : example@upi"

    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val lastUsedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
