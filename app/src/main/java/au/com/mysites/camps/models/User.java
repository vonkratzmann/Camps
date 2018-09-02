package au.com.mysites.camps.models;

/**
 * Tracks users that use the system.
 * <p>
 * profile photo is a file name, while the photo is stored in Firebase storage
 */
public class User {
    private final static String TAG = User.class.getSimpleName();

    private String name;
    private String email;
    private String photo;
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
        this.name = displayName;
        this.email = email;
        this.photo = photoFileName;
        this.lastUsed = lastUsed;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoto() {
        return this.photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getLastUsed() {
        return this.lastUsed;
    }

    public void setLastUsed(String lastUsed) {
        this.lastUsed = lastUsed;
    }
}
