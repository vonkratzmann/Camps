package au.com.mysites.camps.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.models.User;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.UtilDatabase;
import au.com.mysites.camps.util.XmlUtils;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Used to backup and restore database to XML file.
 * Used to do initial loads of small of data amounts of data only mainly
 * for testing purposes.
 */

public class BackUpRestoreActivity extends AppCompatActivity {
    private final static String TAG = BackUpRestoreActivity.class.getSimpleName();

    //Required permissions
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    static boolean havePermissionAccessStorage = false;

    // Constants to select method after permission checks
    private static final int BACKUP = 0;
    private static final int RESTORE = 1;
    private static int mBackupRestoreFlag = 0;

    private FirebaseFirestore mFirestore;
    private CollectionReference mSitesCollectionRef;

    XmlUtils mXmlUtils;

    /**
     * Constructor
     */
    public BackUpRestoreActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_backup_restore);

        // Find the toolbar view inside the activity layout
        Toolbar toolbar = findViewById(R.id.maintbackuprestoretoolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.backup_restore_database_title));

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance();
        //Initialise reference to the sites
        mSitesCollectionRef = mFirestore
                .collection(getString(R.string.collection_sites));
        // set up the listeners for the buttons
        // Listeners will call check storage permissions before doing the backup or restore
        Button buttonBackup = findViewById(R.id.backup);
        buttonBackup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBackupRestoreFlag = BACKUP;
                checkStoragePermission();
            }
        });
        Button buttonRestore = findViewById(R.id.restore);
        buttonRestore.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBackupRestoreFlag = RESTORE;
                checkStoragePermission();
            }
        });
        //get instance of utilities
        mXmlUtils = new XmlUtils();
    }

    /**
     * Gets the user supplied filename and backups the Firestore database in an XML format to
     * three files with a base filename set to the user supplied filename and different
     * extensions for the sites, comments and users files.
     * <p>
     * In turn the different portions of the database are read into memory and then
     * written to the appropriate file. As the the entire portion of the database,
     * ie the sites or the comments or the users is each read into memory in turn, the backup
     * is only meant to be used for backing up a small number of sites. This is meant to be used
     * for testing and development for when a small version of the database can be quickly
     * backed up and restored.
     * <p>
     * Each read of the database is done in an async task, so that task is blocked
     * until the read is complete.
     *
     * @param context context to be used
     */
    public void backupDatabase(final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "backupDatabase()");

        //Get the view of where the filename is entered by the user
        EditText filenameView = findViewById(R.id.backupfilename);
        // Get the user entered filename
        final String filename = filenameView.getText().toString();
        //check filename entered
        if (filename.isEmpty()) {
            Toast.makeText(this, "Backup Database - no filename", LENGTH_SHORT).show();
            return;
        }
        boolean saveSuccessful = true;

        if (!saveSites(filename)) { // Warn the user   

            Toast.makeText(context, getString(R.string.ERROR_Database_sites_backup_failed),
                    Toast.LENGTH_SHORT).show();
            saveSuccessful = false;
        }
        if (!saveComments(filename)) { // Warn the user
            Toast.makeText(context, getString(R.string.ERROR_Database_comments_backup_failed),
                    Toast.LENGTH_SHORT).show();
            saveSuccessful = false;
        }
        if (!saveUsers(filename)) { // Warn the user
            Toast.makeText(context, getString(R.string.ERROR_Database_users_backup_failed),
                    Toast.LENGTH_SHORT).show();
            saveSuccessful = false;
        }
        if (saveSuccessful) {  // tell the user
            Toast.makeText(context, getString(R.string.Database_backup_success), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Read the sites from the Firestore database into memory and save to xml file.
     *
     * @param filename save sites to this file
     * @return true if success
     */
    private boolean saveSites(final String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "saveSites()");

        ArrayList<Site> sites;
        //do in background thread
        GetSitesAsyncTask getSites = new GetSitesAsyncTask();
        try {
            //read the database into an arraylist
            sites = (ArrayList<Site>) getSites
                    .execute(getString(R.string.collection_sites))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        //check we got something to backup
        if (sites == null) {
            return false;
        }
        // Add extension to filename, so separate different files
        String fileNamePlusExt = filename + "." + getString(R.string.collection_sites);

        //write opening tags to buffer
        mXmlUtils.initXmlFile(BackUpRestoreActivity.this, fileNamePlusExt);
        //save it to the file
        for (Site site : sites) {
            mXmlUtils.siteSaveToXMLFile(site);
        }
        //write closing tags to buffer and write buffer to file
        return mXmlUtils.endXmlFile(BackUpRestoreActivity.this);
    }

    /**
     * Read the comments from the Firestore database into memory and save to xml file
     *
     * @param filename save comments to this file
     * @return true if success
     */
    private boolean saveComments(final String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "saveComments()");

        ArrayList<Comment> comments;
        //do in background thread
        GetCommentsAsyncTask getComments = new GetCommentsAsyncTask();
        try {
            //read the database into an arraylist
            comments = (ArrayList<Comment>) getComments
                    .execute(getString(R.string.collection_comments))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        //check we got something to backup
        if (comments == null) {
            return false;
        }
        // Add extension to filename, so separate different files
        String fileNamePlusExt = filename + "." + getString(R.string.collection_comments);

        //write opening tags to buffer
        mXmlUtils.initXmlFile(BackUpRestoreActivity.this, fileNamePlusExt);
        //save it to the file
        for (Comment comment : comments) {
            mXmlUtils.commentSaveToXMLFile(comment);
        }
        //write closing tags to buffer and write buffer to file
        return mXmlUtils.endXmlFile(BackUpRestoreActivity.this);
    }

    /**
     * Read the users from the Firestore database into memory and save to xml file
     *
     * @param filename save users to this file
     * @return true if success
     */
    private boolean saveUsers(final String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "saveUsers()");

        ArrayList<User> users;

        //do in background thread
        GetUsersAsyncTask getUsers = new GetUsersAsyncTask();
        try {
            //read the database into an arraylist
            users = (ArrayList<User>) getUsers
                    .execute(getString(R.string.collection_users))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        //check we got something to backup
        if (users == null) {
            return false;
        }
        // Add extension to filename, so separate different files
        String fileNamePlusExt = filename + "." + getString(R.string.collection_users);

        //write opening tags to buffer
        mXmlUtils.initXmlFile(BackUpRestoreActivity.this, fileNamePlusExt);

        //save it to the file
        for (User user : users) {
            mXmlUtils.userSaveToXMLFile(user);
        }
        //write closing tags to buffer and write buffer to file
        return mXmlUtils.endXmlFile(BackUpRestoreActivity.this);
    }

    /**
     * Gets the user supplied filename and reads the data from the XML supplied filenames
     * into memory. If successful, deletes all of the existing firestore database documents
     * in each of collections for sites,  users and comments.
     * Then loads the data from memory into the firestore database.
     * The firebase storage photo files are not touched.
     *
     * @param context context of calling activity
     */
    public void restoreDatabase(final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "restoreDatabase()");

        //Get the view of where the filename is entered by the user
        EditText filenameView = findViewById(R.id.restorefilename);
        // Get the user entered filename
        String filename = filenameView.getText().toString();
        //check filename entered
        if (filename.isEmpty()) {
            Toast.makeText(context, "Restore Database - no filename", LENGTH_SHORT).show();
            return;
        }

        // Set up storage for sites to be read from file
        final ArrayList<Site> siteList = new ArrayList<>();

        // get the path for the xml file
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();
        //get class where methods are
        XmlUtils xmlUtils = new XmlUtils();

        //Read the file
        if (!xmlUtils.readXmlfile(context, filename, path, siteList)) {
            Toast.makeText(this, "Restore Database - unable to read file", LENGTH_SHORT).show();
            return;
        }

        // do in background thread, delete all documents from the firebase database,
        DeleteSitesAsyncTask deleteSites = new DeleteSitesAsyncTask();
        try {
            deleteSites
                    .execute(getString(R.string.collection_sites), getString(R.string.collection_comments))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            // The Task failed, this is the same exception you'd get in a non-blocking
            // An interrupt occurred while waiting for the task to complete.
            e.printStackTrace();
        }

        // Add sites & their comments read from the xml file, to cleared database
        UtilDatabase.addMultipleSitesAndComments(siteList, context);
        //Tell the user the result
        Toast.makeText(context, " Database restored, No. of sites inserted: " +
                        Integer.toString(siteList.size())
                , LENGTH_SHORT).show();
    }

    /**
     * Check permissions, if ok carry out either backup or restore
     */
    private void checkStoragePermission() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY)
            Log.d(TAG, "checkStoragePermission()()");

        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showMessageOKCancel("You need to allow access to Storage",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(BackUpRestoreActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        Constants.RC_EXTERNAL_STORAGE);
                            }
                        });
                return;
            }
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    Constants.RC_EXTERNAL_STORAGE);
            return;
        }
        // Permission granted, so do it
        switch (mBackupRestoreFlag) {
            case BACKUP:
                backupDatabase(this);
                break;

            case RESTORE:
                restoreDatabase(this);
                break;

            default:
                break;
        }
    }

    /**
     * @param message    message to be displayed
     * @param okListener listener
     */
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * @param requestCode  code to identifier caller
     * @param permissions  being requested
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY)
            Log.d(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case Constants.RC_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, so do it
                    switch (mBackupRestoreFlag) {
                        case BACKUP:
                            backupDatabase(this);
                            break;
                        case RESTORE:
                            restoreDatabase(this);
                            break;
                        default:
                            break;
                    }
                } else {
                    // Permission Denied
                    //havePermissionAccessStorage = false;
                    Toast.makeText(this, getString(R.string.ERROR_Storage_external_unavailable),
                            LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public static class GetSitesAsyncTask extends AsyncTask {
        private final String TAG = GetSitesAsyncTask.class.getSimpleName();

        @Override
        protected ArrayList<Site> doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) objects[0])
                    .get();

            ArrayList<Site> sites = new ArrayList<>();
            Site site;

            try {
                /* Get the result synchronously as executing the task inside a background thread.
                 * Add timeout so that application does not hang */
                QuerySnapshot querySnapshot = Tasks.await(task, 500, TimeUnit.MILLISECONDS);

                if (querySnapshot.isEmpty())     //check we have something
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    site = document.toObject(Site.class);
                    if (Debug.DEBUG_DBMAINT_BACKUP && site != null) Log.d(TAG, "site: " + site.getName());
                    sites.add(site);
                }
            } catch (ExecutionException | InterruptedException e) {
                // The Task failed
                e.printStackTrace();
            } catch (TimeoutException e) {
                // Task timed out before it could complete.
            }
            return sites;
        }
    }

    public static class GetCommentsAsyncTask extends AsyncTask {
        private final String TAG = GetCommentsAsyncTask.class.getSimpleName();

        @Override
        protected ArrayList<Comment> doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) objects[0])
                    .get();

            ArrayList<Comment> comments = new ArrayList<>();
            Comment comment;

            try {
                /* Get the result synchronously as executing the task inside a background thread.
                 * Add timeout so that application does not hang */
                QuerySnapshot querySnapshot = Tasks.await(task, 500, TimeUnit.MILLISECONDS);

                if (querySnapshot.isEmpty())     //check we have something
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    comment = document.toObject(Comment.class);
                    if (Debug.DEBUG_DBMAINT_BACKUP && comment != null)
                        Log.d(TAG, "comment: " + comment.getText());
                    comments.add(comment);
                }
            } catch (ExecutionException | InterruptedException e) {
                // The Task failed
                e.printStackTrace();
            } catch (TimeoutException e) {
                // Task timed out before it could complete.
            }
            return comments;
        }
    }

    public static class GetUsersAsyncTask extends AsyncTask {
        private final String TAG = GetUsersAsyncTask.class.getSimpleName();

        @Override
        protected ArrayList<User> doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) objects[0])
                    .get();

            ArrayList<User> users = new ArrayList<>();
            User user;

            try {
                /* Get the result synchronously as executing the task inside a background thread.
                 * Add timeout so that application does not hang */
                QuerySnapshot querySnapshot = Tasks.await(task, 500, TimeUnit.MILLISECONDS);

                if (querySnapshot.isEmpty())     //check we have something
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    user = document.toObject(User.class);
                    if (Debug.DEBUG_DBMAINT_BACKUP && user != null)
                        Log.d(TAG, "user: " + user.getEmail());
                    users.add(user);
                }
            } catch (ExecutionException | InterruptedException e) {
                // The Task failed
                e.printStackTrace();
            } catch (TimeoutException e) {
                // Task timed out before it could complete.
            }
            return users;
        }
    }

    //todo add some time limits on these methods

    public static class DeleteSitesAsyncTask extends AsyncTask {
        private final String TAG = DeleteSitesAsyncTask.class.getSimpleName();

        @Override
        protected ArrayList<Site> doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //Get list of sites from the database
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) objects[0])
                    .get();

            ArrayList<Site> sites = new ArrayList<>();
            Site site;

            try {
            /* Get the result synchronously as executing the task inside a background thread.
            Uses Google play Task API */
                QuerySnapshot querySnapshot = Tasks.await(task);

                //check we have something
                if (querySnapshot.isEmpty())
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    String docRefId = document.getId();
                    if (Debug.DEBUG_DBMAINT_BACKUP) Log.d(TAG, "docRefId: " + docRefId);
                    // DeleteSite parameters are: Collection name, document reference
                    UtilDatabase.deleteSite((String) objects[0], docRefId);
                }

            } catch (ExecutionException | InterruptedException e) {
                // The Task failed, this is the same exception you'd get in a non-blocking
                // An interrupt occurred while waiting for the task to complete.
                e.printStackTrace();
            }
            return sites;
        }

        /**
         * @param docRefId              comment to be deleted
         * @param collectionRefSites      site ref
         * @param collectionRefComments comment ref
         */
        void deleteCommentsForSite(String docRefId
                , String collectionRefSites
                , String collectionRefComments) {

            if (Debug.DEBUG_DBMAINT_BACKUP) Log.d(TAG, "deleteCommentsForSite()");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection(collectionRefSites)
                    .document(docRefId)
                    .collection(collectionRefComments)
                    .get();

            String commentId;

            try {
                //Get the result synchronously as executing the task inside a background thread.
                QuerySnapshot querySnapshot = Tasks.await(task);

                //check we have something
                if (querySnapshot.isEmpty())
                    return;
                //process each comment, get its id and then delete it, and wait till it finishes
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    commentId = document.getId();
                    if (Debug.DEBUG_DBMAINT_BACKUP) Log.d(TAG, "commentID: " + commentId);

                    FirebaseFirestore commentDocRef = FirebaseFirestore.getInstance();
                    Task<Void> task1 = commentDocRef
                            .collection(collectionRefSites)
                            .document(docRefId)
                            .collection(collectionRefComments)
                            .document(commentId)
                            .delete();
                    //now wait till it finishes
                    Tasks.await(task1);
                }
            } catch (ExecutionException | InterruptedException e) {
                // The Task failed, this is the same exception you'd get in a non-blocking
                // An interrupt occurred while waiting for the task to complete.
                e.printStackTrace();
            }
        }
    }
}
