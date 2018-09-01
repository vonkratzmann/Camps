package au.com.mysites.camps.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.adapter.RatingAdapter;
import au.com.mysites.camps.models.Rating;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.fragments.RatingDialogFragment;
import au.com.mysites.camps.util.Debug;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class RatingDetailActivity extends AppCompatActivity
        implements EventListener<DocumentSnapshot>, RatingDialogFragment.RatingListener {

    private static final String TAG = RatingDetailActivity.class.getSimpleName();

    ImageView mImageView;
    TextView mNameView;
    MaterialRatingBar mRatingIndicator;
    TextView mNumRatingsView;
    TextView mStreetView;
    TextView mCityView;
    TextView mStateView;
    ViewGroup mEmptyView;
    RecyclerView mRatingsRecycler;

    FloatingActionButton mShowRatingDialog;
    ImageView mSiteButtonBack;

    private RatingDialogFragment mRatingDialog;

    private FirebaseFirestore mFirestore;
    private DocumentReference mSiteRef;
    private ListenerRegistration mSiteRegistration;

    private RatingAdapter mRatingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_rating_detail);

        // Get site ID from extras
        String siteId = Objects.requireNonNull(getIntent().getExtras()).getString(getString(R.string.pref_key_site_id));
        if (siteId == null) {
            throw new IllegalArgumentException("Must pass extra " + getString(R.string.pref_key_site_id));
        }

        initViews();
        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance();

        // Get reference to the site
        mSiteRef = mFirestore.collection("sites").document(siteId);

        // Get ratings
        Query ratingsQuery = mSiteRef
                .collection("ratings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50);

        // RecyclerView
        mRatingAdapter = new RatingAdapter(ratingsQuery) {
            @Override
            protected void onDataChanged() {
                if (getItemCount() == 0) {
                    mRatingsRecycler.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mRatingsRecycler.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }
        };

        mRatingsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRatingsRecycler.setAdapter(mRatingAdapter);

    }

    void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "initViews()");

        mImageView = findViewById(R.id.site_image);

        mNameView = findViewById(R.id.site_name);
        mRatingIndicator = findViewById(R.id.site_detail_rating);
        mNumRatingsView = findViewById(R.id.site_num_ratings);
        mStreetView = findViewById(R.id.site_street);
        mCityView = findViewById(R.id.site_city);
        mStateView = findViewById(R.id.site_state);
        mEmptyView = findViewById(R.id.view_empty_ratings);

        mRatingsRecycler = findViewById(R.id.recycler_ratings);

        mSiteButtonBack = findViewById(R.id.site_detail_button_back);
        mSiteButtonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mShowRatingDialog = findViewById(R.id.fab_show_rating_dialog);

        //Create instance of the dialog fragment
        mRatingDialog = new RatingDialogFragment();

        //handle FAB button pressed to show rating dialog
        mShowRatingDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the instance of the dialog fragment
                mRatingDialog.show(getSupportFragmentManager(), RatingDialogFragment.TAG);
            }
        });
    }

    public void onBackArrowClicked(View view) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onBackArrowClicked()");

        onBackPressed();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onStart()");

        mRatingAdapter.startListening();
        mSiteRegistration = mSiteRef.addSnapshotListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onStop()");

        mRatingAdapter.stopListening();
        if (mSiteRegistration != null) {
            mSiteRegistration.remove();
            mSiteRegistration = null;
        }
    }

    /**
     * Add a rating to the sub-collection
     *
     * @param siteRef reference to the site
     * @param rating  rating to be added
     * @return null
     */
    private Task<Void> addRating(final DocumentReference siteRef, final Rating rating) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "Task<Void> addRating()");

        // Create reference for new rating, for use inside the transaction
        final DocumentReference ratingRef = siteRef.collection("ratings")
                .document();

        // In a transaction, add the new rating and update the aggregate totals
        return mFirestore.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction)
                    throws FirebaseFirestoreException {

                Site site = transaction.get(siteRef)
                        .toObject(Site.class);

                // Compute new number of ratings
                assert site != null;
                int newNumRatings = site.getNumRatings() + 1;

                // Compute new average rating
                double oldRatingTotal = site.getAvgRating() *
                        site.getNumRatings();
                double newAvgRating = (oldRatingTotal + rating.getRating()) /
                        newNumRatings;

                // Set new site info
                site.setNumRatings(newNumRatings);
                site.setAvgRating(newAvgRating);

                // Commit to Firestore
                transaction.set(siteRef, site);
                transaction.set(ratingRef, rating);

                return null;
            }
        });
    }

    /**
     * Listener for the Site document ({@link #mSiteRef}).
     */
    @Override
    public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onEvent()");

        if (e != null) {
            Log.w(TAG, "site:onEvent", e);
            return;
        }
        onSiteLoaded(Objects.requireNonNull(snapshot.toObject(Site.class)));
    }

    /**
     * From the site loaded from the database, update the views on the UI
     *
     * @param site      site that has been loaded
     */
    private void onSiteLoaded(Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onSiteLoaded()");

        mNameView.setText(site.getName());
        mRatingIndicator.setRating((float) site.getAvgRating());
        mNumRatingsView.setText(getString(R.string.fmt_num_ratings, site.getNumRatings()));
        mStreetView.setText(site.getStreet());
        mCityView.setText(site.getCity());
        mStateView.setText(site.getState());

        // Background image
        Glide.with(mImageView.getContext())
                .load(site.getThumbnail())
                .into(mImageView);
    }

    /**
     * The dialog fragment receives a reference to this Activity through the
     * Fragment.onAttach() callback, which it uses to call the following method
     * defined by the RatingDialogFragment.RatingListener interface
     *
     * @param rating       rating from the dialog fragment
     */
    @Override
    public void onRating(Rating rating) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onRating()");

        // In a transaction, add the new rating and update the aggregate totals
        addRating(mSiteRef, rating)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Rating added");

                        // Hide keyboard and scroll to top
                        hideKeyboard();
                        mRatingsRecycler.smoothScrollToPosition(0);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Add rating failed", e);

                        // Show failure message and hide keyboard
                        hideKeyboard();
                        Snackbar.make(findViewById(android.R.id.content), "Failed to add rating",
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
}

