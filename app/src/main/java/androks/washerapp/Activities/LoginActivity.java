package androks.washerapp.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androks.washerapp.Models.User;
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
    private Button mConfirmBtn;
    private ProgressBar mProgressView;
    private View mPhoneForm;
    private View mTryAgainForm;
    private String mCurrentUserId;
    private User mCurrentUser;


    private boolean isRegistrationSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        isRegistrationSuccess = false;

        // Set up the login form.
        mPhoneView = (MaskedEditText) findViewById(R.id.phone);
        mConfirmBtn = (Button) findViewById(R.id.phone_confirm);
        mProgressView = (ProgressBar) findViewById(R.id.login_progress);
        mPhoneForm = findViewById(R.id.phone_form);
        mTryAgainForm = findViewById(R.id.try_again_form);

        (findViewById(R.id.phone_confirm)).setOnClickListener(this);
        (findViewById(R.id.try_again_btn)).setOnClickListener(this);


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
        if(!mGoogleApiClient.isConnected())
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
                        }else{
                            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("users");
                            rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (snapshot.hasChild(acct.getId())) {
                                        userLoggedIn();
                                    }else{
                                        mPhoneForm.setVisibility(View.VISIBLE);
                                        mCurrentUserId = acct.getId();
                                        mCurrentUser = new User(acct.getEmail());
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
        switch (view.getId()){
            case R.id.try_again_btn:
                mTryAgainForm.setVisibility(View.GONE);
                signIn();
                break;
            case R.id.phone_confirm:
                if(mPhoneView.getUnmaskedText().length() != 9) {
                    mPhoneView.setError("Required");
                    return;
                }
                writeUserToDB();
                isRegistrationSuccess = true;
                userLoggedIn();
                break;
        }
    }

    private void userLoggedIn(){
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isRegistrationSuccess && mGoogleApiClient.isConnected()){
            // Firebase sign out
            mAuth.signOut();
            // Google sign out
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        }
    }

    private void writeUserToDB() {
        mCurrentUser.setPhone(mPhoneView.getText().toString());
        FirebaseDatabase.getInstance().getReference().child("users").child(mCurrentUserId).setValue(mCurrentUser);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

