package au.com.mysites.camps.models;

/**
 * Tracks users that use the system.
 * <p>
 * profile photo is a file name, while the photo is stored in Firebase storage
 */
public class User {
    private final static String TAG = User.class.getSimpleName();

    private String displayName;
    private String givenName;
    private String familyName;
    private String email;
    private String photoFileName;
    private String lastUsed;
    /**
     * Constructors
     */
    public User() {
    }

    public User(String displayName,
                String givenName,
                String familyName,
                String email,
                String photoFileName,
                String lastUsed) {
        this.displayName = displayName;
        this.givenName = givenName;
        this.familyName = familyName;
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

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
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
