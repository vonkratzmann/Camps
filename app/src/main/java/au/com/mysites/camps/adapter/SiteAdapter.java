package au.com.mysites.camps.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import au.com.mysites.camps.R;
import au.com.mysites.camps.model.Site;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.OperationsDatabase;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * RecyclerView adapter for a list of Sites.
 */
public class SiteAdapter extends FirestoreAdapter<SiteAdapter.ViewHolder> {
    private final static String TAG = SiteAdapter.class.getSimpleName();

    public interface OnSiteSelectedListener {
        void onSiteSelected(DocumentSnapshot site);
    }

    private OnSiteSelectedListener mListener;

    protected SiteAdapter(Query query, OnSiteSelectedListener listener) {
        super(query);
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreateViewHolder()");

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.item_site, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onBindViewHolder()");

        holder.bind(getSnapshot(position), mListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final static String TAG = ViewHolder.class.getSimpleName();

        ImageView thumbnailView;        // sitePhoto is not displayed on this layout, only thumbnail
        TextView nameView;
        MaterialRatingBar ratingBar;
        TextView numRatingsView;
        TextView streetView;
        TextView stateView;
        TextView cityView;

        ViewHolder(View itemView) {
            super(itemView);
            if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "ViewHolder()");

            thumbnailView = itemView.findViewById(R.id.site_item_thumbnail);
            nameView = itemView.findViewById(R.id.site_item_name);
            ratingBar = itemView.findViewById(R.id.site_item_rating);
            numRatingsView = itemView.findViewById(R.id.site_item_num_ratings);
            streetView = itemView.findViewById(R.id.site_item_street);
            cityView = itemView.findViewById(R.id.site_item_city);
            stateView = itemView.findViewById(R.id.site_item_state);
        }

        void bind(final DocumentSnapshot snapshot,
                  final OnSiteSelectedListener listener) {
            if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "bind()");

            Site site = snapshot.toObject(Site.class);
            if (Debug.DEBUG_MODEL) Log.d(TAG, "bind() site: " + site.getName());
            if (Debug.DEBUG_MODEL) Log.d(TAG, "bind() thumbnail: " + site.getThumbnail());

            if (site == null) return;

            nameView.setText(site.getName());
            ratingBar.setRating((float) site.getAvgRating());
            streetView.setText(site.getStreet());
            cityView.setText(site.getCity());
            stateView.setText(site.getState());

            /* Check if there is a valid thumbnail. If on local device load into imageView, else
             * download the file from Firebase Storage and display it in the imageView, so that
             *  future loads of the site will not require the file to be downloaded again. */
            if (site.getThumbnail() != null && !site.getThumbnail().isEmpty()) {
                OperationsDatabase.getImageAndDisplay(site.getThumbnail(), thumbnailView);
            }
            // Photo is not displayed on this layout, only thumbnail.
            //todo fix
            // numRatingsView.setText(resources.getString(R.string.fmt_num_ratings,site.getNumRatings()));

            // Click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onSiteSelected(snapshot);
                    }
                }
            });
        }
    }

}


