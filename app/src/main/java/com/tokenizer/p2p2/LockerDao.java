package com.tokenizer.p2p2;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LockerDao {

    @Query("SELECT * FROM locker")
    List<Locker> loadAll();

    @Query("SELECT * FROM locker WHERE id IN (:lockerIds)")
    List<Locker> loadAllByLockerId(int... lockerIds);

    @Insert
    void insertAll(Locker... locker);

    @Delete
    void delete(Locker lockers);
}
