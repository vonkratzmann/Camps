package au.com.mysites.camps.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;

import com.google.firebase.firestore.Query;

import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Filters;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;

/**
 * Allows for filtering of sites on the summary activity UI
 * Options are:
 *   Filter by site
 *   Order by, Name, or State or Rating
 */
public class FilterDialogFragment extends DialogFragment {

    public static final String TAG = "FilterDialog";

    /**
     * Implemented in SummarySitesActivity. This pases the selected filters back
     * to SummarySitesActivity
     */
    public interface FilterListener {

        void onFilter(Filters filters);
    }

    private View mRootView;

    private Spinner mStateSpinner;
    private Spinner mSortSpinner;

    private FilterListener mFilterListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "onCreateView()");

        mRootView = inflater.inflate(R.layout.dialog_filters, container, false);

        initViews();
        return mRootView;
    }

    private void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "initViews()");

        mStateSpinner = mRootView.findViewById(R.id.filter_spinner_state);
        mSortSpinner = mRootView.findViewById(R.id.filter_spinner_sort);

        Button mButtonCancel = mRootView.findViewById(R.id.filter_button_cancel);
        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        Button mButtonSearch = mRootView.findViewById(R.id.filter_button_search);
        mButtonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFilterListener.onFilter(getFilters());
                getDialog().dismiss();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "onAttach()");

        if (context instanceof FilterListener) {
            mFilterListener = (FilterListener) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "onResume()");

        Objects.requireNonNull(getDialog().getWindow()).setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Nullable
    private String getSelectedState() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "getSelectedState()");

        String selected = (String) mStateSpinner.getSelectedItem();
        if (getString(R.string.value_any_state).equals(selected)) {
            return null;
        } else {
            return selected;
        }
    }

    @Nullable
    private String getSelectedSortBy() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "getSelectedSortBy()");

        String selected = (String) mSortSpinner.getSelectedItem();
        if (getString(R.string.sort_by_rating).equals(selected)) {
            return Constants.FIELD_RATING;
        }
        if (getString(R.string.sort_by_state).equals(selected)) {
            return Constants.FIELD_STATE;
        }
        if (getString(R.string.sort_by_name).equals(selected)) {
            return Constants.FIELD_NAME;
        }
        return null;
    }

    @Nullable
    private Query.Direction getSortDirection() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "getSortDirection()");

        String selected = (String) mSortSpinner.getSelectedItem();
        if (getString(R.string.sort_by_rating).equals(selected)) {
            return Query.Direction.DESCENDING;
        }
        if (getString(R.string.sort_by_state).equals(selected)) {
            return Query.Direction.ASCENDING;
        }
        if (getString(R.string.sort_by_name).equals(selected)) {
            return Query.Direction.ASCENDING;
        }

        return null;
    }

    public void resetFilters() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "resetFilters()");

        if (mRootView != null) {
            mStateSpinner.setSelection(0);
        }
    }

    private Filters getFilters() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "getFilters()");

        Filters filters = new Filters();

        if (mRootView != null) {
            filters.setState(getSelectedState());
            filters.setSortBy(getSelectedSortBy());
            filters.setSortDirection(getSortDirection());
        }
        return filters;
    }
}

