package au.com.mysites.camps.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import au.com.mysites.camps.R;
import au.com.mysites.camps.adapter.SiteAdapter;
import au.com.mysites.camps.fragments.FilterDialogFragment;
import au.com.mysites.camps.models.Filters;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.DividerItemDecoration;
import au.com.mysites.camps.util.Permissions;
import au.com.mysites.camps.viewmodel.SummarySiteActivityViewModel;

/**
 * Displays a summary list of the sites, with photo, name, address and rating, with ability to
 * select a sort order and filter the sites.
 * By touching a site a more detailed display of the site is provided.
 * From the toolbar a signout option is provided which takes the user to the
 * signin/signout Firebase activity.
 */
public class SummarySitesActivity extends AppCompatActivity implements
        FilterDialogFragment.FilterListener,
        SiteAdapter.OnSiteSelectedListener,
        View.OnClickListener {

    private static final String TAG = SummarySitesActivity.class.getSimpleName();

    Toolbar mToolbar;
    TextView mCurrentSearchView;
    TextView mCurrentSortByView;
    RecyclerView mSitesRecyclerView;
    ViewGroup mEmptyView;
    CardView mFilterBar;
    ImageView mFilterClear;

    public FirebaseFirestore mFirestore;
    private Query mQuery;

    private FilterDialogFragment mFilterDialog;
    private SiteAdapter mAdapter;

    private SummarySiteActivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_summary);
        Toolbar toolbar = findViewById(R.id.site_summary_toolbar);
        setSupportActionBar(toolbar);

        // Check have permission to access external storage
        Permissions permissions = new Permissions();
        permissions.checkPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE);

        initViews();

        // Retain filters information over the activity lifecycle using ViewModel
        mViewModel = ViewModelProviders.of(this).get(SummarySiteActivityViewModel.class);

        // Initialize Firestore and the main RecyclerView
        initFirestore();
        initRecyclerView();

        // Filter Dialog
        mFilterDialog = new FilterDialogFragment();

        //used to start activity to add a new site to the database
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addNewSite = new Intent(SummarySitesActivity.this, AddOrEditSiteActivity.class);
                startActivity(addNewSite);
            }
        });
    }

    void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "initViews()");

        mToolbar = findViewById(R.id.site_summary_toolbar);
        // Used to display the search criteria in the search container at the top of the UI
        mCurrentSearchView = findViewById(R.id.summary_text_current_search);

        // Used to display the sort criteria in the search container at the top of the UI
        mCurrentSortByView = findViewById(R.id.summary_text_current_sort_by);

        mSitesRecyclerView = findViewById(R.id.summary_recycler_sites);

        mEmptyView = findViewById(R.id.summary_view_empty);

        mFilterBar = findViewById(R.id.summary_filter_bar);
        mFilterBar.setOnClickListener(this);

        mFilterClear = findViewById(R.id.summary_button_clear_filter);
        mFilterClear.setOnClickListener(this);
    }

    /**
     * Handles request permission results
     *
     * @param requestCode  code to allow identification of original request permissions
     * @param permissions  permissions requested
     * @param grantResults results of permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE:

                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // permission denied, tell user and exit activity
                    Toast.makeText(this, getString(R.string.Exiting_application), Toast.LENGTH_LONG).show();
                    // Cannot continue as external storage needed to run app, so go to signout page
                    Intent signOut = new Intent(SummarySitesActivity.this, MainActivity.class);
                    // Signal to new activity the source of this intent
                    signOut.putExtra(getString(R.string.intent_sign_out), true);
                    startActivity(signOut);
                }
                break;
        }
    }

    private void initFirestore() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "initFirestore()");

        mFirestore = FirebaseFirestore.getInstance();

        // Get the 50 highest rated sites
        mQuery = mFirestore.collection(getString(R.string.collection_sites))
                .orderBy(Constants.FIELD_STATE, Query.Direction.DESCENDING)
                .limit(Constants.LIMIT);
    }

    private void initRecyclerView() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "initRecyclerView()");

        if (mQuery == null) {
            Log.w(TAG, "No query, not initializing RecyclerView");
        }

        mAdapter = new SiteAdapter(mQuery, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    mSitesRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mSitesRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }
            @Override
            protected void onError(FirebaseFirestoreException e) {
                // Show a snack bar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };
        mSitesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Add a line between each item in the recycler view
        mSitesRecyclerView.addItemDecoration(new
                DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        mSitesRecyclerView.setAdapter(mAdapter);
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

    @Override
    public void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onStart()");

        // Apply filters
        onFilter(mViewModel.getFilters());

        // Start listening for Firestore updates
        if (mAdapter != null) {
            mAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onStop()");

        if (mAdapter != null) {
            mAdapter.stopListening();
        }
    }

    /*
     * Dialog to prompt user for yes/no if they want to exit the activity.
     * The other option is they can hit "SignOut" which logouts of Firebase and
     * takes them back to the login screen.
     */
    @Override
    public void onBackPressed() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onBackPressed()");

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
    public void onFilter(Filters filters) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onFilter()");

        // Construct query basic query
        Query query = mFirestore.collection(getString(R.string.collection_sites));

        // Category (equality filter)
        if (filters.hasState()) {
            query = query.whereEqualTo(Constants.FIELD_STATE, filters.getState());
        }

        // Sort by and order by direction
        if (filters.hasSortBy() && filters.hasSortDirection()) {
            query = query.orderBy(filters.getSortBy(), filters.getSortDirection());
        }

        // Limit items
        query = query.limit(Constants.LIMIT);

        // Update the query
        mQuery = query;
        mAdapter.setQuery(query);

        // Update the search & sort criteria displayed in the search container at the top of the UI
        mCurrentSearchView.setText(Html.fromHtml(filters.getSearchDescription(this)));
        mCurrentSortByView.setText(filters.getOrderDescription(this));

        // Save filters
        mViewModel.setFilters(filters);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onCreateOptionsMenu()");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_summary, menu);
        /* Return true so that the visualizer_menu is displayed in the Toolbar */
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onActivityResult()");

    }

    @Override
    public void onSiteSelected(DocumentSnapshot site) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onSiteSelected()");

        // Go to the comment and details page for the selected site
        Intent intent = new Intent(this, DetailSiteActivity.class);
        intent.putExtra(getString(R.string.intent_site_name), site.getId());

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onOptionsItemSelected()");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_summary_sign_out:
                Intent signOut = new Intent(SummarySitesActivity.this, MainActivity.class);
                // Signal to new activity the source of this intent
                signOut.putExtra(getString(R.string.intent_sign_out), true);
                startActivity(signOut);
                break;

            case R.id.menu_summary_database_manage:
                Intent backupRestore = new Intent(SummarySitesActivity.this, BackUpRestoreActivity.class);
                startActivity(backupRestore);
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

            case R.id.summary_filter_bar:
                //show dialog to filter sites
                mFilterDialog.show(getSupportFragmentManager(), FilterDialogFragment.TAG);
                break;

            case R.id.summary_button_clear_filter:
                // Clear filter
                mFilterDialog.resetFilters();
                onFilter(Filters.getDefault());
                break;

            default:
                break;
        }
    }
}

