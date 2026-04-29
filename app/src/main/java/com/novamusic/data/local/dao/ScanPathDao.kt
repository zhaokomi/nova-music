package com.novamusic.data.local.dao

import androidx.room.*
import com.novamusic.data.local.entity.ScanPathEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanPathDao {

    @Query("SELECT * FROM scan_paths ORDER BY date_added DESC")
    fun getAllScanPaths(): Flow<List<ScanPathEntity>>

    @Query("SELECT * FROM scan_paths")
    suspend fun getAllScanPathsList(): List<ScanPathEntity>

    @Query("SELECT * FROM scan_paths WHERE path = :path LIMIT 1")
    suspend fun getScanPath(path: String): ScanPathEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScanPath(scanPath: ScanPathEntity): Long

    @Delete
    suspend fun deleteScanPath(scanPath: ScanPathEntity)

    @Query("DELETE FROM scan_paths WHERE id = :id")
    suspend fun deleteScanPathById(id: Long)

    @Query("SELECT COUNT(*) FROM scan_paths")
    suspend fun getScanPathCount(): Int
}
