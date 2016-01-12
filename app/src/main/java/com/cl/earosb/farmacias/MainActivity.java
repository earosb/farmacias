package com.cl.earosb.farmacias;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import com.cl.earosb.farmacias.constants.OpenStreetMapConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OpenStreetMapConstants {

    private View appView;
    private SharedPreferences mPrefs;

    private MapView mMapView;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ResourceProxy mResourceProxy;
    private SwipeRefreshLayout swipeContainer;

    private List<OverlayItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appView = findViewById(R.id.app_view);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Context context = getApplicationContext();
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setEnabled(false);

        mResourceProxy = new ResourceProxyImpl(context);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setMultiTouchControls(true);

        MinimapOverlay mMinimapOverlay = new MinimapOverlay(context, mMapView.getTileRequestCompleteHandler());
        mMinimapOverlay.setWidth(displayMetrics.widthPixels / 5);
        mMinimapOverlay.setHeight(displayMetrics.heightPixels / 5);

        GpsMyLocationProvider gpsMyLocationProvider = new GpsMyLocationProvider(context);
        mLocationOverlay = new MyLocationNewOverlay(context, gpsMyLocationProvider, mMapView);

        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(mMinimapOverlay);

        mMapView.getController().setZoom(mPrefs.getInt(PREFS_ZOOM_LEVEL, 1));
        mMapView.scrollTo(mPrefs.getInt(PREFS_SCROLL_X, 0), mPrefs.getInt(PREFS_SCROLL_Y, 0));

        mLocationOverlay.enableMyLocation();

        mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mMapView);
        mCompassOverlay.enableCompass();
        mMapView.getOverlays().add(this.mCompassOverlay);

        if (isNetworkAvailable())
            new GetDataTask().execute();

        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

    }

    private void buildAlertMessageNoGps() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(getString(R.string.no_gps_msg))
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    public void setOverlayItems() {
        ItemizedIconOverlay<OverlayItem> mMyLocationOverlay = new ItemizedIconOverlay<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Snackbar snackbar = Snackbar
                                .make(appView, item.getTitle(), Snackbar.LENGTH_LONG)
                                .setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                                .setAction(R.string.details, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                                        alert.setTitle(item.getTitle());
                                        final WebView webView = new WebView(MainActivity.this);
                                        webView.loadDataWithBaseURL(null, item.getSnippet(), "text/html", "UTF-8", null);
                                        alert.setView(webView, 32, 0, 32, 0);
                                        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                            }
                                        });
                                        alert.show();
                                    }
                                });
                        snackbar.show();
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        return false;
                    }
                }, mResourceProxy);
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    public void parseJson(String json) {
        items = new ArrayList<>();

        try {
            int i, length;
            JSONArray jsonArray = new JSONArray(json);
            length = jsonArray.length();

            for (i = 0; i < length; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Farmacia farmacia = new Farmacia();
                farmacia.setNombre(jsonObject.getString("local_nombre"));
                farmacia.setComuna(jsonObject.getString("comuna_nombre"));
                farmacia.setLocalidad(jsonObject.getString("localidad_nombre"));
                farmacia.setDireccion(jsonObject.getString("local_direccion"));
                farmacia.setTelefono(jsonObject.getString("local_telefono"));
                farmacia.setHoraApertura(jsonObject.getString("funcionamiento_hora_apertura"));
                farmacia.setHoraCierre(jsonObject.getString("funcionamiento_hora_cierre"));
                farmacia.setDia(jsonObject.getString("funcionamiento_dia"));
                farmacia.setFecha(jsonObject.getString("fecha"));
                try {
                    farmacia.setLatitud(Double.parseDouble(jsonObject.getString("local_lat")));
                    farmacia.setLongitud(Double.parseDouble(jsonObject.getString("local_lng")));
                } catch (NumberFormatException e) {
                    farmacia.setLatitud(null);
                    farmacia.setLongitud(null);
                }
                if (farmacia.isValid())
                    items.add(new OverlayItem(farmacia.getNombre(), farmacia.toString(), new GeoPoint(farmacia.getLatitud(), farmacia.getLongitud())));
            }
            setOverlayItems();
        } catch (JSONException e) {
            e.printStackTrace();
            Snackbar.make(appView, R.string.json_error, Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void aboutDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Farmacias de turno");
        final WebView webView = new WebView(this);
        webView.loadUrl("file:///android_asset/about.html");
        alert.setView(webView, 32, 0, 32, 0);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_update:
                if (isNetworkAvailable())
                    new GetDataTask().execute();
                break;
            case R.id.action_about:
                aboutDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        final SharedPreferences.Editor edit = mPrefs.edit();
        edit.putString(PREFS_TILE_SOURCE, mMapView.getTileProvider().getTileSource().name());
        edit.putInt(PREFS_SCROLL_X, mMapView.getScrollX());
        edit.putInt(PREFS_SCROLL_Y, mMapView.getScrollY());
        edit.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        edit.putBoolean(PREFS_SHOW_LOCATION, mLocationOverlay.isMyLocationEnabled());
        edit.putBoolean(PREFS_SHOW_COMPASS, mCompassOverlay.isCompassEnabled());
        mCompassOverlay.disableCompass();
        edit.apply();

        mLocationOverlay.disableMyLocation();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String tileSourceName = mPrefs.getString(PREFS_TILE_SOURCE,
                TileSourceFactory.DEFAULT_TILE_SOURCE.name());
        try {
            final ITileSource tileSource = TileSourceFactory.getTileSource(tileSourceName);
            mMapView.setTileSource(tileSource);
        } catch (final IllegalArgumentException e) {
            mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        }
        if (mPrefs.getBoolean(PREFS_SHOW_LOCATION, false)) {
            mLocationOverlay.enableMyLocation();
        }
        mCompassOverlay.enableCompass();

    }

    private class GetDataTask extends AsyncTask<String, String, Void> {
        String result = "";

        protected void onPreExecute() {
            swipeContainer.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                URL url = new URL("http://farmanet.minsal.cl/index.php/ws/getLocalesTurnos");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader r = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                if (urlConnection.getResponseCode() == 200) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        result += line + "\n";
                    }
                } else {
                    Snackbar.make(appView, R.string.connection_error, Snackbar.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Log.e("IOException Data", result);
                e.printStackTrace();
                Snackbar.make(appView, R.string.connection_error, Snackbar.LENGTH_LONG).show();
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            parseJson(result);
            swipeContainer.setRefreshing(false);
        }
    }
}
