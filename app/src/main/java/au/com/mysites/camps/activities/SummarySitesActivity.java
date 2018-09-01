package au.com.mysites.camps.activities;

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import au.com.mysites.camps.viewmodel.SummarySiteACtivityViewModel;

public class SummarySitesActivity extends AppCompatActivity implements
        FilterDialogFragment.FilterListener,
        SiteAdapter.OnSiteSelectedListener {

    private static final String TAG = SummarySitesActivity.class.getSimpleName();

    Toolbar mToolbar;
    TextView mCurrentSearchView;
    TextView mCurrentSortByView;
    RecyclerView mSitesRecycler;
    ViewGroup mEmptyView;
    CardView mFilterBar;
    ImageView mFilterClear;

    public FirebaseFirestore mFirestore;
    private Query mQuery;

    private FilterDialogFragment mFilterDialog;
    private SiteAdapter mAdapter;

    private SummarySiteACtivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_summary);
        Toolbar toolbar = findViewById(R.id.site_summary_toolbar);
        setSupportActionBar(toolbar);

        initViews();

        // View models
        mViewModel = ViewModelProviders.of(this).get(SummarySiteACtivityViewModel.class);

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

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
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "initViews()");

        mToolbar = findViewById(R.id.site_summary_toolbar);

        mCurrentSearchView = findViewById(R.id.summary_text_current_search);

        mCurrentSortByView = findViewById(R.id.summary_text_current_sort_by);

        mSitesRecycler = findViewById(R.id.summary_recycler_sites);

        mEmptyView = findViewById(R.id.summary_view_empty);

        mFilterBar = findViewById(R.id.summary_filter_bar);
        mFilterBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFilterDialog.show(getSupportFragmentManager(), FilterDialogFragment.TAG);
            }
        });

        mFilterClear = findViewById(R.id.summary_button_clear_filter);
        mFilterClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFilterDialog.resetFilters();
                onFilter(Filters.getDefault());
            }
        });
    }

    private void initFirestore() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "initFirestore()");

        mFirestore = FirebaseFirestore.getInstance();

        // Get the 50 highest rated sites
        mQuery = mFirestore.collection("sites")
                .orderBy("state", Query.Direction.DESCENDING)
                .limit(Constants.LIMIT);
    }

    private void initRecyclerView() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "initRecyclerView()");

        if (mQuery == null) {
            Log.w(TAG, "No query, not initializing RecyclerView");
        }

        mAdapter = new SiteAdapter(mQuery, this) {

            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    mSitesRecycler.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mSitesRecycler.setVisibility(View.VISIBLE);
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
        mSitesRecycler.setLayoutManager(new LinearLayoutManager(this));
        mSitesRecycler.setAdapter(mAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onResume()");
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onStart()");

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
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onStop()");

        if (mAdapter != null) {
            mAdapter.stopListening();
        }
    }

    /*
     * Dialog to prompt user for yes/no if they want to exit the activity.
     * Exist the activity if they want to exit.
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
    public void onFilter(Filters filters) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onFilter()");

        // Construct query basic query
        Query query = mFirestore.collection("sites");

        // Category (equality filter)
        if (filters.hasCategory()) {
            query = query.whereEqualTo("category", filters.getCategory());
        }

        // City (equality filter)
        if (filters.hasCity()) {
            query = query.whereEqualTo("city", filters.getCity());
        }

        // Price (equality filter)
        if (filters.hasPrice()) {
            query = query.whereEqualTo("price", filters.getPrice());
        }

        // Sort by (orderBy with direction)
        if (filters.hasSortBy()) {
            query = query.orderBy(filters.getSortBy(), filters.getSortDirection());
        }

        // Limit items
        query = query.limit(Constants.LIMIT);

        // Update the query
        mQuery = query;
        mAdapter.setQuery(query);

        // Set header
        mCurrentSearchView.setText(Html.fromHtml(filters.getSearchDescription(this)));
        mCurrentSortByView.setText(filters.getOrderDescription(this));

        // Save filters
        mViewModel.setFilters(filters);

        // Set header
        mCurrentSearchView.setText(Html.fromHtml(filters.getSearchDescription(this)));
        mCurrentSortByView.setText(filters.getOrderDescription(this));

        // Save filters
        mViewModel.setFilters(filters);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreateOptionsMenu()");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_summary, menu);
        /* Return true so that the visualizer_menu is displayed in the Toolbar */
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onActivityResult()");

    }

/*
    @OnClick(R.id.filter_bar)
    public void onFilterClicked() {
        // Show the dialog containing filter options
        mFilterDialog.show(getSupportFragmentManager(), FilterDialogFragment.TAG);
    }*/

/*    @OnClick(R.id.button_clear_filter)
    public void onClearFilterClicked() {
        mFilterDialog.resetFilters();

        onFilter(Filters.getDefault());
    }*/

    @Override
    public void onSiteSelected(DocumentSnapshot site) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onSiteSelected()");

        // Go to the comment and details page for the selected site
        Intent intent = new Intent(this, DetailSiteActivity.class);
        intent.putExtra(getString(R.string.intent_site_name), site.getId());

        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onOptionsItemSelected()");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_summary_settings:
                Toast.makeText(this, "show option", Toast.LENGTH_SHORT).show();
                break;

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
}

