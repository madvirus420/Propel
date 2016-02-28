package com.aiu.propel.dao;

/**
 * Created by Rohan on 10/16/2015.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.aiu.propel.util.RpeVO;
import com.aiu.propel.util.ServiceUtilities;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteHandlerImpl implements SQLiteHandler {

    public static final String KEY_ROWID = "report_id";


    private static final String DATABASE_NAME = "trailDB";

    private static final String LOG_TABLE_NAME = "logs";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_RPE = "rpe";
    private static final String KEY_UPDATE_TIME = "last_updated";

    private static final int DATABASE_VERSION = 1;
    private final Context ourContext;
    private DBHelper ourHelper;
    private SQLiteDatabase myDB;
    private ServiceUtilities utilities = new ServiceUtilities();

    public SQLiteHandlerImpl(Context c) {
        ourContext = c;
    }

    public long insertSessions(Long rpe){
        ContentValues cv = new ContentValues();
//        cv.put(KEY_DURATION, duration.toString());
        cv.put(KEY_RPE, Long.toString(rpe));
        cv.put(KEY_UPDATE_TIME, Long.toString(System.currentTimeMillis()));
        return myDB.insert( LOG_TABLE_NAME,null, cv);
    }

    public List<RpeVO> getPastRecord(){
        List<RpeVO> rpeVO = new ArrayList<>();
        Cursor c;
        c = myDB.rawQuery( "SELECT * FROM " + LOG_TABLE_NAME + " ORDER BY " + KEY_UPDATE_TIME + " DESC LIMIT 4", null );

 //       int duration_id = c.getColumnIndex( KEY_DURATION );
        int rpe_id = c.getColumnIndex( KEY_RPE );

        for ( c.moveToFirst(); !c.isAfterLast(); c.moveToNext() ) {

            RpeVO temp = new RpeVO();

            //temp.setDuration(c.getString( duration_id ));
            temp.setRpe(c.getString(rpe_id));

            rpeVO.add(temp);
        }
        return rpeVO;
    }


    public SQLiteHandlerImpl open() throws SQLException {
        ourHelper = new DBHelper( ourContext );
        myDB = ourHelper.getWritableDatabase();
        return this;
    }

    public void close() throws SQLException {
        ourHelper.close();
    }

    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper( Context context ) {
            super( context, DATABASE_NAME, null, DATABASE_VERSION );
        }

        @Override
        public void onCreate( SQLiteDatabase db ) {

            db.execSQL( "CREATE TABLE " + LOG_TABLE_NAME + " ("
                            + KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + KEY_RPE + " TEXT NOT NULL, "
                            + KEY_UPDATE_TIME + " DATETIME NOT NULL );"
            );
        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
            db.execSQL( "DROP TABLE IF EXISTS " + LOG_TABLE_NAME );
            onCreate( db );
        }
    }
}
