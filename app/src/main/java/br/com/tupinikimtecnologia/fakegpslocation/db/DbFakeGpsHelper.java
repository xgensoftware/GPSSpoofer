package br.com.tupinikimtecnologia.fakegpslocation.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import br.com.tupinikimtecnologia.fakegpslocation.constant.DbConstantes;

/**
 * Created by felipe on 22/03/15.
 */
public class DbFakeGpsHelper extends SQLiteOpenHelper {

    private String SQL_CREATE_TABLE_HISTORICO = "create table if not exists " + DbConstantes.NOME_TABELA_HISTORICO +
            "(" +
            DbConstantes.KEY_ID_HISTORICO + " integer primary key autoincrement, " +
            DbConstantes.KEY_ENDERECO_HISTORICO + " text not null," +
            DbConstantes.KEY_COORD_X_HISTORICO + " integer not null," +
            DbConstantes.KEY_COORD_Y_HISTORICO + " integer not null," +
            DbConstantes.KEY_DATA_HISTORICO + " DATETIME DEFAULT CURRENT_DATE, " +
            DbConstantes.KEY_HORA_HISTORICO + " DATETIME DEFAULT CURRENT_TIME" +
            ");";

    public DbFakeGpsHelper(Context context) {
        super(context, DbConstantes.NOME_TABELA_HISTORICO, null, DbConstantes.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_HISTORICO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }
}
