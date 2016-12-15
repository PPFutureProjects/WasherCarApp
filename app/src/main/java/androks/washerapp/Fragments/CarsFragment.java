package androks.washerapp.Fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androks.washerapp.Activities.LoginActivity;
import androks.washerapp.Models.AddCarDialog;
import androks.washerapp.Models.Car;
import androks.washerapp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CarsFragment extends BaseFragment implements AddCarDialog.AddCarDialogListener {

    //No ideas why I have to use this, without it it not work
    private static final int REQUEST_WEIGHT = 1;

    private DatabaseReference mCurrentUserRef;

    private ListView mCarsListView;
    private ProgressBar mProgressBar;
    private View mNoCarsText;
    private String mCurrentCar;

    public CarsFragment() {
        // Required empty public constructor
    }

    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_cars, container, false);

        mCarsListView = (ListView) rootView.findViewById(R.id.cars_list_view);
        mNoCarsText = rootView.findViewById(R.id.no_cars_text);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.loading_cars_indicator);

        rootView.findViewById(R.id.add_car_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppCompatDialogFragment addCarDialog = new AddCarDialog();
                addCarDialog.setTargetFragment(CarsFragment.this, REQUEST_WEIGHT);
                addCarDialog.show(getFragmentManager(), "Add car");
            }
        });

        if (getCurrentUser() == null)
            startActivityForResult(new Intent(getActivity(), LoginActivity.class), 101);
        else {
            updateCurrentCar();

        }

        return rootView;
    }

    private void updateCurrentCar() {

        mCurrentUserRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(getCurrentUser().getUid());

        mCurrentUserRef.child("current-car").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mCurrentCar = dataSnapshot.getValue(String.class);
                if (mCurrentCar == null) {
                    mNoCarsText.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.GONE);
                    mNoCarsText.setVisibility(View.GONE);
                    setCarsList();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void setCarsList() {

        mCarsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        final FirebaseListAdapter<Car> adapter = new FirebaseListAdapter<Car>(
                getActivity(),
                Car.class,
                android.R.layout.simple_list_item_single_choice,
                mCurrentUserRef.child("cars")
        ) {
            @Override
            protected void populateView(View v, Car model, int position) {
                ((TextView) v.findViewById(android.R.id.text1)).setText(model.toString());
                if (TextUtils.equals(getRef(position).getKey(), mCurrentCar)) {
                    mCarsListView.clearChoices();
                    mCarsListView.setItemChecked(position, true);
                    notifyDataSetChanged();
                }
            }
        };

        mCarsListView.setAdapter(adapter);
    }

    @Override
    public void onItemAdded(Car car) {
        mNoCarsText.setVisibility(View.GONE);
        String key = mCurrentUserRef.child("cars").push().getKey();
        car.setId(key);
        mCurrentUserRef.child("cars").child(key).setValue(car);
        mCurrentUserRef.child("current-car").setValue(key);
        updateCurrentCar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        updateCurrentCar();
    }

    @Override
    public void onPause() {
        Car car = (Car) mCarsListView.getItemAtPosition(mCarsListView.getCheckedItemPosition());
        if(car != null )
            mCurrentUserRef.child("current-car").setValue(car.getId());
        super.onPause();
    }
}
