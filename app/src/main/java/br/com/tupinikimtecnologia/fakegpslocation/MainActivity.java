package br.com.tupinikimtecnologia.fakegpslocation;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import br.com.tupinikimtecnologia.fakegpslocation.constant.DbConstantes;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import br.com.tupinikimtecnologia.fakegpslocation.constant.GeralConstantes;
import br.com.tupinikimtecnologia.fakegpslocation.db.DbFakeGpsHelper;
import br.com.tupinikimtecnologia.fakegpslocation.db.TableHistorico;
import br.com.tupinikimtecnologia.fakegpslocation.services.FakeService;

public class MainActivity extends AppCompatActivity implements GoogleMap.OnMapClickListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker marker;
    private Intent itService;
    private Menu menu;
    private LatLng latLng;
    private SharedPreferences prefService;
    private DbFakeGpsHelper dbFakeGpsHelper;
    private TableHistorico tHistorico;
    private CursorAdapter cursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((UILApplication) getApplication()).getTracker(UILApplication.TrackerName.APP_TRACKER);

        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();


        if(activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        itService = new Intent(this, FakeService.class);

        if (setUpMapIfNeeded()) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            mMap.setOnMapClickListener(this);
        }

        prefService = this.getSharedPreferences(GeralConstantes.PREFS_SERVICE_NAME, Context.MODE_PRIVATE);
        latLng = new LatLng(
                Double.longBitsToDouble(prefService.getLong(GeralConstantes.PREFS_SERVICE_LAT_TAG, 0)),
                Double.longBitsToDouble(prefService.getLong(GeralConstantes.PREFS_SERVICE_LONG_TAG, 0))
        );
        int gPlayServiceStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        if(gPlayServiceStatus== ConnectionResult.SUCCESS) {
            addMark(latLng);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        //MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.historico_context_menu, menu);

        MenuItem menuItemRemover = menu.add(0, 0, 0, R.string.remover);

        final AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo)menuItemRemover.getMenuInfo();

        menuItemRemover.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                dbFakeGpsHelper = new DbFakeGpsHelper(MainActivity.this);
                tHistorico = new TableHistorico(MainActivity.this);
                tHistorico.deleteHistorico(info.id);
                dbFakeGpsHelper.close();
                Cursor cursor = tHistorico.getHistorico();
                cursorAdapter.swapCursor(cursor);
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        if(FakeService.running){
            menu.findItem(R.id.menuStart).setVisible(false);
            menu.findItem(R.id.menuPause).setVisible(true);
        }else{
            menu.findItem(R.id.menuStart).setVisible(true);
            menu.findItem(R.id.menuPause).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.menuStart:
                if(isMockSettingsON()) {
                    if (marker != null) {

                        SharedPreferences.Editor editor = prefService.edit();
                        editor.putLong(GeralConstantes.PREFS_SERVICE_LAT_TAG, Double.doubleToLongBits(latLng.latitude));
                        editor.putLong(GeralConstantes.PREFS_SERVICE_LONG_TAG, Double.doubleToLongBits(latLng.longitude));
                        editor.commit();

                        List<Address> listSearch = searchLocation(latLng.latitude, latLng.longitude, 1);
                        if(listSearch!=null && !listSearch.isEmpty()) {
                            addHistorico(listSearch);
                        }

                        startService(itService);
                        menu.findItem(R.id.menuStart).setVisible(false);
                        menu.findItem(R.id.menuPause).setVisible(true);

                        Toast.makeText(this, getString(R.string.toast_service_start), Toast.LENGTH_SHORT).show();

                        finish();
                    }
                }else{
                    openDevSettings();
                }
                return true;
            case R.id.menuPause:
                stopService(itService);
                menu.findItem(R.id.menuStart).setVisible(true);
                menu.findItem(R.id.menuPause).setVisible(false);
                Toast.makeText(this,getString(R.string.toast_service_pause), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menuSearch:

                final Dialog searchDialog = new Dialog(this);
                searchDialog.setContentView(R.layout.dialog_search);
                searchDialog.setTitle(getString(R.string.search));

                final EditText searchEdit = (EditText) searchDialog.findViewById(R.id.searchEdit);
                ImageButton searchButton = (ImageButton) searchDialog.findViewById(R.id.searchButton);

                searchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(searchEdit.getText()!=null && !searchEdit.getText().toString().equals("")) {
                            try {
                                List<Address> listSearch = searchLocationName(searchEdit.getText().toString(), 1);
                                if(listSearch!=null && !listSearch.isEmpty()) {
                                    latLng = new LatLng(listSearch.get(0).getLatitude(), listSearch.get(0).getLongitude());
                                    updateCameraZoom(latLng);
                                    searchDialog.dismiss();
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setTitle(getString(R.string.dialog_local_confirm_title))
                                            .setMessage(getString(R.string.dialog_local_confirm_msg))
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    addMark(latLng);
                                                }
                                            }).show();
                                    //Log.d("SEARCH", "Lista: " + listSearch);
                                }else{
                                    Toast.makeText(getBaseContext(), R.string.toast_address_notfound, Toast.LENGTH_SHORT).show();
                                }
                            }catch (Exception e){
                                Toast.makeText(getBaseContext(), R.string.toast_address_notfound, Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            searchEdit.setError(getString(R.string.toast_inform_search_error));
                        }
                    }
                });

                searchDialog.show();

                return true;
            case R.id.menuSobre:
                showSobre();
                return true;
            case R.id.menuHistorico:
                abrirHistoricoDialog();
                return true;
        }

        return false;
    }

    private void abrirHistoricoDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(R.string.historico);
        dialog.setContentView(R.layout.dialog_historico);

        ListView listView = (ListView) dialog.findViewById(R.id.listHistorico);
        TextView semRegistroTextView = (TextView) dialog.findViewById(R.id.semRegistroTextView);

        DbFakeGpsHelper db = new DbFakeGpsHelper(this);
        TableHistorico tHistorico = new TableHistorico(this);

        Cursor cursor = tHistorico.getHistorico();

        if(cursor==null || cursor.getCount()<=0){
            semRegistroTextView.setVisibility(View.VISIBLE);
        }else {
            cursorAdapter = new CursorAdapter(this, cursor, 0) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
                }

                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    TextView enderecoTextView = (TextView) view.findViewById(android.R.id.text1);
                    TextView latlongTextView = (TextView) view.findViewById(android.R.id.text2);

                    String textLatLong = "Lat: " + cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_COORD_X_HISTORICO)) + " | " +
                            "Long: " + cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_COORD_Y_HISTORICO));

                    enderecoTextView.setText(cursor.getString(cursor.getColumnIndex(DbConstantes.KEY_ENDERECO_HISTORICO)));
                    latlongTextView.setText(textLatLong);
                }
            };

            listView.setAdapter(cursorAdapter);

            registerForContextMenu(listView);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(menu.findItem(R.id.menuStart).isVisible()) {
                        TextView latlongTextView = (TextView) view.findViewById(android.R.id.text2);
                        String[] latLongText = latlongTextView.getText().toString().split(" ");

                        latLng = new LatLng(Double.parseDouble(latLongText[1]), Double.parseDouble(latLongText[4]));
                        updateCameraZoom(latLng);
                        addMark(latLng);
                    }else{
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle(getString(R.string.alert_servico_rodando_title))
                            .setMessage(getString(R.string.alert_servico_rodando))
                            .setPositiveButton(getString(R.string.ok), null).show();
                    }
                    dialog.dismiss();
                }
            });

        }

        dialog.show();
    }

    private void addHistorico(List<Address> a){
        dbFakeGpsHelper = new DbFakeGpsHelper(this);
        tHistorico = new TableHistorico(this);
        //03-24 03:24:12.264    1619-1619/br.com.tupinikimtecnologia.fakegpslocation D/DB﹕ [Address[addressLines=[0:"União do Sul - State of Mato Grosso",1:"78543-000",2:"Brazil"],feature=78543-000,admin=State of Mato Grosso,sub-admin=União do Sul,locality=União do Sul,thoroughfare=null,postalCode=78543-000,countryCode=BR,countryName=Brazil,hasLatitude=true,latitude=-11.6128802,hasLongitude=true,longitude=-54.3467152,phone=null,url=null,extras=null]]
        //03-24 03:27:59.804    1619-1619/br.com.tupinikimtecnologia.fakegpslocation D/DB﹕ [Address[addressLines=[0:"R. Nove, 650 - Setor Oeste",1:"Goiânia - State of Goiás",2:"74110-100",3:"Brazil"],feature=650,admin=State of Goiás,sub-admin=Goiânia,locality=null,thoroughfare=R. Nove,postalCode=74110-100,countryCode=BR,countryName=Brazil,hasLatitude=true,latitude=-16.6845174,hasLongitude=true,longitude=-49.2678488,phone=null,url=null,extras=null]]

        String endereco = a.get(0).getAddressLine(0)+", "+a.get(0).getAddressLine(2)+", "+a.get(0).getAddressLine(1)+", "+a.get(0).getAddressLine(3);
        endereco = endereco.replace("null", "unknown");
        tHistorico.insertDados(endereco, a.get(0).getLatitude(), a.get(0).getLongitude());
        dbFakeGpsHelper.close();
    }

    private void showSobre(){
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            Calendar calendar = Calendar.getInstance();
            int ano = calendar.get(Calendar.YEAR);

            if(versionName!=null) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setMessage(String.format(getString(R.string.menu_sobre), getString(R.string.app_name), versionName, ano))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(R.string.ok, null).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void openDevSettings(){
        int dMessage;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dMessage = R.string.alert_mocksetting_and6_msg;
        }else{
            dMessage = R.string.alert_mocksetting_msg;
        }
        new AlertDialog.Builder(this)
            .setTitle(R.string.warning)
            .setMessage(dMessage)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        Intent intent = new Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS");
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                        startActivity(intent);
                    }

                }
            }).show();
    }

    private boolean isMockSettingsON() {
        boolean isMockLocation = false;
        try{
            //if marshmallow
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                AppOpsManager opsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID)== AppOpsManager.MODE_ALLOWED);
            }
            else{
                // in marshmallow this will always return true
                isMockLocation = !android.provider.Settings.Secure.getString(getContentResolver(), "mock_location").equals("0");
            }
        }
        catch (Exception e){
            return isMockLocation;
        }
        return isMockLocation;
    }

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            if(mMap != null){
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.setIndoorEnabled(true);
                mMap.setTrafficEnabled(true);
            }
            return mMap != null;
        }
        return false;
    }

    private List searchLocationName(String endereco, int max){
        Geocoder gc = new Geocoder(this, Locale.getDefault());
        try {
            return gc.getFromLocationName(endereco,max);
        } catch (IOException e) {
            return null;
        }
    }

    private List searchLocation(double latitude, double longitude, int max){
        Geocoder gc = new Geocoder(this, Locale.getDefault());
        try {
            return gc.getFromLocation(latitude, longitude, max);
        } catch (IOException e) {
            return null;
        }
    }

    private void addMark(LatLng latLng) {
        if(marker!=null) {
            marker.remove();
        }
        this.latLng = latLng;
        marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Lat: "+latLng.latitude+" | "+"Long: "+latLng.longitude));
    }

    private void updateCamera(LatLng latLng){
        CameraUpdate update = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(update);
    }

    private void updateCameraZoom(LatLng latLng){
        CameraPosition position = new CameraPosition.Builder()
        .target(latLng)
                .zoom(19)
                .tilt(13f)
                .build();
        CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
        mMap.animateCamera(update);
    }

    @Override
    public void onMapClick(LatLng latLng) {

        if(menu.findItem(R.id.menuStart).isVisible()) {
            addMark(latLng);
            updateCamera(latLng);
        }else{
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.alert_servico_rodando_title))
                .setMessage(getString(R.string.alert_servico_rodando))
                .setPositiveButton(getString(R.string.ok), null).show();
        }

    }


}
