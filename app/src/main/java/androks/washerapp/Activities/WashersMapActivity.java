package androks.washerapp.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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

import androks.washerapp.Models.DirectionFinder;
import androks.washerapp.Models.DirectionFinderListener;
import androks.washerapp.Models.Route;
import androks.washerapp.Models.Washer;
import androks.washerapp.R;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class WashersMapActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, View.OnClickListener, GoogleMap.OnMapClickListener, DirectionFinderListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final int REQUEST_CHECK_SETTINGS_ON_START = 1000;
    private static final int REQUEST_CHECK_SETTINGS_ON_BUILD_ROUTE = 1001;
    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationManager locationManager;
    Status GpsStatus;

    private Location mCurrentLocation;

    //Polyline list using as buffer to build directions
    private List<Polyline> polylinePaths = new ArrayList<>();

    private BroadcastReceiver mInternetReceiver;

    //Reference for downloading all washers
    private DatabaseReference mWashersReference;
    //Reference for downloading Ids of free washers
    private DatabaseReference mFreeWashersReference;
    ValueEventListener mListenerForDownloadWashers, mListenerForDownloadFreeWashersList;

    //Relation between markers on map and list of washers
    private HashMap<String, Washer> mWashersList = new HashMap<>();
    private HashMap<String, Marker> mMarkersList = new HashMap<>();
    private ArrayList<String> mWashersNonfreeList = new ArrayList<>();
    private ArrayList<String> mWashersFreeList = new ArrayList<>();
    private String currentWasherId;
    private LatLng mLocationOfCurrentWasherToBuildRoute;

    /**
     * Views
     */

    //View for showing download all list of washers indicator
    private View mLoadingIndicator;

    //View to handle change showing marker types
    private FloatingActionButton mShowOnlyFreeWashersFab;

    private FloatingActionButton mOrderToNearestWash;

    /**
     * Navigation feature
     */
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    NavigationView mNavigationView;
    /**
     * Bottom Sheet views
     */
    BottomSheetBehavior behavior;
    //Flag to handle if bottom sheet expanded or not
    private FloatingActionButton mOrderToCurrentWash;

    /**
     * Flags
     */
    boolean botoom_sheet_expanded;
    private boolean isDirectionAlreadyBuilt;
    private boolean includeBusyWashers = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_washers_map);
        mLoadingIndicator = findViewById(R.id.loading_washers_indicator);
        mLoadingIndicator.setVisibility(View.VISIBLE);
        //Receiver for stable internet connection
        mInternetReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                checkInternetConnection();
            }
        };
        registerReceiver(mInternetReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        createLocationRequest();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest).setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                GpsStatus = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (GpsStatus.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            GpsStatus.startResolutionForResult(WashersMapActivity.this, REQUEST_CHECK_SETTINGS_ON_START);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Toast.makeText(getApplicationContext(), "Location System error", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        /**
         * Views
         */


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
                mLoadingIndicator.setVisibility(View.VISIBLE);
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Washer temp = child.getValue(Washer.class);
                    mWashersList.put(temp.getId(), temp);
                }
                if(!mWashersList.isEmpty())mLoadingIndicator.setVisibility(View.GONE);
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
                        if (washer != null) {
                            washer.setStatus((Boolean) child.getValue());

                            if (washer.getStatus()) {
                                mWashersNonfreeList.remove(washer.getId());
                                mWashersFreeList.add(washer.getId());
                            } else {
                                mWashersNonfreeList.add(washer.getId());
                                mWashersFreeList.remove(washer.getId());
                            }


                            setWasherToMap(mWashersList.get(child.getKey()));
                        }

                    }
                if(!mWashersList.isEmpty())mLoadingIndicator.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Error while download\n Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        };

        /**
         * Navigation feature
         */


        setUpToolbar();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.nav_drawer);
        setUpNavDrawer();
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.navigation_item_1:
                        startActivity(new Intent(WashersMapActivity.this, CarSelectActivity.class));
                        return true;
                    case R.id.navigation_item_2:
                        Toast.makeText(WashersMapActivity.this, "Item Two", Toast.LENGTH_SHORT).show();
                        return true;
                    default:
                        return true;
                }
            }
        });
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
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    mShowOnlyFreeWashersFab.startAnimation(shrinkAnimation);
                    mOrderToNearestWash.startAnimation(shrinkAnimation);
                    mShowOnlyFreeWashersFab.setVisibility(View.GONE);
                    mOrderToNearestWash.setVisibility(View.GONE);
                    botoom_sheet_expanded = true;
                } else if (botoom_sheet_expanded) {
                    mShowOnlyFreeWashersFab.startAnimation(growAnimation);
                    mOrderToNearestWash.startAnimation(growAnimation);
                    mShowOnlyFreeWashersFab.setVisibility(View.VISIBLE);
                    mOrderToNearestWash.setVisibility(View.VISIBLE);
                    botoom_sheet_expanded = false;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        //setting flags
        isDirectionAlreadyBuilt = false;

        //Adding listeners for bottom sheet views
        findViewById(R.id.bottom_sheet_title).setOnClickListener(this);
        findViewById(R.id.bottom_sheet_order_fab).setOnClickListener(this);
    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Adding single value listener to download list of washers
        mWashersReference.addValueEventListener(mListenerForDownloadWashers);
        //Adding single value listener to download list of only free washers
        mFreeWashersReference.addValueEventListener(mListenerForDownloadFreeWashersList);

    }

    @Override
    protected void onPause() {
        stopLocationUpdates();
        super.onPause();
    }

    @Override
    protected void onStop() {

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

        unregisterReceiver(mInternetReceiver);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS_ON_START:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
            case REQUEST_CHECK_SETTINGS_ON_BUILD_ROUTE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        isDirectionAlreadyBuilt = false;
                        startLocationUpdates();
                        buildRouteFromCurrentToMarkerLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
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
        ((TextView) bottomSheet.findViewById(R.id.washer_name)).setText(washer.getName());
        ((TextView) bottomSheet.findViewById(R.id.washer_location)).setText(String.valueOf(washer.getLangtitude() + washer.getLongtitude()));
        ((TextView) bottomSheet.findViewById(R.id.washer_phone)).setText(washer.getPhone());
        ((TextView) bottomSheet.findViewById(R.id.washer_opening_hours)).setText(washer.getHours());
    }

    @Override
    public void onMapClick(LatLng latLng) {
        //Hide bottom sheet if map clicked
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onDirectionFinderStart() {
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {


        if (polylinePaths != null)
            for (Polyline polyline : polylinePaths)
                polyline.remove();

        polylinePaths = new ArrayList<>();

        for (Route route : routes) {
            if (!isDirectionAlreadyBuilt)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 14));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(12);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));

            isDirectionAlreadyBuilt = true;
            mLocationOfCurrentWasherToBuildRoute = route.endLocation;
        }
        mLoadingIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_status_marker:
                includeBusyWashers = !includeBusyWashers;
                for (String washerId : mWashersNonfreeList)
                    mMarkersList.get(washerId).setVisible(includeBusyWashers);
                mShowOnlyFreeWashersFab.setImageResource(includeBusyWashers ? R.mipmap.ic_marker_free : R.mipmap.ic_markers_all);
                break;

            case R.id.bottom_sheet_title:
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                break;

            case R.id.bottom_sheet_order_fab:
                mLocationOfCurrentWasherToBuildRoute = mMarkersList.get(currentWasherId).getPosition();
                if (gpsEnabled()) {
                    isDirectionAlreadyBuilt = false;
                    buildRouteFromCurrentToMarkerLocation();
                } else {
                    try {
                        GpsStatus.startResolutionForResult(WashersMapActivity.this, REQUEST_CHECK_SETTINGS_ON_BUILD_ROUTE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case R.id.fab_get_direction:
                if (mWashersList.isEmpty() || mMarkersList.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "No washers avaliable", Toast.LENGTH_SHORT).show();
                    break;
                }

                if (gpsEnabled()) {
                    isDirectionAlreadyBuilt = false;
                    mLocationOfCurrentWasherToBuildRoute = find_closest_marker().getPosition();
                    buildRouteFromCurrentToMarkerLocation();
                } else {
                    try {
                        GpsStatus.notify();
                        GpsStatus.startResolutionForResult(WashersMapActivity.this, REQUEST_CHECK_SETTINGS_ON_BUILD_ROUTE);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
                break;
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // Note that this can be NULL if last location isn't already known.
        if (mCurrentLocation != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
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
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        // New location has now been determined
        mCurrentLocation = location;
        buildRouteFromCurrentToMarkerLocation();
    }

    protected void createLocationRequest() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
    }

    protected void buildRouteFromCurrentToMarkerLocation() {
        if (mCurrentLocation != null && mLocationOfCurrentWasherToBuildRoute != null) {
            try {
                new DirectionFinder(this, new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), mLocationOfCurrentWasherToBuildRoute).execute();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    protected boolean gpsEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return true;
        return false;
    }

    private void setUpToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
    }

    private void setUpNavDrawer() {
        if (mToolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationIcon(R.drawable.ic_drawer);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

    }

    private Marker find_closest_marker() {

        double pi = Math.PI;
        int R = 6371; //equatorial radius
        double[] distances = new double[mWashersFreeList.size()];
        int closest = -1;
        for (int i = 0; i < mWashersFreeList.size(); i++) {
            double lat2 = mMarkersList.get(mWashersFreeList.get(i)).getPosition().latitude;
            double lon2 = mMarkersList.get(mWashersFreeList.get(i)).getPosition().longitude;

            double chLat = lat2 - mCurrentLocation.getLatitude();
            double chLon = lon2 - mCurrentLocation.getLongitude();

            double dLat = chLat * (pi / 180);
            double dLon = chLon * (pi / 180);

            double rLat1 = mCurrentLocation.getLatitude() * (pi / 180);
            double rLat2 = lat2 * (pi / 180);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(rLat1) * Math.cos(rLat2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double d = R * c;

            distances[i] = d;
            if (closest == -1 || d < distances[closest]) {
                closest = i;
            }
        }
        return mMarkersList.get(mWashersFreeList.get(closest));
    }
}
