package com.vinsol.roadrunner;



        import android.content.ContentValues;
        import android.content.Context;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.database.sqlite.SQLiteOpenHelper;

public class DB_Helper extends SQLiteOpenHelper {

    public static final String TOKEN_TABLE="token_table";
    public static final String CREATE_TOKEN_TABLE= "create table if not exists "+TOKEN_TABLE+"(token  text)";
    public static final String DROP_TOKEN_TABLE="DROP TABLE IF EXISTS"+TOKEN_TABLE;
    public static final String TOKEN_DELETE_CONTENT="DELETE FROM "+TOKEN_TABLE;

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TOKEN_TABLE);
    }

    public DB_Helper(Context context)
    {
        super(context, "ROADRUNNER_DATABASE", null, 1);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DROP_TOKEN_TABLE);
        onCreate(sqLiteDatabase);
    }

    public boolean insertToken (String token)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("token", token);
        db.insert(TOKEN_TABLE, null, contentValues);
        return true;
    }

    public void deleteContent (){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(TOKEN_DELETE_CONTENT);
    }
    public int getCount(){
        SQLiteDatabase db = this.getWritableDatabase();
        int count;
        Cursor c=db.rawQuery( "select * from "+TOKEN_TABLE,null);
        c.moveToFirst();
        count=c.getCount();
        return count;
    }
    public String getToken(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c=db.rawQuery( "select token from "+TOKEN_TABLE,null);
        c.moveToFirst();
        return c.getString(0);
    }
}
