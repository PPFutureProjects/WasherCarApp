package androks.washerapp.Models;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androks.washerapp.R;

/**
 * Created by androks on 12/11/2016.
 */

public class AddCarDialog extends AppCompatDialogFragment implements  View.OnClickListener{

    public interface AddCarDialogListener {
        void onItemAdded(Car car);
    }

    private String mCurrentMake;
    private HashMap<String, List<String>> mCarList = new HashMap<>();

    private EditText mCarNumber;
    private AutoCompleteTextView mCarMaker;
    private AutoCompleteTextView mCarModel;
    private View mAddCarForm;
    private View mProgressBar;
    private Button mConfirmButton;

    public AddCarDialog() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.car_dialog, container, false);
        Dialog dialog = getDialog();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);


        mCarNumber = (EditText) view.findViewById(R.id.car_number);
        mCarMaker = (AutoCompleteTextView) view.findViewById(R.id.car_maker);
        mCarModel = (AutoCompleteTextView) view.findViewById(R.id.car_model);
        mCarMaker.setThreshold(0);
        mCarModel.setThreshold(0);
        mAddCarForm = view.findViewById(R.id.add_car_form);
        mAddCarForm.setVisibility(View.GONE);
        mProgressBar = view.findViewById(R.id.loading_car_lists);
        mProgressBar.setVisibility(View.VISIBLE);
        mConfirmButton = (Button) view.findViewById(R.id.car_confirm);
        mConfirmButton.setOnClickListener(this);

        mCarMaker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mCarMaker.setText("");
                mCarModel.setText("");
                mCarModel.setEnabled(false);
                mCarNumber.setEnabled(false);
                mConfirmButton.setEnabled(false);
                return false;
            }
        });
        mCarMaker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mCarModel.setEnabled(true);
                mCarModel.setAdapter(new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_list_item_1,
                        mCarList.get(mCarMaker.getText().toString())));
                mCarModel.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });
        mCarModel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mCarModel.setText("");
                mCarNumber.setText("");
                mCarNumber.setEnabled(false);
                mConfirmButton.setEnabled(false);
                return false;
            }
        });

        mCarModel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mCarNumber.setEnabled(true);
                mCarNumber.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        mCarNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length()>4)
                    mConfirmButton.setEnabled(true);
                else
                    mConfirmButton.setEnabled(false);
            }
        });




        FirebaseDatabase.getInstance().getReference().child("cars").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> makers = new ArrayList<>();
                List<String> models;
                for(DataSnapshot child: dataSnapshot.getChildren()){
                    makers.add(child.getKey());

                    models = Arrays.asList(child.getValue(String.class).split(","));

                    mCarList.put(child.getKey(), models);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>
                        (getActivity(), android.R.layout.simple_list_item_1, makers);
                mCarMaker.setAdapter(adapter);

                mProgressBar.setVisibility(View.GONE);
                mAddCarForm.setVisibility(View.VISIBLE);

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Show soft keyboard automatically
        mCarMaker.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onClick(View view) {

        //First letter of maker to uppercase
        String maker = String.valueOf(mCarMaker.getText().toString().toUpperCase().charAt(0));
        maker += mCarMaker.getText().toString().substring(1, mCarMaker.getText().length());

        // Return input text to activity
        ( (AddCarDialogListener) getTargetFragment() ).onItemAdded(new Car(
                maker,
                mCarModel.getText().toString(),
                mCarNumber.getText().toString().toUpperCase()
        ));
        this.dismiss();
    }
}
