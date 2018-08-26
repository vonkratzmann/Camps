package au.com.mysites.camps.dbmaint;

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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import au.com.mysites.camps.R;
import au.com.mysites.camps.model.Comment;
import au.com.mysites.camps.model.Site;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.OperationsDatabase;
import au.com.mysites.camps.util.XmlUtils;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * Used to backup and restore database to XML file,
 * can be used to do initial loads of data and testing
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
    private Query mQuery;

    /**
     * Constructor
     */
    public BackUpRestoreActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_RATING) Log.d(TAG, "onCreate()");


        setContentView(R.layout.activity_backup_restore);

        // Find the toolbar view inside the activity layout
        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.maintbackuprestoretoolbar);
        // The internal implementation of the support library just checks if the Toolbar has a title (not null)
        // at the moment the SupportActionBar is set up. If there is, then this title will be used instead of
        // the window title. You can then set a dummy title while you load the real title.
        toolbar.setTitle("");

        // Sets the Toolbar to act as the ActionBar for this Activity window.
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        // Make sure the actionbar exists in the activity and is not null
        if (actionBar != null) {
            //todo clarify fix
            //myActionBar.setHomeAsUpIndicator(R.drawable.bin); can set an image if required by removing comment
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);

            //Set the title
            actionBar.setTitle(R.string.maint_manage_database);
        }

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
    }

    /**
     * get filename and backup the database in an XML format to the supplied filename
     * Database read from firestore, stored in an array then written to a filename
     * provide by the user
     * <p>
     * As comments are in a sub-collection the data base is accessed twice for each site,
     * once for the site and once for the comments.
     * The two sequential database accesses for each site are done synchronously
     * to avoid race conditions. As each read is done in an async task, the task is blocked
     * until the read is complete.
     *
     * @param context context to be used
     */
    public void backupDatabase(final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_DBMAINT) Log.d(TAG, "backupDatabase()");

        //Get the view of where the filename is entered by the user
        EditText filenameView = findViewById(R.id.backupfilename);
        // Get the user entered filename
        final String filename = filenameView.getText().toString();
        //check filename entered
        if (filename.isEmpty()) {
            Toast.makeText(this, "Backup Database - no filename", LENGTH_SHORT).show();
            return;
        }
        //read the database and save to xml file
        ArrayList<Site> sites = new ArrayList<>();

        //do in background thread
        GetSitesAsyncTask getSites = new GetSitesAsyncTask();
        try {
            //read the database into an arraylist

            sites = (ArrayList<Site>) getSites
                    .execute(getString(R.string.collection_sites), getString(R.string.collection_comments))
                    .get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        //check we got something to backup
        if (sites == null) {
            //no warn the user
            Toast.makeText(context, getString(R.string.Database_backup_null),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        //get instance of utilities
        final XmlUtils xmlUtils = new XmlUtils();
        //write opening tags to buffer
        xmlUtils.initXmlFile(BackUpRestoreActivity.this, filename);

        //save it to the file
        for (Site site : sites) {
            xmlUtils.siteSaveToXMLFile(site);
        }
        //write closing tags to buffer and write buffer to file
        Boolean result = xmlUtils.endXmlFile(BackUpRestoreActivity.this);
        // Tell the user the result
        if (result) {
            Toast.makeText(context, getString(R.string.Database_backup_success),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, getString(R.string.ERROR_Database_backup_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Gets the user supplied filename,
     * read the data from the XML supplied filename,
     * delete the existing firebase documents,
     * load the data into the firebase database.
     *
     * @param context context of calling activity
     */
    public void restoreDatabase(final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "restoreDatabase()");

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
        OperationsDatabase.addMultipleSitesAndComments(siteList, context);
        //Tell the user the result
        Toast.makeText(context, " Database restored, No. of sites inserted: " +
                        Integer.toString(siteList.size())
                , LENGTH_SHORT).show();
    }

    /**
     * Check permissions, if ok carry out either backup or restore
     */
    private void checkStoragePermission() {
        if (Debug.DEBUG_METHOD_ENTRY_RATING)
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
                                        Constants.REQUEST_EXTERNAL_STORAGE);
                            }
                        });
                return;
            }
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                    Constants.REQUEST_EXTERNAL_STORAGE);
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
     *
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (Debug.DEBUG_METHOD_ENTRY_RATING)
            Log.d(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case Constants.REQUEST_EXTERNAL_STORAGE:
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

    //todo add some time limits on these methods
    public class GetSitesAsyncTask extends AsyncTask {
        private final String TAG = GetSitesAsyncTask.class.getSimpleName();

        @Override
        protected ArrayList<Site> doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_DBMAINT) Log.d(TAG, "doInBackground");

            //uses Google play Task API
            Task<QuerySnapshot> task = FirebaseFirestore
                    .getInstance()
                    .collection((String) objects[0])
                    .get();

            ArrayList<Site> sites = new ArrayList<>();
            Site site;

            try {
                //Get the result synchronously as executing the task inside a background thread.
                QuerySnapshot querySnapshot = Tasks.await(task);

                //check we have something
                if (querySnapshot.isEmpty())
                    return null;
                //process each document
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    site = document.toObject(Site.class);
                    if (Debug.DEBUG_DBMAINT_BACKUP)
                        Log.d(TAG, "site: " + site.getName());

                    //Get comments as they are in a sub-collection, then add to the site
                    getCommentsForSite(site, (String) objects[0], (String) objects[1]);
                    sites.add(site);
                }

            } catch (ExecutionException | InterruptedException e) {
                // The Task failed, this is the same exception you'd get in a non-blocking
                // An interrupt occurred while waiting for the task to complete.
                e.printStackTrace();
            }
            return sites;
        }
    }

    /**
     * @param site
     */
    void getCommentsForSite(Site site, String collRefSites, String collRefComments) {
        if (Debug.DEBUG_DBMAINT_BACKUP) Log.d(TAG, "getCommentsForSite()");

        //uses Google play Task API
        Task<QuerySnapshot> task = FirebaseFirestore
                .getInstance()
                .collection(collRefSites)
                .document(site.getName())
                .collection(collRefComments)
                .get();

        Comment comment;

        try {
            //Get the result synchronously as executing the task inside a background thread.
            QuerySnapshot querySnapshot = Tasks.await(task);

            //check we have something
            if (querySnapshot.isEmpty())
                return;
            //process each comment
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                comment = document.toObject(Comment.class);
                if (Debug.DEBUG_DBMAINT_BACKUP)
                    Log.d(TAG, "comment text: " + comment.getText());
                //add to comment to the site
                site.addComment(comment);
            }
        } catch (ExecutionException | InterruptedException e) {
            // The Task failed, this is the same exception you'd get in a non-blocking
            // An interrupt occurred while waiting for the task to complete.
            e.printStackTrace();
        }
    }


    //todo add some time limits on these methods

    public class DeleteSitesAsyncTask extends AsyncTask {
        private final String TAG = DeleteSitesAsyncTask.class.getSimpleName();

        @Override
        protected ArrayList<Site> doInBackground(Object[] objects) {
            if (Debug.DEBUG_METHOD_ENTRY_DBMAINT) Log.d(TAG, "doInBackground");

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

                    //Get comments as they are in a sub-collection, then delete
                    deleteCommentsForSite(docRefId, (String) objects[0], (String) objects[1]);
                    deleteSite(docRefId, (String) objects[0]);
                }

            } catch (ExecutionException | InterruptedException e) {
                // The Task failed, this is the same exception you'd get in a non-blocking
                // An interrupt occurred while waiting for the task to complete.
                e.printStackTrace();
            }
            return sites;
        }

        /**
         * @param docRefId
         * @param collectionRefSites
         * @param collectionRefComments
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

        /**
         * @param docRefId
         * @param collectionRefSites
         */
        void deleteSite(String docRefId, String collectionRefSites) {
            if (Debug.DEBUG_DBMAINT_BACKUP) Log.d(TAG, "deleteStite");

            FirebaseFirestore siteDocRef = FirebaseFirestore.getInstance();
            siteDocRef
                    .collection(collectionRefSites)
                    .document(docRefId)
                    .delete();
        }
    }
}
