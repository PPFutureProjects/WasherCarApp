package androks.washerapp.Fragments;

import android.support.v4.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androks.washerapp.Models.Car;

/**
 * Created by androks on 12/11/2016.
 */

public abstract class BaseFragment extends Fragment {
    protected Car mCurrentCar;
    protected FirebaseUser mCurrentUser;

    public FirebaseUser getCurrentUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void checkCurrentUser(){
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public void checkCurrentCar(){
        FirebaseUser user = getCurrentUser();
        if(user != null) {
            final DatabaseReference mUserRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(user.getUid());

            mUserRef.child("current-car").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    String currentCarId = dataSnapshot.getValue(String.class);

                    mUserRef.child("cars").child(currentCarId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            mCurrentCar = dataSnapshot.getValue(Car.class);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
