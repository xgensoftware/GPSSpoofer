package br.com.tupinikimtecnologia.fakegpslocation.dialogs;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import br.com.tupinikimtecnologia.fakegpslocation.R;
import br.com.tupinikimtecnologia.fakegpslocation.constant.DbConstantes;
import br.com.tupinikimtecnologia.fakegpslocation.db.DbFakeGpsHelper;
import br.com.tupinikimtecnologia.fakegpslocation.db.TableHistorico;

/**
 * Created by felipe on 24/03/15.
 */
public class HistoricoDialog extends Activity{

    private DbFakeGpsHelper db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_historico);

        ListView listView = (ListView) findViewById(R.id.listHistorico);
        db = new DbFakeGpsHelper(this);
        TableHistorico tHistorico = new TableHistorico(this);

        Cursor cursor = tHistorico.getHistorico();
        CursorAdapter cursorAdapter = new CursorAdapter(this, cursor, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
                TextView semRegistrosTextView = (TextView) view.findViewById(R.id.semRegistroTextView);

                if(cursor==null || cursor.getCount()<=0){
                    semRegistrosTextView.setVisibility(View.VISIBLE);
                }
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView enderecoTextView = (TextView) view.findViewById(android.R.id.text1);
                TextView latlongTextView = (TextView) view.findViewById(android.R.id.text2);

                String textLatLong = "Lat: "+cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_COORD_X_HISTORICO)) + " | "+
                        "Long: "+cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_COORD_Y_HISTORICO));

                enderecoTextView.setText(cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_ENDERECO_HISTORICO)));
                latlongTextView.setText(textLatLong);
            }
        };

        listView.setAdapter(cursorAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}
