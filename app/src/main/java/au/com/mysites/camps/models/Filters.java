package au.com.mysites.camps.models;

import android.content.Context;
import android.text.TextUtils;

import com.google.firebase.firestore.Query;

import au.com.mysites.camps.R;
import au.com.mysites.camps.util.Constants;

public class Filters {

    private String state = null;
    private String sortBy = null;
    private Query.Direction sortDirection = null;

    public Filters() {}

    public static Filters getDefault() {
        Filters filters = new Filters();
        filters.setSortBy(Constants.FIELD_NAME);
        filters.setSortDirection(Query.Direction.ASCENDING);

        return filters;
    }

    public boolean hasState() {
        return !(TextUtils.isEmpty(state));
    }

    public boolean hasSortBy() {
        return !(TextUtils.isEmpty(sortBy));
    }

    public boolean hasSortDirection() {
        return (sortDirection != null);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Query.Direction getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(Query.Direction sortDirection) {
        this.sortDirection = sortDirection;
    }

    /**
     * Gets search description which is a state or all states
     * @param context   of calling activity
     * @return          state used in search description
     */
    public String getSearchDescription(Context context) {
        StringBuilder desc = new StringBuilder();

        if (state == null) {
            desc.append("<b>");
            desc.append(context.getString(R.string.all_sites));
            desc.append("</b>");
        }
        if (state != null) {
            desc.append("<b>");
            desc.append(state);
            desc.append("</b>");
        }
        return desc.toString();
    }

    public String getOrderDescription(Context context) {
        switch (sortBy) {

            case Constants.FIELD_STATE:
                return context.getString(R.string.sorted_by_state);
            case Constants.FIELD_RATING:
                return context.getString(R.string.sorted_by_rating);
            case Constants.FIELD_NAME:
                return context.getString(R.string.sorted_by_name);
            default:
                return context.getString(R.string.sorted_by_name);
        }
    }
}

