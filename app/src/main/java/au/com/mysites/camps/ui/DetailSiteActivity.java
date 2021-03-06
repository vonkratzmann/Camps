package au.com.mysites.camps.ui;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Collections;
import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.adapter.CommentAdapter;
import au.com.mysites.camps.fragments.CommentDialogFragment;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.DividerItemDecoration;
import au.com.mysites.camps.util.UtilDatabase;
import au.com.mysites.camps.util.UtilMap;
import au.com.mysites.camps.viewmodel.DetailSiteViewModel;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

import static android.widget.RelativeLayout.CENTER_VERTICAL;
import static android.widget.Toast.makeText;

/**
 * Displays the detail for a single site with comments for that site.
 * The toolbar provides the option to edit the site, or display a map with a marker
 * for the site location.
 */
public class DetailSiteActivity extends AppCompatActivity implements View.OnClickListener,
        EventListener<DocumentSnapshot>, CommentDialogFragment.CommentListener {

    private static final String TAG = DetailSiteActivity.class.getSimpleName();

    private ImageView mPhotoView;
    private TextView mNumRatingsView;
    private TextView mStreetView;
    private TextView mCityView;
    private TextView mPostcodeView;
    private TextView mStateView;
    private Group mEmptyView;
    private TextView mLatitudeView;
    private TextView mLongitudeView;

    MaterialRatingBar mRatingIndicator;
    private RecyclerView mCommentsRecyclerView;

    private CommentDialogFragment mCommentDialog;

    private FirebaseFirestore mFirestore;

    private ListenerRegistration mSiteRegistration;

    private CommentAdapter mCommentAdapter;

    //used to display the facilities
    private LinearLayout mFacilityIconLinearLayout;
    private LinearLayout.LayoutParams mLayoutParams;

    // Holds the site loaded from the snapshot
    private Site mSite;

    // Id of the site (same as the name of the site) loaded from the calling Intent
    private String mSiteId;

    private DetailSiteViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_site_detail);

        // Get site ID from extras
        mSiteId = Objects.requireNonNull(getIntent().getExtras()).getString(getString(R.string.intent_site_name));
        if (mSiteId == null) {
            throw new IllegalArgumentException("Must pass extra " + getString(R.string.intent_site_name));
        }
        Toolbar toolbar = findViewById(R.id.site_detail_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(mSiteId);

        initViews();

        // View models
        mViewModel = ViewModelProviders.of(this).get(DetailSiteViewModel.class);

        // Initialize Firestore, views and listeners
        mFirestore = FirebaseFirestore.getInstance();

        // Build a query for comments for this site
        CollectionReference commentRef = mFirestore.collection(getString(R.string.collection_comments));
        // Find all comments with the siteId of this site
        Query query = commentRef.whereEqualTo(Comment.FIELD_SITEID, mSiteId);

        // RecyclerView
        mCommentAdapter = new CommentAdapter(query) {
            @Override
            protected void onDataChanged() {
                if (getItemCount() == 0) {
                    mCommentsRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mCommentsRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }
        };
        mCommentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Add a line between each item in the recycler view
        mCommentsRecyclerView.addItemDecoration(new
                DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        mCommentsRecyclerView.setAdapter(mCommentAdapter);
    }

    /**
     * Initialise the views and attach listeners
     */
    void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "initViews()");

        mPhotoView = findViewById(R.id.site_detail_photo);
        mNumRatingsView = findViewById(R.id.site_num_ratings);
        mStreetView = findViewById(R.id.site_detail_street);
        mCityView = findViewById(R.id.site_detail_city);
        mPostcodeView = findViewById(R.id.site_detail_postcode);
        mStateView = findViewById(R.id.site_detail_state);
        mLatitudeView = findViewById(R.id.site_detail_map_coordinates_lat);
        mLongitudeView = findViewById(R.id.site_detail_map_coordinates_long);

        mEmptyView = findViewById(R.id.site_details_no_comments_group);
        mRatingIndicator = findViewById(R.id.site_detail_rating);
        mCommentsRecyclerView = findViewById(R.id.site_detail_recycler_comments);

        //Create instance of the dialog fragment
        mCommentDialog = new CommentDialogFragment();

        FloatingActionButton mShowCommentDialog = findViewById(R.id.fab_show_comment_dialog);
        mShowCommentDialog.setOnClickListener(this);

        /* Note, display of facilities not done here. These are handled differently,
         * these are displayed using the snapshot returned from the Firestore database
         * call in {@link #onStart()}. }
         */
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onResume()");
    }

    /**
     * Implement snapshot listener, the initial call of the callback
     * {@link #onEvent(DocumentSnapshot, FirebaseFirestoreException)}
     * as a result of using addSnapshotListener() immediately creates a document snapshot with the
     * current contents of the single document.
     * Then, each time the contents change, another call updates the document snapshot.
     * Results are returned in the overridden {@link #onEvent} method.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onStart()");

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn();
            return;
        }
        // Start listening for Firestore updates
        if (mCommentAdapter != null) {
            mCommentAdapter.startListening();
        }

        // Get reference to this site
        DocumentReference docRef = mFirestore
                .collection(getString(R.string.collection_sites))
                .document(mSiteId);
        // The site data is returned in onEvent()
        mSiteRegistration = docRef.addSnapshotListener(this);
    }

    /**
     *
     */
    @Override
    public void onStop() {
        super.onStop();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onStop()");

        if (mCommentAdapter != null) {
            mCommentAdapter.stopListening();
        }
        if (mSiteRegistration != null) {
            mSiteRegistration.remove();
            mSiteRegistration = null;
        }
    }

    /*
     *
     */
    @Override
    public void onBackPressed() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onBackPressed()");

        finish();
    }

    private boolean shouldStartSignIn() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "shouldStartSignIn()");

        return (!mViewModel.getIsSigningIn() && FirebaseAuth.getInstance().getCurrentUser() == null);
    }

    private void startSignIn() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "startSignIn()");

        // Sign in with FirebaseUI
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build()))
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(intent, Constants.RC_SIGN_IN);
        mViewModel.setIsSigningIn(true);
    }

    /**
     * Provides realtime updates with Cloud Firestore.
     * An initial call of this callback after using addSnapshopListener() immediately creates
     * a document snapshot with the current contents of the single document.
     * Then, each time the contents change, another call updates the document snapshot.
     */
    @Override
    public void onEvent(@Nullable DocumentSnapshot snapshot,
                        @Nullable FirebaseFirestoreException e) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onEvent()");

        if (e != null) {
            Log.w(TAG, "site:onEvent", e);
            return;
        }
        if (snapshot != null && snapshot.exists()) {  // Process the data loaded from the snapshot
            onSiteLoaded(Objects.requireNonNull(snapshot.toObject(Site.class)));
        }
    }

    /**
     * Using the site loaded from the database, update the views on the UI.
     * Called from {@link #onEvent(DocumentSnapshot, FirebaseFirestoreException)}
     *
     * @param site site that has been loaded
     */
    private void onSiteLoaded(Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onSiteLoaded()");

        mSite = site;
        mStreetView.setText(site.getStreet());
        mCityView.setText(site.getCity());
        mPostcodeView.setText(site.getPostcode());
        mStateView.setText(site.getState());
        mLatitudeView.setText(site.getLatitude());
        mLongitudeView.setText(site.getLongitude());

        mRatingIndicator.setRating((float) site.getAvgRating());
//        mNumRatingsView.setText(getString(R.string.fmt_num_ratings, site.getNumRatings()));

        // only process if site photo is valid
        if (site.getSitePhoto() != null && !site.getSitePhoto().isEmpty()) {
            UtilDatabase.getImageAndDisplay(this, site.getSitePhoto(), mPhotoView);
        }
        //displays only those facilities present at the site
        displayFacilities(site, this);
    }

    /**
     * When the Floating Action Button (FAB) is pressed to add a new comment,
     * the CommentDialogFragment receives a reference to this Activity,
     * overrides the {@link CommentDialogFragment#onAttach(Context)}
     * which instantiate the CommentListener callback. When the submit button in the fragment
     * is pressed, the fragment via listener sends the comment to this method.
     * <p>>
     * This method then saves the comment to the database in the collection comments.
     *
     * @param comment comment from the dialog fragment
     */

    @Override
    public void onComment(Comment comment) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onComment()");

        // Add site id for this comment
        comment.setSiteId(mSiteId);

        mFirestore.collection(getString(R.string.collection_comments))
                .add(comment) // Firebase automatically generates a comment Id
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Hide keyboard and scroll to top
                        hideKeyboard();
                        mCommentsRecyclerView.smoothScrollToPosition(0);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Add comment failed", e);

                        // Show failure message and hide keyboard
                        hideKeyboard();
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.ERROR_Comment_not_saved),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void hideKeyboard() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "hideKeyboard()");

        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) Objects.requireNonNull(getSystemService(Context.INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Shows facilities that are present at the site
     * sets up listener for each site that shows a brief description of the facility
     * when the facility is touched.
     *
     * @param site    site to store data
     * @param context context of calling activity
     */
    public void displayFacilities(Site site, final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "displayFacilities");

        //get the view and set up new linear layout
        mFacilityIconLinearLayout = ((Activity) context).findViewById(R.id.site_detail_facility_icons);
        mLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // Ensure layout is empty as there may have been changes if we were editing this site.
        if ((mFacilityIconLinearLayout).getChildCount() > 0)
            (mFacilityIconLinearLayout).removeAllViews();

        //configure layout
        mLayoutParams.setMargins(2, 2, 2, 2);
        mLayoutParams.gravity = Gravity.START | CENTER_VERTICAL;

        if (site.checkIfFacilityPresent(Site.Facility.DUMPPOINT)) {
            addFacilityImage(R.mipmap.dumppoint, context.getString(R.string.dumppoint), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.FREE)) {
            addFacilityImage(R.mipmap.free, context.getString(R.string.free), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.MOBILE)) {
            addFacilityImage(R.mipmap.mobile, context.getString(R.string.mobile), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.PLAYEQUIPMENT)) {
            addFacilityImage(R.mipmap.playequipment, context.getString(R.string.playequipment), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.SCENIC)) {
            addFacilityImage(R.mipmap.scenic, context.getString(R.string.scenic), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.SHOWERS)) {
            addFacilityImage(R.mipmap.shower, context.getString(R.string.showers), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.SWIMMING)) {
            addFacilityImage(R.mipmap.swimming, context.getString(R.string.swimming), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.TOILETS)) {
            addFacilityImage(R.mipmap.toilets, context.getString(R.string.toilets), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.TVRECEPTION)) {
            addFacilityImage(R.mipmap.tvreception, context.getString(R.string.tvreception), context);
        }
        if (site.checkIfFacilityPresent(Site.Facility.WATER)) {
            addFacilityImage(R.mipmap.water, context.getString(R.string.water), context);
        }
    }

    /**
     * Add image for the facility to the layout
     *
     * @param imageResource image to be added to the layout
     * @param string        message to be displayed when image is clicked
     * @param context       context of calling method
     */
    private void addFacilityImage(int imageResource, final String string, Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "addFacilityImage()");

        ImageView image1 = new ImageView((context));
        image1.setLayoutParams(mLayoutParams);
        image1.setAdjustViewBounds(true);
        image1.setImageResource(imageResource);
        image1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), string
                        , Toast.LENGTH_SHORT).show();
            }
        });
        mFacilityIconLinearLayout.addView(image1);
    }

    /**
     * Inflates the menu
     *
     * @param menu menu
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onCreateOptionsMenu()");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail_site, menu);

        /* Return true so that the visualizer_menu is displayed in the Toolbar */
        return true;
    }

    /**
     * Respond to user interaction on menu
     *
     * @param item selected item
     * @return item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onOptionsItemSelected()");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_detail_edit_site:
                Intent editSite = new Intent(DetailSiteActivity.this, AddOrEditSiteActivity.class);
                editSite.putExtra(getString(R.string.intent_site_name), mSite.getName());
                startActivity(editSite);
                break;

            case R.id.menu_detail_show_map:
                // Checks latitude and longitude are set,
                if (UtilMap.checkLatLongSet(mSite)) {
                    // Ok, display map with a marker at this site's location by creating an intent
                    UtilMap.mapShow(mSite, this);
                } else {
                    //warn the user not valid
                    makeText(this, getString(R.string.ERROR_GPS_coordinates),
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles onClick Events from the buttons
     *
     * @param v View
     */
    @Override
    public void onClick(View v) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onClick()");

        switch (v.getId()) {

            case R.id.fab_show_comment_dialog:
                //show dialog to enter a new comment
                mCommentDialog.show(getSupportFragmentManager(), CommentDialogFragment.TAG);
                break;

            default:
                break;
        }
    }
}

