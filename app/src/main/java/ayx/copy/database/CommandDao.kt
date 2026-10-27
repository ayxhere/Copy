package ayx.copy.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {

    @Query("SELECT * FROM commands ORDER BY createdAt DESC")
    fun getAllCommands(): Flow<List<Command>>

    @Query("SELECT * FROM commands WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteCommands(): Flow<List<Command>>

    @Query("SELECT * FROM commands WHERE lastUsedAt > 0 ORDER BY lastUsedAt DESC LIMIT 50")
    fun getRecentCommands(): Flow<List<Command>>

    @Query("SELECT * FROM commands WHERE command LIKE '%' || :query || '%' OR replacementText LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchCommands(query: String): Flow<List<Command>>

    @Query("SELECT * FROM commands WHERE command = :command LIMIT 1")
    suspend fun getCommandByExactName(command: String): Command?

    @Query("SELECT * FROM commands")
    suspend fun getAllCommandsSync(): List<Command>

    @Query("SELECT * FROM commands WHERE id = :id")
    suspend fun getCommandById(id: Long): Command?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: Command): Long

    @Update
    suspend fun update(command: Command)

    @Delete
    suspend fun delete(command: Command)

    @Query("DELETE FROM commands WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE commands SET useCount = useCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUseCount(id: Long, timestamp: Long)

    @Query("UPDATE commands SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM commands")
    suspend fun getCount(): Int
}
