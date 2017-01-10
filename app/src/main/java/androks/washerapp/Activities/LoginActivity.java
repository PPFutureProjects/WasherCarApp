package androks.washerapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.github.pinball83.maskededittext.MaskedEditText;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androks.washerapp.Models.Car;
import androks.washerapp.R;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements OnClickListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;

    private GoogleApiClient mGoogleApiClient;

    // UI references.
    private MaskedEditText mPhoneView;
    private ProgressBar mProgressView;
    private View mPhoneForm;
    private View mTryAgainForm;
    private View mCarForm;
    private Button mConfirmCar;
    private Button mConfirmPhone;
    private FirebaseUser mCurrentUser;

    private HashMap<String, List<String>> mCarList = new HashMap<>();

    private EditText mCarNumber;
    private AutoCompleteTextView mCarMaker;
    private AutoCompleteTextView mCarModel;


    private boolean isRegistrationSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        isRegistrationSuccess = false;
        // Set up the login form.
        mPhoneView = (MaskedEditText) findViewById(R.id.login_phone);
        mProgressView = (ProgressBar) findViewById(R.id.login_progress);
        mPhoneForm = findViewById(R.id.login_phone_form);
        mTryAgainForm = findViewById(R.id.try_login_again_form);
        mCarForm = findViewById(R.id.login_car_form);

        mCarNumber = (EditText) findViewById(R.id.car_number);
        mCarMaker = (AutoCompleteTextView) findViewById(R.id.car_maker);
        mCarModel = (AutoCompleteTextView) findViewById(R.id.car_model);
        mCarMaker.setThreshold(0);
        mCarModel.setThreshold(0);

        mConfirmCar = (Button) findViewById(R.id.car_confirm);
        mConfirmCar.setOnClickListener(this);
        mConfirmPhone = (Button) findViewById(R.id.phone_confirm);
        mConfirmPhone.setOnClickListener(this);
        (findViewById(R.id.try_again_btn)).setOnClickListener(this);

        mCarMaker.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mCarMaker.setText("");
                mCarModel.setText("");
                mCarModel.setEnabled(false);
                mCarNumber.setEnabled(false);
                mConfirmCar.setEnabled(false);
                return false;
            }
        });
        mCarMaker.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mCarModel.setEnabled(true);
                mCarModel.setAdapter(new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_list_item_1,
                        mCarList.get(mCarMaker.getText().toString())));
                mCarModel.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });
        mCarModel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mCarModel.setText("");
                mCarNumber.setText("");
                mCarNumber.setEnabled(false);
                mConfirmCar.setEnabled(false);
                return false;
            }
        });

        mCarModel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mCarNumber.setEnabled(true);
                mCarNumber.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
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
                if (editable.length() > 4)
                    mConfirmCar.setEnabled(true);
                else
                    mConfirmCar.setEnabled(false);
            }
        });


        FirebaseDatabase.getInstance().getReference().child("cars").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> makers = new ArrayList<>();
                List<String> models;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    makers.add(child.getKey());

                    models = Arrays.asList(child.getValue(String.class).split(","));

                    mCarList.put(child.getKey(), models);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>
                        (LoginActivity.this, android.R.layout.simple_list_item_1, makers);
                mCarMaker.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        signIn();
        showProgressDialog();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                showProgressDialog();
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                mTryAgainForm.setVisibility(View.VISIBLE);
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            mTryAgainForm.setVisibility(View.VISIBLE);
                        } else {
                            mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("users");
                            rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.hasChild(mCurrentUser.getUid())) {
                                        userLoggedIn();
                                    } else {
                                        mPhoneForm.setVisibility(View.VISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                        }

                        hideProgressDialog();
                    }
                });
    }

    private void hideProgressDialog() {
        mProgressView.setVisibility(View.GONE);
    }

    private void showProgressDialog() {
        mProgressView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again_btn:
                mTryAgainForm.setVisibility(View.GONE);
                signIn();
                break;
            case R.id.phone_confirm:
                if(mPhoneView.getUnmaskedText().length() != 9) {
                    mPhoneView.setError("Required");
                    return;
                }
                mPhoneForm.setVisibility(View.GONE);
                mCarForm.setVisibility(View.VISIBLE);
                break;

            case R.id.car_confirm:
                writeDataToDB();
                isRegistrationSuccess = true;
                userLoggedIn();
                break;
        }
    }

    private void writeDataToDB() {
        DatabaseReference mCurrentUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(mCurrentUser.getUid());
        mCurrentUserRef.child("email")
                .setValue(mCurrentUser.getEmail());
        mCurrentUserRef.child("email")
                .child("phone")
                .setValue(mPhoneView.getText().toString());

        //First letter of maker to uppercase
        String maker = String.valueOf(mCarMaker.getText().toString().toUpperCase().charAt(0));
        maker += mCarMaker.getText().toString().substring(1, mCarMaker.getText().length());

        Car car = new Car(maker,
                mCarModel.getText().toString(),
                mCarNumber.getText().toString().toUpperCase());

        String key = mCurrentUserRef.child("cars").push().getKey();
        car.setId(key);
        mCurrentUserRef.child("cars").child(key).setValue(car);
        mCurrentUserRef.child("current-car").setValue(key);
    }

    private void userLoggedIn() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        isRegistrationSuccess = true;
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isRegistrationSuccess && mGoogleApiClient.isConnected()) {
            // Firebase sign out
            mAuth.signOut();
            // Google sign out
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

