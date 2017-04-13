package br.com.tupinikimtecnologia.fakegpslocation.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;

import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import br.com.tupinikimtecnologia.fakegpslocation.MainActivity;
import br.com.tupinikimtecnologia.fakegpslocation.R;
import br.com.tupinikimtecnologia.fakegpslocation.constant.GeralConstantes;

/**
 * Created by felipe on 07/03/15.
 */
public class FakeService extends Service {

    private Handler handler;
    private Runnable runnable;
    public static boolean running = false;
    public static boolean passL = false;
    private LocationManager locationManager;
    private double latitude, longitude;
    private Intent it;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    private SharedPreferences prefService;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        running = true;
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //check the last fake location
        prefService = this.getSharedPreferences(GeralConstantes.PREFS_SERVICE_NAME, Context.MODE_PRIVATE);
        //get the last latitude and longitude
        latitude = Double.longBitsToDouble(prefService.getLong(GeralConstantes.PREFS_SERVICE_LAT_TAG, 0));
        longitude = Double.longBitsToDouble(prefService.getLong(GeralConstantes.PREFS_SERVICE_LONG_TAG, 0));

        //build the Notification
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setAutoCancel(false);
        mBuilder.setOngoing(true);
        mBuilder.setContentTitle(getString(R.string.notification_service_run_title));
        mBuilder.setContentText(getString(R.string.notification_service_run_text));

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        //set the fake location
        runnable = new Runnable() {
            @Override
            public void run() {
                if(running) {
                    try {
                        setLocation(latitude, longitude);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    //Log.d("SERVICE", ""+startId);
                    //Log.d("INFOS-GET-EXTRA", "Latitude: "+latitude+" | Longitude: "+longitude);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        runnable.run();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            //When the service are destroyed, stop the faking gps
            stopFakeTh();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    //stop faking method
    public void stopFakeTh() throws Exception{
        handler.removeCallbacksAndMessages(runnable);
        stopForeground(true);
        //stop the test provider
        locationManager.setTestProviderEnabled(GeralConstantes.FAKE_PROVIDER_NAME, false);
        //remove the test provider
        locationManager.removeTestProvider(GeralConstantes.FAKE_PROVIDER_NAME);
        //set that faking are not running
        running = false;
        //close notification
        mNotificationManager.cancel(0);
        stopSelf();

    }

    //set fake location method
    public void setLocation(double latitude, double longitude) throws Exception{
        locationManager.addTestProvider(GeralConstantes.FAKE_PROVIDER_NAME, false, false, false, false, false, false, false, 1, 1);
        locationManager.setTestProviderEnabled(GeralConstantes.FAKE_PROVIDER_NAME,true);
        Location location = new Location(GeralConstantes.FAKE_PROVIDER_NAME);
        if (android.os.Build.VERSION.SDK_INT >= 17)
        {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(16f);
        location.setAltitude(0.0D);
        location.setBearing(0.0F);
        location.setTime(System.currentTimeMillis());
        locationManager.setTestProviderLocation(GeralConstantes.FAKE_PROVIDER_NAME, location);
    }
}
