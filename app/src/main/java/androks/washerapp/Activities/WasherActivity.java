package androks.washerapp.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androks.washerapp.Models.Washer;
import androks.washerapp.R;

public class WasherActivity extends AppCompatActivity {

    private DatabaseReference mWasherReference;
    private ValueEventListener listenerForSingleWasher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_washer);

        mWasherReference = FirebaseDatabase.getInstance().getReference().child("washers").child(getIntent().getStringExtra("id"));

        listenerForSingleWasher = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                inflateView(dataSnapshot.getValue(Washer.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void inflateView(Washer washer) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mWasherReference.addListenerForSingleValueEvent(listenerForSingleWasher);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWasherReference.removeEventListener(listenerForSingleWasher);
    }
}
