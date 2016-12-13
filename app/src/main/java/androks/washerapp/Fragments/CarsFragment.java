package androks.washerapp.Fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    private DatabaseReference mCarsRef;

    private ListView mCarsListView;

    private ProgressBar mProgressBar;

    public CarsFragment() {
        // Required empty public constructor
    }
    View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_cars, container, false);

        mCarsListView = (ListView) rootView.findViewById(R.id.cars_list_view);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.loading_cars_indicator);
        rootView.findViewById(R.id.add_car_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppCompatDialogFragment addCarDialog = new AddCarDialog();
                addCarDialog.setTargetFragment(CarsFragment.this, REQUEST_WEIGHT);
                addCarDialog.show(getFragmentManager(), "Add car");
            }
        });

        mCarsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if(getCurrentUser() == null)
            startActivityForResult(new Intent(getActivity(), LoginActivity.class), 101);
        return rootView;
    }

    @Override
    public void onStart() {

        mCarsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(getCurrentUser().getUid())
                .child("cars");


        FirebaseListAdapter<Car> adapter  = new FirebaseListAdapter<Car>(
                getActivity(),
                Car.class,
                android.R.layout.simple_list_item_single_choice,
                mCarsRef
        ) {
            @Override
            protected void populateView(View v, Car model, int position) {
                ((TextView)v.findViewById(android.R.id.text1)).setText(model.toString());
                mProgressBar.setVisibility(View.GONE);
            }
        };

        mCarsListView.setAdapter(adapter);

        super.onStart();
    }

    @Override
    public void onItemAdded(Car car) {
        mCarsRef.push().setValue(car);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {return;}
        this.onStart();
    }
}
