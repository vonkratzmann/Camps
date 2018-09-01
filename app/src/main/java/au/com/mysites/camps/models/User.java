package au.com.mysites.camps.models;

/**
 * Tracks users that use the system.
 * <p>
 * profile photo is a file name, while the photo is stored in Firebase storage
 */
public class User {
    private final static String TAG = User.class.getSimpleName();

    private String displayName;
    private String email;
    private String photoFileName;
    private String lastUsed;
    /**
     * Constructors
     */
    public User() {
    }

    public User(String displayName,
                String email,
                String photoFileName,
                String lastUsed) {
        this.displayName = displayName;
        this.email = email;
        this.photoFileName = photoFileName;
        this.lastUsed = lastUsed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }


    public String getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(String lastUsed) {
        this.lastUsed = lastUsed;
    }
}
