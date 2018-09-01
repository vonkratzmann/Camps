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
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.util.Debug;

public class CommentDialogFragment extends DialogFragment {
    public static final String TAG = CommentDialogFragment.class.getSimpleName();

    EditText mCommentText;
    Button mSiteFormSubmit;
    Button mSiteFormCancel;

    /**
     * The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it.
     */
    public interface CommentListener {
        void onComment(Comment comment);
    }

    /**
     * Use this instance of the interface to deliver action events
     */
    private CommentDialogFragment.CommentListener mCommentListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (Debug.DEBUG_METHOD_ENTRY_COMMENT) Log.d(TAG, "onCreateView()");

        // Inflate and set the layout for the dialog

        return inflater.inflate(R.layout.dialog_comment, container, false);
    }

    /**
     * On comment submit button compiles the comment and sends it to the calling activity
      */
    void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_COMMENT) Log.d(TAG, "initViews()");

        mCommentText = getDialog().findViewById(R.id.site_form_text);
        mSiteFormSubmit = getDialog().findViewById(R.id.site_form_button_submit);
        mSiteFormSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //get user email to display on the comment
                String email;
                String uid;
                FirebaseUser user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

                if (user != null) {
                    email = user.getEmail();
                    uid = user.getUid();

                    Comment comment = new Comment(mCommentText.getText().toString(), email, uid);

                    if (mCommentListener != null) {
                        mCommentListener.onComment(comment);
                    }
                    dismiss();
                }
            }
        });
        //cancel comment entry
        mSiteFormCancel = getDialog().findViewById(R.id.site_form_button_cancel);
        mSiteFormCancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    /**
     * Override the Fragment.onAttach() method to instantiate the CommentListener
     *
     * @param context context of caller
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (Debug.DEBUG_METHOD_ENTRY_COMMENT) Log.d(TAG, "onAttach()");

        // Verify that the host activity implements the callback interface
        if (context instanceof CommentDialogFragment.CommentListener) {
            // Instantiate the CommentListener so we can send events to the host
            mCommentListener = (CommentDialogFragment.CommentListener) context;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY_COMMENT) Log.d(TAG, "onResume()");

        Objects.requireNonNull(getDialog().getWindow()).setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        //set layout before initialise the views
        initViews();
    }
}
