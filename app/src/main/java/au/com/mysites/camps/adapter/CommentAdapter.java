package au.com.mysites.camps.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.Query;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.UtilGeneral;

/**
 * RecyclerView adapter for comments when displaying details of a site using
 * the activity DetailSiteActivity
 */

public class CommentAdapter extends FirestoreAdapter<CommentAdapter.ViewHolder> {
    private final static String TAG = CommentAdapter.class.getSimpleName();

    /**
     * @param query database query from the calling routine
     */
    protected CommentAdapter(Query query) {
        super(query);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (Debug.DEBUG_METHOD_ENTRY_ADAPTER) Log.d(TAG, "onCreateViewHolder()");

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.recycler_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CommentAdapter.ViewHolder holder, int position) {
        if (Debug.DEBUG_METHOD_ENTRY_ADAPTER) Log.d(TAG, "onBindViewHolder()");

        holder.bind(getSnapshot(position).toObject(Comment.class));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView dateView;
        TextView authorView;
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.comment_author_image);
            dateView = itemView.findViewById(R.id.comment_date);
            authorView = itemView.findViewById(R.id.comment_author);
            textView = itemView.findViewById(R.id.comment_text);
        }

        void bind(Comment comment) {
            if (Debug.DEBUG_METHOD_ENTRY_ADAPTER) Log.d(TAG, "bind()");

            if (comment == null) return;
            // Check there is a stored image for the author
            if (!UtilGeneral.stringEmpty(comment.getPhoto())) {
                Glide.with(imageView.getContext())
                        .load(comment.getPhoto())
                        .into(imageView);
            }
            dateView.setText(comment.getCreatedDate());
            authorView.setText(comment.getAuthor());
            textView.setText(comment.getText());
        }
    }
}
