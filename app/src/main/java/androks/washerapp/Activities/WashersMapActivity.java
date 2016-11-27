package androks.washerapp.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androks.washerapp.Models.Washer;
import androks.washerapp.Modules.DirectionFinder;
import androks.washerapp.Modules.DirectionFinderListener;
import androks.washerapp.Modules.Route;
import androks.washerapp.R;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class WashersMapActivity extends BaseActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        View.OnClickListener,
        GoogleMap.OnMapClickListener,
        DirectionFinderListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    private Location currentLocation;

    //Polyline list using as buffer to build directions
    private List<Polyline> polylinePaths = new ArrayList<>();

    private boolean isDirectionBuilt;

    //Reference for downloading all washers
    private DatabaseReference mWashersReference;
    //Reference for downloading Ids of free washers
    private DatabaseReference mFreeWashersReference;
    ValueEventListener mListenerForDownloadWashers, mListenerForDownloadFreeWashersList;

    //Relation between markers on map and list of washers
    private HashMap<String, Washer> mWashersList = new HashMap<>();
    private HashMap<String, Marker> mMarkersList = new HashMap<>();
    private ArrayList<Washer> mWashersNonfreeList = new ArrayList<>();
    private String currentWasherId;
    private LatLng currentWasherLocation;
    /**
     * Views
     */

    //View for showing download all list of washers indicator
    private View mLoadingIndicator;

    //View to handle change showing marker types
    private FloatingActionButton mShowOnlyFreeWashersFab;
    private boolean includeBusyWashers = true;
    private FloatingActionButton mOrderToNearestWash;

    /**
     * Bottom Sheet views
     */
    BottomSheetBehavior behavior;
    //Flag to handle if bottom sheet expanded or not
    boolean botoom_sheet_expanded;
    private FloatingActionButton mOrderToCurrentWash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_washers_map);

        //Receiver for stable internet connection
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                checkInternetConnection();
            }
        }, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = new LocationRequest().setInterval(10000).setFastestInterval(5000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        isDirectionBuilt = false;

        /**
         * Views
         */
        mLoadingIndicator = findViewById(R.id.loading_washers_indicator);

        mShowOnlyFreeWashersFab = (FloatingActionButton) findViewById(R.id.fab_status_marker);
        mShowOnlyFreeWashersFab.setOnClickListener(this);

        mOrderToNearestWash = (FloatingActionButton) findViewById(R.id.fab_get_direction);
        mOrderToNearestWash.setOnClickListener(this);

        /**
         * Database section
         */
        mWashersReference = FirebaseDatabase.getInstance().getReference().child("washers");
        mFreeWashersReference = FirebaseDatabase.getInstance().getReference().child("free-washers");

        /**
         * Database listener implementation
         */
        mListenerForDownloadWashers = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Washer temp = child.getValue(Washer.class);
                    mWashersList.put(temp.getId(), temp);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error while download\n Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        };

        mListenerForDownloadFreeWashersList = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mLoadingIndicator.setVisibility(View.VISIBLE);
                if (dataSnapshot.hasChildren())
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Washer washer = mWashersList.get(child.getKey());
                        if(washer != null) {
                            washer.setStatus((Boolean) child.getValue());

                            if (!washer.getStatus())
                                mWashersNonfreeList.add(washer);
                            else mWashersNonfreeList.remove(washer);

                            setWasherToMap(mWashersList.get(child.getKey()));
                        }

                    }
                mLoadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error while download\n Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        };

        /**
         * Bottom Sheet
         */

        // To handle FAB animation upon entrance and exit
        final Animation growAnimation = AnimationUtils.loadAnimation(this, R.anim.simple_grow);
        final Animation shrinkAnimation = AnimationUtils.loadAnimation(this, R.anim.simple_shrink);

        //Adding behavior for bottom sheet
        behavior = BottomSheetBehavior.from(findViewById(R.id.coordinatorLayout).findViewById(R.id.bottom_sheet));
        //Set bottom sheet hidden by default
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        //Set listeners for bottom sheet states
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
               if(newState == BottomSheetBehavior.STATE_EXPANDED){
                   mShowOnlyFreeWashersFab.startAnimation(shrinkAnimation);
                   mOrderToNearestWash.startAnimation(shrinkAnimation);
                   mShowOnlyFreeWashersFab.setVisibility(View.GONE);
                   mOrderToNearestWash.setVisibility(View.GONE);
                   botoom_sheet_expanded = true;
               }else if(botoom_sheet_expanded){
                   mShowOnlyFreeWashersFab.startAnimation(growAnimation);
                   mOrderToNearestWash.startAnimation(growAnimation);
                   mShowOnlyFreeWashersFab.setVisibility(View.VISIBLE);
                   mOrderToNearestWash.setVisibility(View.VISIBLE);
                   botoom_sheet_expanded = false;
               }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        //Adding listeners for bottom sheet views
        findViewById(R.id.bottom_sheet_title).setOnClickListener(this);
        findViewById(R.id.bottom_sheet_order_fab).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mLoadingIndicator.setVisibility(View.VISIBLE);

        //Adding single value listener to download list of washers
        mWashersReference.addValueEventListener(mListenerForDownloadWashers);
        //Adding single value listener to download list of only free washers
        mFreeWashersReference.addValueEventListener(mListenerForDownloadFreeWashersList);

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnecting the client invalidates it.
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        // only stop if it's connected, otherwise we crash
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        //Adding single value listener to download list of washers
        mWashersReference.removeEventListener(mListenerForDownloadWashers);
        //Adding single value listener to download list of only free washers
        mFreeWashersReference.removeEventListener(mListenerForDownloadFreeWashersList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
    }

    private void setWasherToMap(Washer washer) {
        //Adding map is does not exist
        if (!mMarkersList.containsKey(washer.getId())) {
            mMarkersList.put(washer.getId(),
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(washer.getLangtitude(), washer.getLongtitude()))
                            .title(washer.getName())
                            .icon(BitmapDescriptorFactory.defaultMarker(washer.getStatus() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED))
                            .title(washer.getId())
                    )
            );
        } else {
            /**
             * Change marker parameters after changing marker status
             */
            //Set marker image
            mMarkersList.get(washer.getId()).setIcon(BitmapDescriptorFactory.defaultMarker(washer.getStatus() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED));
            //Set washer visible if has free status
            if (washer.getStatus())
                mMarkersList.get(washer.getId()).setVisible(true);
            //Dont show marker if washer has false status and busy washers don't showing
            if (!includeBusyWashers && !washer.getStatus())
                mMarkersList.get(washer.getId()).setVisible(false);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //Move camera to clicked marker and set washer's id to currentWash string
        currentWasherId = marker.getTitle();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 14));
        //Inflating bottom sheet view by washer details
        inflateWasherDetails(mWashersList.get(marker.getTitle()));
        //Show bottom sheet as collapsed
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        //Show order to current wash button with animation
        findViewById(R.id.bottom_sheet_order_fab).startAnimation(AnimationUtils.loadAnimation(this, R.anim.simple_grow));
        return true;
    }

    private void inflateWasherDetails(Washer washer) {
        //TODO: create normal washers details view
        View bottomSheet = findViewById(R.id.bottom_sheet);
        ((TextView)bottomSheet.findViewById(R.id.washer_name)).setText(washer.getName());
        ((TextView)bottomSheet.findViewById(R.id.washer_location)).setText(String.valueOf(washer.getLangtitude()+washer.getLongtitude()));
        ((TextView)bottomSheet.findViewById(R.id.washer_phone)).setText(washer.getPhone());
        ((TextView)bottomSheet.findViewById(R.id.washer_opening_hours)).setText(washer.getHours());
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //Hide bottom sheet if map clicked
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_status_marker:
                includeBusyWashers = !includeBusyWashers;
                for (Washer washer : mWashersNonfreeList)
                    mMarkersList.get(washer.getId()).setVisible(includeBusyWashers);
                mShowOnlyFreeWashersFab.setImageResource(includeBusyWashers?R.mipmap.ic_marker_free :   R.mipmap.ic_markers_all);
                break;

            case R.id.bottom_sheet_title:
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;

            case R.id.bottom_sheet_order_fab:
                if(currentLocation != null){
                    try {
                        new DirectionFinder(this, new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), mMarkersList.get(currentWasherId).getPosition()).execute();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                isDirectionBuilt = false;
                break;
        }
    }

    @Override
    public void onDirectionFinderStart() {
        mLoadingIndicator.setVisibility(View.VISIBLE);
        if (polylinePaths != null)
            for (Polyline polyline:polylinePaths)
                polyline.remove();
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        mLoadingIndicator.setVisibility(View.GONE);
        polylinePaths = new ArrayList<>();

        for (Route route : routes) {
            if(!isDirectionBuilt)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 14));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(12);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
            isDirectionBuilt = true;
            currentWasherLocation = route.endLocation;
        }
    }

    private void checkInternetConnection() {
        boolean isProcess;
        try {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            isProcess = networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            isProcess = false;
            e.printStackTrace();
        }

        if (!isProcess) {
            try {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(WashersMapActivity.this, R.style.AppCompatAlertDialogStyle);
                builder.setTitle("Internet not avaliable");
                builder.setMessage("You are offline. Please, check your internet connection");
                builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        checkInternetConnection();
                    }
                });
                builder.setCancelable(false);
                builder.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Crouton.makeText(WashersMapActivity.this, "Internet connection avaliable", Style.CONFIRM).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Get last known recent location.
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // Note that this can be NULL if last location isn't already known.
        if (mCurrentLocation != null) {
            // Print current location if not null
            Log.d("DEBUG", "current location: " + mCurrentLocation.toString());
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        }
        // Begin polling for new location updates.
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == CAUSE_SERVICE_DISCONNECTED) {
            Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
        } else if (i == CAUSE_NETWORK_LOST) {
            Toast.makeText(this, "Network lost. Please re-connect.", Toast.LENGTH_SHORT).show();
        }
    }

    // Trigger new location updates at interval
    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {
        // New location has now been determined
        currentLocation = location;
        if(isDirectionBuilt){
            try {
                new DirectionFinder(this, new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), currentWasherLocation).execute();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

}
