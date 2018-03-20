package com.jacksvarghese.clusterdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.Cluster;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.ClusterItem;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.ClusterManagerPlugin;
import com.mapbox.mapboxsdk.plugins.cluster.clustering.view.DefaultClusterRenderer;
import com.mapbox.mapboxsdk.plugins.cluster.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Customize default markers to any given layout
 */
public class CustomMarkerClusterActivity extends AppCompatActivity implements
        ClusterManagerPlugin.OnClusterClickListener<CustomMarkerClusterActivity.Picture>,
        ClusterManagerPlugin.OnClusterInfoWindowClickListener<CustomMarkerClusterActivity.Picture>,
        ClusterManagerPlugin.OnClusterItemClickListener<CustomMarkerClusterActivity.Picture>,
        ClusterManagerPlugin.OnClusterItemInfoWindowClickListener<CustomMarkerClusterActivity.Picture> {

    private MapView mMapView;
    private MapboxMap mMapboxMap;
    private ClusterManagerPlugin<Picture> mClusterManager;
    private static final int[] DRAW_IDS = new int[]{R.drawable.gran, R.drawable.john, R.drawable.mechanic,
            R.drawable.ruth, R.drawable.stefan, R.drawable.teacher, R.drawable.turtle, R.drawable.walter, R.drawable.yeats};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_marker_clusters);

        mMapView = (MapView) findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                CustomMarkerClusterActivity.this.mMapboxMap = mapboxMap;
                setup();
            }
        });
    }

    protected void setup() {
        mMapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.865539, 2.348603), 10.8),2800);

        // Initializing the cluster plugin
        mClusterManager = new ClusterManagerPlugin<>(CustomMarkerClusterActivity.this, mMapboxMap);
        initCameraListeners();

        //Set a custom renderer. This will help us to modify the default markers.
        mClusterManager.setRenderer(new PictureRenderer(getApplicationContext(), mMapboxMap, mClusterManager));

        addItems();
        mClusterManager.cluster();
    }

    protected void initCameraListeners() {
        mMapboxMap.addOnCameraIdleListener(mClusterManager);
        mMapboxMap.setOnMarkerClickListener(mClusterManager);
        mMapboxMap.setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(CustomMarkerClusterActivity.this);
        mClusterManager.setOnClusterInfoWindowClickListener(CustomMarkerClusterActivity.this);
        mClusterManager.setOnClusterItemClickListener(CustomMarkerClusterActivity.this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(CustomMarkerClusterActivity.this);

    }

    protected void addItems() {
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.paris_bike_share_hubs);
            List<Picture> items = new MyItemReader().read(inputStream);
            mClusterManager.addItems(items);
        } catch (JSONException exception) {
            Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onClusterClick(Cluster<Picture> cluster) {
        // Create the builder to collect all essential cluster items for the bounds.
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();

        // Animate camera to the bounds
        try {
            mMapboxMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<Picture> cluster) {

    }

    @Override
    public boolean onClusterItemClick(Picture item) {
        //Handle click here
        return true;
    }

    @Override
    public void onClusterItemInfoWindowClick(Picture item) {

    }

    /**
     * Custom class for use by the marker cluster plugin
     */
    public static class Picture implements ClusterItem {
        private final LatLng position;
        private String title;
        private String snippet;
        private int drawableRes;

        public Picture(double lat, double lng, String title, String snippet, int drawableRes) {
            position = new LatLng(lat, lng);
            this.title = title;
            this.snippet = snippet;
            this.drawableRes = drawableRes;
        }

        @Override
        public LatLng getPosition() {
            return position;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getSnippet() {
            return snippet;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public int getDrawableRes() {
            return drawableRes;
        }
    }

    private class PictureRenderer extends DefaultClusterRenderer<Picture> {

        private final IconGenerator itemIcon = new IconGenerator(getApplicationContext());
        private final IconGenerator clusterIcon  = new IconGenerator(getApplicationContext());
        private ImageView itemIV;
        private ImageView clusterIV;
        private TextView clusterCountTV;
        private TextView itemNameTV;
        private final int clusterSize;
        private IconFactory iconFactory;

        public PictureRenderer(Context context, MapboxMap map, ClusterManagerPlugin<Picture> clusterManagerPlugin) {
            super(context, map, clusterManagerPlugin);

            //Inflate cluster layout and set it to clusterIcon generator
            View multiPicture = getLayoutInflater().inflate(R.layout.cluster_marker, null);
            clusterIcon.setContentView(multiPicture);
            clusterIV = (ImageView) multiPicture.findViewById(R.id.m_image);
            clusterCountTV = multiPicture.findViewById(R.id.count);
            clusterSize = (int) getResources().getDimension(R.dimen.cluster_image_size);

            //Inflate item marker layout and set it to clusterIcon generator
            View singlePicture = getLayoutInflater().inflate(R.layout.item_marker, null);
            itemIcon.setContentView(singlePicture);
            itemIV = singlePicture.findViewById(R.id.s_image);
            itemNameTV = singlePicture.findViewById(R.id.text);
            iconFactory = IconFactory.getInstance(context);
        }

        @Override
        protected void onBeforeClusterItemRendered(Picture item, MarkerOptions markerOptions) {
            //Customize the item marker here.

            //set values to item marker layout
            itemIV.setImageResource(item.getDrawableRes());
            itemNameTV.setText(item.getTitle());
            //Generate icon
            Bitmap icon = itemIcon.makeIcon();

            //Set icon to marker
            markerOptions.icon(iconFactory.fromBitmap(icon));
            markerOptions.setTitle(item.getTitle());
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<Picture> cluster, MarkerOptions markerOptions) {
            //Customize the item marker here.

            //Create a drawable using four images
            List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            int width = clusterSize;
            int height = clusterSize;

            for (Picture p : cluster.getItems()) {
                // Draw 4 at most.
                if (profilePhotos.size() == 4) break;
                Drawable drawable = getResources().getDrawable(p.getDrawableRes());
                drawable.setBounds(0, 0, width, height);
                profilePhotos.add(drawable);
            }
            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, width, height);

            //set values to cluster marker layout
            clusterIV.setImageDrawable(multiDrawable);
            clusterCountTV.setText(String.valueOf(cluster.getSize()));
            //Generate icon
            Bitmap icon = clusterIcon.makeIcon();

            //Set icon to marker
            markerOptions.icon(iconFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 1;
        }
    }


    // Add the mMapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * Custom class which reads JSON data and creates a list of Picture objects
     */
    public static class MyItemReader {

        private static final String REGEX_INPUT_BOUNDARY_BEGINNING = "\\A";

        public List<Picture> read(InputStream inputStream) throws JSONException {
            List<Picture> items = new ArrayList<Picture>();
            String json = new Scanner(inputStream).useDelimiter(REGEX_INPUT_BOUNDARY_BEGINNING).next();
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                String title = null;
                String snippet = null;
                JSONObject object = array.getJSONObject(i);
                double lat = object.getDouble("latitude");
                double lng = object.getDouble("longitude");
                if (!object.isNull("name")) {
                    title = object.getString("name");
                }
                if (!object.isNull("address")) {
                    snippet = object.getString("address");
                }
                items.add(new Picture(lat, lng, title, snippet, DRAW_IDS[i%9]));
            }
            return items;
        }
    }
}