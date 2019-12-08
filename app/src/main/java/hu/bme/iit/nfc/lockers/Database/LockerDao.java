package hu.bme.iit.nfc.lockers.Database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import hu.bme.iit.nfc.lockers.Model.Locker;

import java.util.List;

@Dao
public interface LockerDao {

    @Query("SELECT * FROM locker")
    LiveData<List<Locker>> loadAll();

    @Query("SELECT * FROM locker WHERE id IN (:lockerIds)")
    LiveData<List<Locker>> loadAllByLockerId(int... lockerIds);

    @Insert
    void insertAll(Locker... locker);

    @Delete
    void delete(Locker lockers);
}
