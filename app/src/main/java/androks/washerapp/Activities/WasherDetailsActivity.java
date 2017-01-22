package androks.washerapp.Activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.cachapa.expandablelayout.ExpandableLayout;

import androks.washerapp.Models.Washer;
import androks.washerapp.R;

public class WasherDetailsActivity extends BaseActivity implements View.OnClickListener {

    private CollapsingToolbarLayout mCollapsingToolbar;
    private String mWasherId;
    private Washer mWasher;
    int mutedColor = R.attr.colorPrimary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_washer_details);
        showProgressDialog();

        mWasherId = getIntent().getStringExtra("id");

        final Toolbar toolbar = (Toolbar) findViewById(R.id.anim_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FirebaseDatabase.getInstance().getReference()
                .child("washers")
                .child(mWasherId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mWasher = dataSnapshot.getValue(Washer.class);
                inflateView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        ImageView header = (ImageView) findViewById(R.id.header);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.vianor);
        header.setImageBitmap(bitmap);
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {
                mutedColor = palette.getMutedColor(R.color.primary_500);
                mCollapsingToolbar.setContentScrimColor(mutedColor);
                mCollapsingToolbar.setStatusBarScrimColor(R.color.black_trans80);
            }
        });

    }

    private void inflateView() {
        mCollapsingToolbar.setTitle(mWasher.getName());
        ((TextView) findViewById(R.id.stars)).setText(String.valueOf(mWasher.getStars() + " people like it"));
        ((TextView) findViewById(R.id.washer_location)).setText(mWasher.getLocation());
        ((TextView) findViewById(R.id.washer_phone)).setText(mWasher.getPhone());
        ((TextView) findViewById(R.id.washer_opening_hours)).setText(mWasher.getHours());
        ((TextView) findViewById(R.id.free_boxes)).setText(mWasher.getFreeBoxes() + " of " + mWasher.getBoxes() + " boxes are free");
        ((TextView) findViewById(R.id.description)).setText(mWasher.getDescription());
        ((TextView) findViewById(R.id.prices)).setText(mWasher.getPriceList());
        ((ImageView) findViewById(R.id.wifi)).setColorFilter(mWasher.getWifi() ?
                ContextCompat.getColor(this, R.color.colorServiceAvailable): ContextCompat.getColor(this, R.color.colorServiceNotAvailable));
        ((ImageView) findViewById(R.id.coffee)).setColorFilter(mWasher.getCafe() ?
                ContextCompat.getColor(this, R.color.colorServiceAvailable): ContextCompat.getColor(this, R.color.colorServiceNotAvailable));
        ((ImageView) findViewById(R.id.lunch_room)).setColorFilter(mWasher.getLunchRoom() ?
                ContextCompat.getColor(this, R.color.colorServiceAvailable): ContextCompat.getColor(this, R.color.colorServiceNotAvailable));
        ((ImageView) findViewById(R.id.rest_room)).setColorFilter(mWasher.getRestRoom() ?
                ContextCompat.getColor(this, R.color.colorServiceAvailable): ContextCompat.getColor(this, R.color.colorServiceNotAvailable));
        ((ImageView) findViewById(R.id.wc)).setColorFilter(mWasher.getWc() ?
                ContextCompat.getColor(this, R.color.colorServiceAvailable): ContextCompat.getColor(this, R.color.colorServiceNotAvailable));
        ((ImageView) findViewById(R.id.tire)).setColorFilter(mWasher.getTire() ?
                ContextCompat.getColor(this, R.color.colorServiceAvailable): ContextCompat.getColor(this, R.color.colorServiceNotAvailable));
        findViewById(R.id.prices_btn).setOnClickListener(this);
        findViewById(R.id.description_btn).setOnClickListener(this);
        hideProgressDialog();
    }

    @Override
    public void onClick(View view) {
        ExpandableLayout expandableLayout;
        switch (view.getId()){
            case R.id.prices_btn:
                expandableLayout = (ExpandableLayout) findViewById(R.id.expandable_prices);
                expandableLayout.toggle(true);
                break;

            case R.id.description_btn:
                expandableLayout = (ExpandableLayout) findViewById(R.id.expandable_description);
                expandableLayout.toggle(true);
                break;
        }
    }
}
