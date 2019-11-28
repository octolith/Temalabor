package com.tokenizer.p2p2;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "locker")
public class Locker implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name="token_string")
    private String tokenString;
    @ColumnInfo(name="number")
    private String number;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTokenString() {
        return tokenString;
    }

    public void setTokenString(String tokenString) {
        this.tokenString = tokenString;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Locker(String tokenString, String number) {
        this.tokenString = tokenString;
        this.number = number;
    }

    /* everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeString(tokenString);
        out.writeString(number);
    }

    // this is used to regenerate your object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Locker> CREATOR = new Parcelable.Creator<Locker>() {
        public Locker createFromParcel(Parcel in) {
            return new Locker(in);
        }

        public Locker[] newArray(int size) {
            return new Locker[size];
        }
    };

    // example constructor that takes a Parcel and gives you an object populated with it's values
    private Locker(Parcel in) {
        id = in.readInt();
        tokenString = in.readString();
        number = in.readString();
    }
}
