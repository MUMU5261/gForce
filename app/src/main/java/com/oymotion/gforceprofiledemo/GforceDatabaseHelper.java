package com.oymotion.gforceprofiledemo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class GforceDatabaseHelper extends SQLiteOpenHelper {

    public static final String Create_Quaternion="create table Quaternion ("
            + "id integer primary key autoincrement,"
            + "user_id integer,"
            + "timestamp text,"
            + "section text,"
            + "w float,"
            + "x float,"
            + "y float,"
            + "z float)";

    private Context mContext;
    public GforceDatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Create_Quaternion);
        Toast.makeText(mContext,"create succeed", Toast.LENGTH_SHORT).show();
        System.out.println("database create successful");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
