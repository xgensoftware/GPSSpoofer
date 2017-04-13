package br.com.tupinikimtecnologia.fakegpslocation.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import br.com.tupinikimtecnologia.fakegpslocation.constant.DbConstantes;

/**
 * Created by felipe on 23/03/15.
 */
public class TableHistorico {

    private SQLiteDatabase db;

    public TableHistorico(Context context){
        DbFakeGpsHelper dbFakeGpsHelper = new DbFakeGpsHelper(context);
        db = dbFakeGpsHelper.getWritableDatabase();
    }

    public long insertDados(String endereco, double coordx, double coordy){
        ContentValues campos = new ContentValues();
        campos.put(DbConstantes.KEY_ENDERECO_HISTORICO, endereco);
        campos.put(DbConstantes.KEY_COORD_X_HISTORICO, coordx);
        campos.put(DbConstantes.KEY_COORD_Y_HISTORICO, coordy);
        return db.insert(DbConstantes.NOME_TABELA_HISTORICO, null, campos);
    }

    public Cursor getHistorico(){
        return db.query(DbConstantes.NOME_TABELA_HISTORICO, new String[]{ "rowid _id", DbConstantes.KEY_ENDERECO_HISTORICO,DbConstantes.KEY_COORD_X_HISTORICO, DbConstantes.KEY_COORD_Y_HISTORICO, DbConstantes.KEY_DATA_HISTORICO, DbConstantes.KEY_HORA_HISTORICO }, null, null, null, null, DbConstantes.KEY_ID_HISTORICO+ " DESC");
    }

    public void deleteHistoricosAll(){
        db.delete(DbConstantes.NOME_TABELA_HISTORICO, null, null);
    }

    public void deleteHistorico(long id){
        String[] args = {Long.toString(id)};
        db.delete(DbConstantes.NOME_TABELA_HISTORICO, DbConstantes.KEY_ID_HISTORICO+"=?", args);
    }
}
