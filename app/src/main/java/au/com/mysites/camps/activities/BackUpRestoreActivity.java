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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.models.User;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
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

    // Set up storage for sites to be read from file
    public final ArrayList<Site> mSiteList = new ArrayList<>();
    // Set up storage for comments to be read from file
    public final ArrayList<Comment> mCommentList = new ArrayList<>();
    // Set up storage for users to be read from file
    public final ArrayList<User> mUserList = new ArrayList<>();

    TextView mProgressTextView;

    Toast mToast;

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

        mProgressTextView = findViewById(R.id.backup_restore_progress_textView);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
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
            mToast.setText(R.string.ERROR_Database_no_filename);
            mToast.show();
            return;
        }

        //Store status messages as UI will be locked as we do a wait for the asynctasks to finish
        ArrayList<String> statusMessages = new ArrayList();

        statusMessages.add(getString(R.string.Database_backup_started));

        if (!saveSites(filename)) { // Warn the user
            statusMessages.add(getString(R.string.ERROR_Database_sites_backup_failed));
        }
        if (!saveComments(filename)) { // Warn the user
            statusMessages.add(getString(R.string.ERROR_Database_comments_backup_failed));
        }
        if (!saveUsers(filename)) { // Warn the user
            statusMessages.add(getString(R.string.ERROR_Database_users_backup_failed));
        }

        if (statusMessages.size() == 0) {  // Tell the user
            mToast.setText(R.string.Database_backup_success);
            mToast.show();
        } else {    // Warn the user
             int size = statusMessages.size();
             for (int i = 0; i < size; i++ ) {
                 mToast.setText(R.string.Database_backup_failed);
                 mToast.show();
             }
        }
    }

    /**
     * Gets the user supplied filename and reads the data from the XML supplied filenames
     * into memory. If successful, deletes all of the existing firestore database documents
     * in each of collections for sites, users and comments.
     * Then loads the data from memory into the firestore database.
     * The firebase storage photo files are not touched.
     * <p>
     * If errors reading any of the file the restore is aborted.
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
        if (filename.isEmpty()) { // Warn the user if error
            Toast.makeText(context, getString(R.string.ERROR_Database_no_filename),
                    LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(context, getString(R.string.Database_restore_started), LENGTH_SHORT).show();
        }

        if (!readFiles(filename)) { // Warn the user if error
            Toast.makeText(this, getString(R.string.ERROR_File_error),
                    LENGTH_SHORT).show();
            return;
        }
        //if ok delete sites
        if (!deleteDatabase()) { // Warn the user if error
            Toast.makeText(this, getString(R.string.ERROR_Database_unable_delete_documents),
                    LENGTH_SHORT).show();
            return;
        }

        // Add sites to cleared database
        String string = null;
        try {
            string = new AddSiteAsyncTask().execute(mSiteList).get(Constants.ASYNCTIMEOUT,
                    TimeUnit.MILLISECONDS);
        } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException e) {
            Log.w(TAG, e);
        }
        //Tell them the result
        Toast.makeText(context, "message: " + string, LENGTH_SHORT).show();

        // Add comments to cleared database
        try {
            string = new AddCommentAsyncTask().execute(mCommentList).get(Constants.ASYNCTIMEOUT,
                    TimeUnit.MILLISECONDS);
        } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException e) {
            Log.w(TAG, e);
        }
        //Tell them the result
        Toast.makeText(context, "message: " + string, LENGTH_SHORT).show();

        // Add users to cleared database
        try {
            string = new AddUserAsyncTask().execute(mUserList).get(Constants.ASYNCTIMEOUT,
                    TimeUnit.MILLISECONDS);
        } catch (CancellationException | ExecutionException | InterruptedException |
                TimeoutException e) {
            Log.w(TAG, e);
        }
        //Tell them the result
        Toast.makeText(context, "message: " + string, LENGTH_SHORT).show();
    }

    /**
     * Read the sites from the Firestore database into memory and save to xml file.
     *
     * @param filename save sites to this file
     * @return true if success
     */
    @SuppressWarnings("unchecked")
    private boolean saveSites(final String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "saveSites()");

        ArrayList<Site> mSites;
        //do in background thread as we want to wait until query completed
        GetSitesAsyncTask getSites = new GetSitesAsyncTask();
        try {
            //read the database into an arraylist
            mSites = new GetSitesAsyncTask().execute(getString(R.string.collection_sites))
                    .get(Constants.ASYNCTIMEOUT, TimeUnit.MILLISECONDS);
            // mSites = (ArrayList<Site>) getSites
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            return false;
        }
        //check we got something to backup
        if (mSites == null) {
            return false;
        }
        // Add extension to filename, so separate different files
        String fileNamePlusExt = filename + "." + getString(R.string.collection_sites);

        //write opening tags to buffer
        mXmlUtils.initXmlFile(BackUpRestoreActivity.this, fileNamePlusExt);
        //save it to the file
        for (Site site : mSites) {
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
     * Reads each of the backup files. If any errors returns and aborts the restore.
     *
     * @param filename filename to restore database from
     * @return true if success
     */
    private boolean readFiles(String filename) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "readFiles()");

        String filenamePlusExt;     //Used to separate the different files i.e. site, comment, user

        //get instance of utilities
        XmlUtils xmlUtils = new XmlUtils();

        // get the path for the xml file
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();

        //Read the site file
        filenamePlusExt = filename + "." + getString(R.string.collection_sites);
        if (!xmlUtils.readSiteXmlfile(this, filenamePlusExt, path, mSiteList)) return false;

        //Read the comment file
        filenamePlusExt = filename + "." + getString(R.string.collection_comments);
        if (!xmlUtils.readCommentXmlfile(this, filenamePlusExt, path, mCommentList)) return false;

        //Read the user file
        filenamePlusExt = filename + "." + getString(R.string.collection_users);
        if (!xmlUtils.readUserXmlfile(this, filenamePlusExt, path, mUserList)) return false;

        return true;
    }

    /**
     * Do in background thread, and wait for each delete to finish, so do not swamp the resources.
     * Delete collections for sites, comments and users.
     *
     * @return
     */
    private boolean deleteDatabase() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "deleteDatabase()");

        DeleteDatabaseAsyncTask deleteDatabase = new DeleteDatabaseAsyncTask();
        boolean result = true;

        // Delete sites
        try {
            deleteDatabase
                    .execute(getString(R.string.collection_sites),
                            getString(R.string.collection_comments),
                            getString(R.string.collection_users))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
     * Check permissions, if ok carry out either backup or restore
     */
    private void checkStoragePermission() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "checkStoragePermission()()");

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
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "onRequestPermissionsResult()");

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


    //Start class GetSitesAsyncTask
    public class GetSitesAsyncTask extends AsyncTask<String, Void, ArrayList> {
        private final String TAG = GetSitesAsyncTask.class.getSimpleName();

        /**
         * Get list of sites from the Firestore database.
         *
         * @param strings name of collection
         * @return list of sites or null
         */
        @Override
        protected ArrayList<Site> doInBackground(String... strings) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) strings[0])
                    .get();

            ArrayList<Site> sites = new ArrayList<>();
            Site site;

            try {
                // Wait until the query is finished with TasksAwait or timeout
                QuerySnapshot querySnapshot = Tasks.await(task, Constants.QUERYTIMEOUT,
                        TimeUnit.MILLISECONDS);

                if (querySnapshot.isEmpty())     //check we have something
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    site = document.toObject(Site.class);
                    if (Debug.DEBUG_BACKUP_RESTORE && site != null)
                        Log.d(TAG, "site: " + site.getName());
                    sites.add(site);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                // The Task failed
                e.printStackTrace();
                return null;
            }
            return sites;
        }
    }
    //End Class


    /* Class GetCommentsAsyncTask */
    public class GetCommentsAsyncTask extends AsyncTask<String, Void, ArrayList> {
        private final String TAG = GetCommentsAsyncTask.class.getSimpleName();

        /**
         * Get list of comments from the Firestore database.
         *
         * @param strings name of collection
         * @return list of comments or null
         */
        @Override
        protected ArrayList<Comment> doInBackground(String... strings) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) strings[0])
                    .get();

            ArrayList<Comment> comments = new ArrayList<>();
            Comment comment;

            try {
                // Wait until the query is finished with TasksAwait or timeout
                QuerySnapshot querySnapshot = Tasks.await(task, Constants.QUERYTIMEOUT,
                        TimeUnit.MILLISECONDS);

                if (querySnapshot.isEmpty())     //check we have something
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    comment = document.toObject(Comment.class);
                    if (Debug.DEBUG_BACKUP_RESTORE && comment != null)
                        Log.d(TAG, "comment: " + comment.getText());
                    comments.add(comment);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                // The Task failed
                e.printStackTrace();
                return null;
            }
            return comments;
        }
    }
    /* End Class */


    /* Class GetUsersAsyncTask */
    public class GetUsersAsyncTask extends AsyncTask<String, Void, ArrayList> {
        private final String TAG = GetUsersAsyncTask.class.getSimpleName();

        /**
         * Get list of uses from the Firestore database.
         *
         * @param strings name of collection
         * @return list of users or null
         */
        @Override
        protected ArrayList<User> doInBackground(String... strings) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) strings[0])
                    .get();

            ArrayList<User> users = new ArrayList<>();
            User user;

            try {
                // Wait until the query is finished with TasksAwait or timeout
                QuerySnapshot querySnapshot = Tasks.await(task, Constants.QUERYTIMEOUT,
                        TimeUnit.MILLISECONDS);

                if (querySnapshot.isEmpty())     //check we have something
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    user = document.toObject(User.class);
                    if (Debug.DEBUG_BACKUP_RESTORE && user != null)
                        Log.d(TAG, "user: " + user.getEmail());
                    users.add(user);
                }
            } catch (TimeoutException | ExecutionException | InterruptedException e) {
                // Task timed out before it could complete.
                return null;
            }
            return users;
        }
    }
    /* End Class */


    /* class DeleteDatabaseAsyncTask */
    public class DeleteDatabaseAsyncTask extends AsyncTask {
        private final String TAG = DeleteDatabaseAsyncTask.class.getSimpleName();

        /**
         * Reads the documents from each collection and then in turn deletes each document.
         * Done in a background thread and waits for each read and delete to be completed so
         * as not to consume too many resources.
         *
         * @param objects
         * @return true if success
         */
        @Override
        protected Object doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground()");

            int count = objects.length;
            for (int i = 0; i < count; i++) {   // Do for each parameter passed to doInBackground
                //Get list of items from the database
                Task<QuerySnapshot> task = FirebaseFirestore
                        .getInstance()
                        .collection((String) objects[i])
                        .get();
                try {
            /* Get the result synchronously as executing the task inside a background thread.
            Uses Google play Task API */
                    QuerySnapshot querySnapshot = Tasks.await(task, 2000, TimeUnit.MILLISECONDS);

                    //check we have something
                    if (querySnapshot.isEmpty())
                        return null;
                    // Delete documents retruned for collection
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        deleteDocument(document, (String) objects[i]);
                    }
                } catch (ExecutionException | TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
    /* End Class */


    /**
     * @param document to be deleted
     * @return true if success
     */
    private void deleteDocument(final DocumentSnapshot document, final String myCollection) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "deleteDocument()");

        final String docId = document.getId();
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

        mFirestore.collection(myCollection).document(docId).delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Document deleted: " + docId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Document not deleted: " + docId, e);
                    }
                });
    }

    /* class AddSiteAsyncTask  */
    private class AddSiteAsyncTask extends AsyncTask<ArrayList, Void, String> {
        private final String TAG = AddSiteAsyncTask.class.getSimpleName();

        /**
         * @param sites contains sites to be added
         * @return string with result description
         */
        @Override
        protected String doInBackground(ArrayList... sites) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            ArrayList<Site> mySites = sites[0];
            Site site;
            String result = "Added sites success";

            //for each site write to the database
            int siteCount = mySites.size();
            for (int i = 0; i < siteCount; i++) {
                site = mySites.get(i);

                //uses Google play Task API
                Task<Void> task = FirebaseFirestore
                        .getInstance()
                        .collection("sites")
                        .document(site.getName())
                        .set(site);
                try {
                    /* Get the result synchronously as executing the task inside a background thread.
                     * Add timeout so that application does not hang */
                    Tasks.await(task, 1000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException | ExecutionException | InterruptedException e) {
                    // Task timed out before it could complete.
                    Log.w(TAG, "Site not added: " + site.getName());
                    result = "Sites not added";
                }
            }
            return result;
        }
    }
    /* End Class */


    /* class AddCommentAsyncTask */
    private class AddCommentAsyncTask extends AsyncTask<ArrayList, Void, String> {
        private final String TAG = AddCommentAsyncTask.class.getSimpleName();

        /**
         * @param comments to be added to the database
         * @return string with result description
         */
        @Override
        protected String doInBackground(ArrayList... comments) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            ArrayList<Comment> myComments = comments[0];
            Comment comment;
            String result = "Added comments success";

            //for each comment write to the database
            int commentCount = myComments.size();
            for (int i = 0; i < commentCount; i++) {
                comment = myComments.get(i);
                //uses Google play Task API
                Task<com.google.firebase.firestore.DocumentReference> task = FirebaseFirestore
                        .getInstance()
                        .collection("comments")
                        .add(comment);
                try {
                    /* Get the result synchronously as executing the task inside a background thread.
                     * Add timeout so that application does not hang */
                    Tasks.await(task, 1000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException | ExecutionException | InterruptedException e) {
                    // Task timed out before it could complete.
                    Log.w(TAG, "Comment not added: " + comment.getAuthor());
                    result = "Comments not added";
                }
            }
            return result;
        }
    }
    /* End Class */


    /* Class AddCommentAsyncTask */
    private class AddUserAsyncTask extends AsyncTask<ArrayList, Void, String> {
        private final String TAG = AddUserAsyncTask.class.getSimpleName();

                /**
         * @param users users to be added
         * @return string with result description
         */
        @Override
        protected String doInBackground(ArrayList... users) {
            if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "doInBackground");

            ArrayList<User> myUsers = users[0];
            User user;
            String result = "Added users success";

            //for each user write to the database
            int userCount = myUsers.size();
            for (int i = 0; i < userCount; i++) {
                user = myUsers.get(i);

                //uses Google play Task API
                Task<com.google.firebase.firestore.DocumentReference> task = FirebaseFirestore
                        .getInstance()
                        .collection("users")
                        .add(user);
                try {
                    /* Get the result synchronously as executing the task inside a background thread.
                     * Add timeout so that application does not hang */
                    Tasks.await(task, 1000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException | ExecutionException | InterruptedException e) {
                    // Task timed out before it could complete.
                    Log.w(TAG, "User not added: " + user.getEmail());
                    result = "Users not added";
                }
            }
            return result;
        }
    }
    /* End Class */
}

