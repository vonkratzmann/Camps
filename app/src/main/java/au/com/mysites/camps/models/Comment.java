package au.com.mysites.camps.models;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import au.com.mysites.camps.R;
import au.com.mysites.camps.util.Debug;

import static au.com.mysites.camps.util.AppContextProvider.getContext;

public class Comment {
    private final static String TAG = Comment.class.getSimpleName();
    public static final String FIELD_SITEID = "siteId";

    private String mText;
    private String mCreatedDate;
    private String mAuthor;
    private String mSiteId;
    private String mPhoto;

    /**
     * Constructor
     */
    public Comment() {
    }

    /**
     * Constructor
     * <p>
     * add comment text, author and generate date created
     *
     * @param text   text for comment
     * @param author author of comment
     */
    @SuppressLint("StringFormatInvalid")
    public Comment(String text, String author) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "Comment()");

        String format = getContext().getString(R.string.dateformat, Locale.ENGLISH);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ENGLISH);
        Date now = new Date();

        this.mCreatedDate = sdf.format(now);
        this.mText = text;
        this.mAuthor = author;
           }

    /**
     * Constructor
     * <p>
     * add comment text, author and date
     *
     * @param text   text for comment
     * @param date   date created for the comment
     * @param author author of comment
     */
    public Comment(String text, String date, String format, String author, String siteId) {

        mText = text;
        mCreatedDate = date;
        mAuthor = author;
        this.mSiteId = siteId;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public String getCreatedDate() {
        return mCreatedDate;
    }

    public void setCreatedDate(String createdDate) {
        this.mCreatedDate = createdDate;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        this.mAuthor = author;
    }

    public String getSiteId() {
        return mSiteId;
    }

    public void setSiteId(String siteId) {
        this.mSiteId = siteId;
    }

    public void setCommentDate(String s) {
        mCreatedDate = s;
    }

    public String getPhoto() {
        return mPhoto;
    }

    public void setPhoto(String photo) {
        this.mPhoto = photo;
    }
}
