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

    private String text;
    private String createdDate;
    private String author;
    private String siteId;

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

        this.createdDate = sdf.format(now);
        this.text = text;
        this.author = author;
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
    public Comment(String text, String date, String author, String siteId) {

        this.text = text;
        this.createdDate = date;
        this.author = author;
        this.siteId = siteId;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSiteId() {
        return this.siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }
}
