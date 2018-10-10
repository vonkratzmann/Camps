package au.com.mysites.camps.models;

import android.text.TextUtils;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model POJO for a rating.
 */
class Rating {

    private String userId;
    private String userName;
    private double rating;
    private @ServerTimestamp
    Date timestamp;

    @SuppressWarnings("unused")
    public Rating() {
    }

    @SuppressWarnings("unused")
    public Rating(FirebaseUser user, double rating) {
        this.userId = user.getUid();
        this.userName = user.getDisplayName();
        if (TextUtils.isEmpty(this.userName)) {
            this.userName = user.getEmail();
        }
        this.rating = rating;
    }

    @SuppressWarnings("unused")
    public String getUserId() {
        return userId;
    }

    @SuppressWarnings("unused")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @SuppressWarnings("unused")
    public String getUserName() {
        return userName;
    }

    @SuppressWarnings("unused")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @SuppressWarnings("unused")
    public double getRating() {
        return rating;
    }

    @SuppressWarnings("unused")
    public void setRating(double rating) {
        this.rating = rating;
    }

    @SuppressWarnings("unused")
    public Date getTimestamp() {
        return timestamp;
    }

    @SuppressWarnings("unused")
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}


