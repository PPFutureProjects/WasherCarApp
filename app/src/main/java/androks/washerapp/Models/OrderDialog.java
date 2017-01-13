package androks.washerapp.Models;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androks.washerapp.Fragments.BaseFragment;
import androks.washerapp.R;

/**
 * Created by androks on 12/15/2016.
 */

public class OrderDialog extends AppCompatDialogFragment implements View.OnClickListener {

    public interface OrderToWashListener {
        void onOrder(Order order);
    }

    private Washer washer;
    private Car car;
    private TextView carTV;
    private TextView washerTV;
    private TextView priceTV;
    private TextView locationTV;
    private View mOrderForm;

    public OrderDialog() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.order_dialog, container, false);
        Dialog dialog = getDialog();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setTitle("Order");

        washerTV = (TextView) view.findViewById(R.id.order_washer_name);
        priceTV = (TextView) view.findViewById(R.id.order_price);
        carTV = (TextView) view.findViewById(R.id.order_car);
        locationTV = (TextView) view.findViewById(R.id.order_washer_location);
        mOrderForm = view.findViewById(R.id.order_form);
        mOrderForm.setVisibility(View.GONE);

        view.findViewById(R.id.order_confirm).setOnClickListener(this);
        view.findViewById(R.id.order_cancel).setOnClickListener(this);

        Bundle bundle = getArguments();
        if (bundle.getString("current-washer-id") != null)
            FirebaseDatabase.getInstance().getReference()
                    .child("washers")
                    .child(bundle.getString("current-washer-id"))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            washer = dataSnapshot.getValue(Washer.class);
                            if (washer == null)
                                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

        final DatabaseReference mUserRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(((BaseFragment) getTargetFragment()).getCurrentUser().getUid());


        mUserRef.child("current-car").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String carId = dataSnapshot.getValue(String.class);
                        if (carId != null)
                            mUserRef.child("cars").child(carId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    car = dataSnapshot.getValue(Car.class);
                                    inflateOrderList();
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


        return view;
    }

    public void inflateOrderList(){
        washerTV.setText(washer.getName());
        locationTV.setText(String.valueOf(washer.getLangtitude()));
        carTV.setText(car.toString());
        priceTV.setText("110");
        mOrderForm.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStart() {
        getDialog().getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        super.onStart();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.order_cancel:
                this.dismiss();
                break;

            case R.id.order_confirm:
                ( (OrderToWashListener) getTargetFragment() ).onOrder(new Order(
                        washer, car, ((BaseFragment) getTargetFragment()).getCurrentUser(), 110
                ));
                this.dismiss();
                break;
        }
    }
}
