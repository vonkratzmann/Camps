package au.com.mysites.camps.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.User;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.UtilDatabase;
import au.com.mysites.camps.util.UtilDialog;
import au.com.mysites.camps.util.UtilFile;
import au.com.mysites.camps.util.UtilGeneral;
import au.com.mysites.camps.util.UtilImage;

/**
 * Firebase Authentication using a Google ID Token.
 * Whenever there is a new sign in, saves the users profile information in the database under
 * the collection users with the user google identification string as the document name. The user
 * profile picture is save to firebase storage with the name of the photo stored in the database.
 * <p>
 * Each time the application is started the last used date for the user is updated
 * in the Firestore database.
 * <p>
 * The application checks if there has been a request to sign out from the SummarySitesActivities,
 * if there is, then stay in this application and display the user sign out buttons. If there
 * has not been a request to sign out and if the user is logged in,
 * start the SummarySitesActivities.
 */

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAuth mAuth;

    private GoogleSignInClient mGoogleSignInClient;
    private TextView mStatusTextView;
    private TextView mDetailTextView;

    private ImageView mProfilePhotoImageView;

    private ProgressDialog mProgressDialog;

    private File mFile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);

        // Views
        mStatusTextView = findViewById(R.id.main_status);
        mDetailTextView = findViewById(R.id.main_detail);
        mProfilePhotoImageView = findViewById(R.id.main_background_imageView);

        // Progress Dialog
        mProgressDialog = new ProgressDialog(this);

        // Button listeners
        findViewById(R.id.main_sign_in_button).setOnClickListener(this);
        findViewById(R.id.main_sign_out_button).setOnClickListener(this);
        findViewById(R.id.main_disconnect_button).setOnClickListener(this);
        findViewById(R.id.main_start_app).setOnClickListener(this);

        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
    }

    /**
     * Check if a users is signed in and update the display to show appropriate buttons.
     * If a user is signed in, updates the user last used date.
     * If called by SummarySitesActivity and if there was a request to sign out,
     * stay in this activity so user can use the sign out buttons,
     * otherwise the app has just started. If no request to sign out and the user is signed in
     * then start the SummarySitesActivity to display a list of sites.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onStart()");

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        /* Displays the appropriate buttons on the UI,
         * ie user is presented with sign in or sign out buttons. */
        updateUI(currentUser);

        Intent intent = getIntent();
        boolean signOutRequest;
        if (intent != null) {
            signOutRequest = intent.getBooleanExtra(getString(R.string.intent_sign_out), false);

            if (!signOutRequest && currentUser != null) {
                //Not a request to sign out, and if signed in, go to summary site activity
                Intent SummarySite = new Intent(MainActivity.this, SummarySitesActivity.class);
                //kk            startActivity(SummarySite);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        UtilDialog.hideProgressDialog(mProgressDialog);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onActivityResult()");


        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == Constants.RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
                // Save the user profile in Firestore database
                updateUserProfile();
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // [START_EXCLUDE]
                updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        UtilDialog.showProgressDialog(mProgressDialog);
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        UtilDialog.hideProgressDialog(mProgressDialog);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]

    /**
     * Saves user profile information to Firestore database in case it has changed.
     * Saves user profile photo to Firebase storage, and saves a copy
     * to local storage, so that next time the photo can be fetched from local storage rather than
     * the remote database.
     */
    private void updateUserProfile() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "updateUserProfile()");

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct == null) return;

        // Extract user information
        User mGoogleUser = retrieveProfile(acct);

        String mUserId = acct.getId();
        Uri photoUri = acct.getPhotoUrl();

        if (photoUri != null) {
            String string = photoUri.toString();
            /* Display the profile photo. If the user is logged in and if the SummarySitesActivity
             * is started the photo will not been seen */
            Glide.with(MainActivity.this).load(photoUri).into(mProfilePhotoImageView);

            // Start AsyncTask and implement onPostExecute Method
            new AsyncTaskGetUri().execute(string);

            // Saves user information to database
            saveUserToFirestore(mUserId, mGoogleUser);
        }
    }

    /**
     * Save copy of user profile information to Firestore database
     *
     * @param id   User profile id, used as document reference
     * @param user user information to be saved
     */
    private void saveUserToFirestore(String id, User user) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "saveUserToFirestore()");

        // Initialize Firestore
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
        // User Id is used as the document reference
        assert id != null;

        mFirestore.collection(getString(R.string.firebase_collection_users))
                .document(id)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (Debug.DEBUG_METHOD_ENTRY)
                            Log.d(TAG, "updateUserProfile() document successfully written");
                        //   Now save the photo to firebase storage
                        if (mFile != null)
                            UtilDatabase.saveFileFirestore(MainActivity.this, mFile,
                                    getString(R.string.firebase_collection_users));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error while saving user: " + e);
                    }
                });
    }

    /**
     * Retrieves signed-in user profile information.
     *
     * @param acct Contains profile information for the user
     * @return User with profile information
     */
    private User retrieveProfile(GoogleSignInAccount acct) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "retrieveProfile()");

        String displayName = acct.getDisplayName();
        String givenName = acct.getGivenName();
        String familyName = acct.getFamilyName();
        String email = acct.getEmail();
        String lastUsed = UtilGeneral.getTodaysDate(getString(R.string.dateformat));
        String photoFileName = null;

        // Create a unique file name for the photo
        try {
            mFile = UtilImage.createImageFile(this);
        } catch (IOException ioe) {
            Log.e(TAG, "updateUserProfile() IOException");
        }
        if (mFile != null)
            photoFileName = mFile.getName();

        return new User(displayName, givenName, familyName, email, photoFileName, lastUsed);
    }


    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN);
    }

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    /**
     * Display appropriate sign in or sign out buttons on the UI
     *
     * @param user      Current logged in user
     */
    private void updateUI(FirebaseUser user) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "updateUI()");

        UtilDialog.hideProgressDialog(mProgressDialog);
        if (user != null) {
            mStatusTextView.setText(getString(R.string.google_status_fmt, user.getEmail()));
            mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));

            findViewById(R.id.main_sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.main_sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            mDetailTextView.setText(null);

            findViewById(R.id.main_sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.main_sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    /*
     * Dialog to prompt user for yes/no if they want to exit the activity.
     */
    @Override
    public void onBackPressed() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onBackPressed()");

        new AlertDialog.Builder(this)
                .setIcon(R.mipmap.warning)
                .setTitle(getString(R.string.exit))
                .setMessage(getString(R.string.sure))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    @Override
    public void onClick(View v) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onClick()");

        int i = v.getId();
        if (i == R.id.main_sign_in_button) {
            signIn();
        } else if (i == R.id.main_sign_out_button) {
            signOut();
        } else if (i == R.id.main_start_app) {
            Intent startApp = new Intent(MainActivity.this, SummarySitesActivity.class);
            startActivity(startApp);
        } else if (i == R.id.main_disconnect_button) {
            revokeAccess();
        }
    }

    /**
     * fetches a file from the web, copies the result to local storage
     */
    @SuppressLint("StaticFieldLeak")
    private class AsyncTaskGetUri extends AsyncTask<String, Void, File> {
        private final String TAG = AsyncTaskGetUri.class.getSimpleName();

        @Override
        protected File doInBackground(String... string) {
            if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "doInBackground()");

            File file = null;
            try {
                file = Glide.with(MainActivity.this)
                        .load(string[0])// Get path to the file
                        .downloadOnly(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get();

            } catch (InterruptedException | ExecutionException e) {
                Log.d(TAG, "copyFile error: " + e);
            }
            return file;
        }

        /**
         * save file to local storage
         *
         * @param result file copied from the web
         */
        @Override
        protected void onPostExecute(File result) {
            if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onPostExecute()");

            try {
                UtilFile.copyFile(result, mFile);
            } catch (IOException e) {
                Log.w(TAG, "Error while copying file: " + e);
            }
        }
    }
}


