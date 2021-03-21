package com.maple.smartcan.util;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

public final class DBController {
    private SQLiteDatabase mMemoryDb;
    private static final String LOG_TAG = "DBController";

    private static final String TABLE_NAME_ACCOUNT = "maple_smartcan_account";


    private DBController(){
        mMemoryDb = createMemoryDb();
    }

    private static final DBController sDefault = new DBController();

    public static DBController getDefault(){
        return sDefault;
    }

    public interface Columns extends BaseColumns {
        public static final String account = "account";
        public static final String password = "password";
    }

    /**
     * 创建内存数据库
     */
    private SQLiteDatabase createMemoryDb(){
        SQLiteDatabase database = SQLiteDatabase.create(null);
        String t_user_sql = "CREATE TABLE "+TABLE_NAME_ACCOUNT+"(Id integer primary key autoincrement,"+Columns.account+" varchar(11),"+Columns.password+" varchar(32))";
        database.execSQL(t_user_sql);
        return database;
    }
    /**
     * 向内存数据库中插入一条数据
     */
    public void Insert(String account,String password) {
        SQLiteDatabase db = mMemoryDb;
        check(db);
        ContentValues values = new ContentValues();
        values.put(Columns.account, account);
        values.put(Columns.password, password);

        db.insert(TABLE_NAME_ACCOUNT, null, values);

    }
    /**
     * 查询内存数据库中的数据
     */
    public void Query(){
        SQLiteDatabase db = mMemoryDb;
        check(db);
        @SuppressLint("Recycle") Cursor c = db.rawQuery("select account,password from "+TABLE_NAME_ACCOUNT, null);

        while(c.moveToNext()){
            String account = c.getString(0);
            String password = c.getString(0);
        }

    }
    @Override
    protected void finalize() throws Throwable {
        releaseMemory();
        super.finalize();
    }

    public void releaseMemory(){
        SQLiteDatabase db = mMemoryDb;
        if(db!=null){
            db.close();
            mMemoryDb = null;
        }
    }

    private void check(SQLiteDatabase db) {
        if(db==null || !db.isOpen()){
            throw new IllegalStateException("memory database already closed");
        }
    }

}

