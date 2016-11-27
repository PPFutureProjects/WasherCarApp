package androks.washerapp.Activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import androks.washerapp.Models.Washer;
import androks.washerapp.R;

public class WashersMapActivity extends BaseActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, View.OnClickListener, GoogleMap.OnMapClickListener {

    private GoogleMap mMap;

    //Reference for downloading all washers
    private DatabaseReference mWashersReference;

    //Reference for downloading Ids of free washers
    private DatabaseReference mFreeWashersReference;

    ValueEventListener mListenerForDownloadWashers, mListenerForDownloadFreeWashersList;

    //Relation between markers on map and list of washers
    private HashMap<String, Washer> mWashersList = new HashMap<>();
    private HashMap<String, Marker> mMarkersList = new HashMap<>();
    private ArrayList<Washer> mWashersNonfreeList = new ArrayList<>();

    //View for showing download all list of washers indicator
    private View mLoadingWashersView;

    //View to handle change showing marker types
    private FloatingActionButton mChangeMarkerStatusFab;
    private boolean includeBusyWashers = true;
    private FloatingActionButton mOrderToWash;

    BottomSheetBehavior behavior;

    boolean STATE_EXPANDED;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_washers_map);

        showProgressDialog();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        //Finding views
        mLoadingWashersView = findViewById(R.id.loading_washers_indicator);

        mChangeMarkerStatusFab = (FloatingActionButton) findViewById(R.id.fab_status_marker);
        mChangeMarkerStatusFab.setOnClickListener(this);

        mOrderToWash = (FloatingActionButton) findViewById(R.id.fab_get_direction);
        mOrderToWash.setOnClickListener(this);

        mWashersReference = FirebaseDatabase.getInstance().getReference().child("washers");
        mFreeWashersReference = FirebaseDatabase.getInstance().getReference().child("free-washers");

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
                mLoadingWashersView.setVisibility(View.VISIBLE);
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
                mLoadingWashersView.setVisibility(View.GONE);
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

        behavior = BottomSheetBehavior.from(findViewById(R.id.coordinatorLayout).findViewById(R.id.bottom_sheet));
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
               if(newState == BottomSheetBehavior.STATE_EXPANDED){
                   mChangeMarkerStatusFab.startAnimation(shrinkAnimation);
                   mOrderToWash.startAnimation(shrinkAnimation);
                   mChangeMarkerStatusFab.setVisibility(View.GONE);
                   mOrderToWash.setVisibility(View.GONE);
                   STATE_EXPANDED = true;
               }else if(STATE_EXPANDED){
                   mChangeMarkerStatusFab.startAnimation(growAnimation);
                   mOrderToWash.startAnimation(growAnimation);
                   mChangeMarkerStatusFab.setVisibility(View.VISIBLE);
                   mOrderToWash.setVisibility(View.VISIBLE);
                   STATE_EXPANDED = false;
               }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
        findViewById(R.id.bottom_sheet_title_layout).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoadingWashersView.setVisibility(View.VISIBLE);

        //Adding single value listener to download list of washers
        mWashersReference.addValueEventListener(mListenerForDownloadWashers);
        //Adding single value listener to download list of only free washers
        mFreeWashersReference.addValueEventListener(mListenerForDownloadFreeWashersList);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        hideProgressDialog();
    }

    private void setWasherToMap(Washer washer) {
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
            mMarkersList.get(washer.getId()).setIcon(BitmapDescriptorFactory.defaultMarker(washer.getStatus() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED));
            if (washer.getStatus()) mMarkersList.get(washer.getId()).setVisible(true);
            if (!includeBusyWashers && !washer.getStatus())
                mMarkersList.get(washer.getId()).setVisible(false);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 14));
        inflateWasherDetails(mWashersList.get(marker.getTitle()));
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        findViewById(R.id.bottom_sheet_order_fab).startAnimation(AnimationUtils.loadAnimation(this, R.anim.simple_grow));
        return true;
    }

    private void inflateWasherDetails(Washer washer) {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        ((TextView)bottomSheet.findViewById(R.id.washer_name)).setText(washer.getName());
        ((TextView)bottomSheet.findViewById(R.id.washer_location)).setText(String.valueOf(washer.getLangtitude()+washer.getLongtitude()));
        ((TextView)bottomSheet.findViewById(R.id.washer_phone)).setText(washer.getPhone());
        ((TextView)bottomSheet.findViewById(R.id.washer_opening_hours)).setText(washer.getHours());
    }

    @Override
    public void onMapClick(LatLng latLng) {
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_status_marker:
                includeBusyWashers = !includeBusyWashers;
                addBusyWashersToMap(includeBusyWashers);
                break;
            case R.id.bottom_sheet_title_layout:
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                break;
        }
    }

    private void addBusyWashersToMap(boolean temp){
        for (Washer washer : mWashersNonfreeList)
            mMarkersList.get(washer.getId()).setVisible(temp);
        mChangeMarkerStatusFab.setImageResource(temp?R.mipmap.ic_marker_free :   R.mipmap.ic_markers_all);
    }
}
