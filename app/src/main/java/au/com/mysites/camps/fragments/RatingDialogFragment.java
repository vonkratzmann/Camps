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

import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Rating;
import au.com.mysites.camps.util.Debug;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

/**
 * Dialog Fragment containing rating form.
 */
public class RatingDialogFragment extends DialogFragment {
    public static final String TAG = RatingDialogFragment.class.getSimpleName();

    MaterialRatingBar mRatingBar;

    EditText mRatingText;

    Button mSiteFormSubmit;
    Button mSiteFormCancel;

    public interface RatingListener {
        void onRating(Rating rating);
    }

    private RatingListener mRatingListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "onCreateView()");

        View v = inflater.inflate(R.layout.dialog_rating, container, false);

        return v;
    }

    void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "initViews()");

        mRatingBar = getDialog().findViewById(R.id.site_form_rating);

        mRatingText = getDialog().findViewById(R.id.site_form_text);

        mSiteFormSubmit = getDialog().findViewById(R.id.site_form_button_submit);
        mSiteFormSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Rating rating = new Rating(
                        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()),
                        mRatingBar.getRating(),
                        mRatingText.getText().toString());

                if (mRatingListener != null) {
                    mRatingListener.onRating(rating);
                }
                dismiss();
            }
        });

        mSiteFormCancel = getDialog().findViewById(R.id.site_form_button_cancel);
        mSiteFormCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "onAttach()");

        if (context instanceof RatingListener) {
            mRatingListener = (RatingListener) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY_FRAGMENT) Log.d(TAG, "onResume()");

        Objects.requireNonNull(getDialog().getWindow()).setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        //set layout before initialise the views
        initViews();
    }
}
