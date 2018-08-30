package au.com.mysites.camps.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Comment;
import au.com.mysites.camps.models.Site;

import static android.widget.Toast.makeText;

/**
 * Methods to read and write firestore database
 */

public class UtilDatabase {
    private final static String TAG = UtilDatabase.class.getSimpleName();


    /**
     * Write sites to the firebase database with database id equal to the activities name
     * if any comments for each activities, then write as a sub-collection
     *
     * @param siteList list of sites to be written to the database
     * @param context  of calling activity
     */
    public static void addMultipleSitesAndComments(ArrayList<Site> siteList, Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addSitesAndComments()");

        //for each activities write to the database
        int siteCount = siteList.size();
        for (int i = 0; i < siteCount; i++) {
            Site site = siteList.get(i);
            addOneSiteAndComments(site, context);
        }
    }

    /**
     * Write one activities to the firebase database with database id equal to the activities name
     *
     * @param site    to be written to the database
     * @param context of calling activity
     */
    public static void addOneSiteAndComments(final Site site, final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addOneSiteAndComments()");

        //use the activities name as the ID for the database document
        String siteName = site.getName();
        final int numberComments = site.getComments().size(); //get number of comments

        // Initialize Firestore and add activities to the database
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();
        mFirestore.collection(context.getString(R.string.collection_sites))
                .document(siteName)
                .set(site)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void avoid) {
                        //if no comments, tell caller Ok, otherwise let comments tell caller
                        if (numberComments <= 0)
                            UtilDatabase.shortToast(context.getString(R.string.Site_saved),
                                    Constants.TOASTTIMEDATABASE, context);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if no comments, tell caller Not Ok, otherwise let comments tell caller
                        if (numberComments <= 0)
                            UtilDatabase.shortToast(context.getString(R.string.ERROR_Database),
                                    Constants.TOASTTIMEDATABASE, context);
                    }
                });
        //check if any comments to write to the database
        if (numberComments > 0)
            addComments(site, context);
    }

    /**
     * Write comments to the firebase database, comments written as a sub-collection
     *
     * @param site    containing comments to be written to the database
     * @param context of calling activity
     */
    private static void addComments(final Site site, final Context context) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addComments()");

        String siteName = site.getName(); //use the activities name as the ID for the database document

        // Initialize Firestore
        FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();

        int commentCount = site.getComments().size();
        for (int j = 0; j < commentCount; j++) {
            Comment comment = site.getComments().get(j);
            mFirestore.collection(context.getString(R.string.collection_sites))
                    .document(siteName)
                    .collection(context.getString(R.string.collection_comments))
                    .document()
                    .set(comment)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void avoid) {
                            UtilDatabase.shortToast(context.getString(R.string.Comment_saved),
                                    Constants.TOASTTIMEDATABASE, context);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            UtilDatabase.shortToast(context.getString(R.string.ERROR_Database),
                                    Constants.TOASTTIMEDATABASE, context);
                        }
                    });
        }
    }

    /**
     * Saves a file to Firebase storage, at the supplied path.
     *
     * @param context context from calling activity
     * @param file file to be saved to storage
     * @param path file path to use in firebase
     */
    public static void saveFileFirestore(final Context context, final File file,
                                         final String path) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "saveFileFirestore()");

        //Now store file in Firestore storage, create reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        Uri uri = Uri.fromFile(file);

        final String storagePath = path + "/" + file.getName();
        StorageReference fileReference = storageRef
                .child(storagePath);

        // Save the file at the uri to the Firebase storage reference
        fileReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                if (Debug.DEBUG_UTIL) Log.d(TAG, "file: " + file.getName() + " upload success");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "file: " + file.getName() + " upload failure");
                // Warn the user
                shortToast(context.getString(R.string.ERROR_Photo_not_saved), 2000, context);
            }
        });
    }

    /**
     * Checks if file exits on the local device, if yes display it into imageView, else
     * download the file from Firebase Storage and display it in the imageView, so that
     *  future loads of the activities will not require the file to be downloaded again.
     *
     * @param fileName     name of file containing image
     * @param imageView    view where image is to be displayed
     */
    public static void getImageAndDisplay(String fileName, ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getImageAndDisplay()");

        // Get the local storage directory
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // Create local file with directory and name of the file
        final File localFile = new File(storageDir, fileName);

        if (localFile.exists()) {
            // File exists on local device
            if (Debug.DEBUG_UTIL) Log.d(TAG, "getImageAndDisplay() local file exists: " +
                    localFile.toString());
            // Display the file
            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
            imageView.setImageBitmap(bitmap);
        } else {
            // Get the file from storage and display it
            UtilDatabase.getFileOffFirebaseStorageAndDisplay(fileName, localFile,
                    storageDir, imageView);
            if (Debug.DEBUG_UTIL) Log.d(TAG, "getImageAndDisplay() get file from Firebase storage: "
                    + localFile.toString());
        }
    }


    /**
     * Gets a file from storage, store on the local storage directory,
     * and displays it on the imageView.
     *
     * @param fileName      name of file containing image
     * @param localFile     file containing image
     * @param storageDir    directory where the local file is to be stored
     * @param imageView     view where image is to be displayed
     */
    private static void
    getFileOffFirebaseStorageAndDisplay(final String fileName, final File localFile,
                                        final File storageDir, final ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getFileFirebaseOffStorageDisplay()");

        // Check the local storage directory exits
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        // Get a reference to the remote file
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference pathReference = storageRef.child("camps/" + fileName);

        pathReference.getFile(localFile).addOnSuccessListener(
                new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {

                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        if (Debug.DEBUG_UTIL)
                            Log.d(TAG, "getFileFirebaseStorageDisplay() local file created: " +
                                    localFile.toString());

                        // Display the file
                        Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        imageView.setImageBitmap(bitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if (Debug.DEBUG_UTIL)
                    Log.d(TAG, "getFileFirebaseStorageDisplay() local file not created: "
                            + localFile.toString() + exception.toString());
            }
        });
    }

    /**
     * @param docRefId              Document to be deleted
     * @param collectionRefSites    Reference to collection
     */
   public static void deleteSite( final String collectionRefSites, final String docRefId) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "deleteSite");

        FirebaseFirestore siteDocRef = FirebaseFirestore.getInstance();
        siteDocRef
                .collection(collectionRefSites)
                .document(docRefId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (Debug.DEBUG_UTIL) Log.d(TAG, "Document successfully deleted!: "
                                + docRefId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (Debug.DEBUG_UTIL) Log.w(TAG, "Error deleting document: "+ docRefId, e);
                    }
                });
    }

    /**
     * Display a short toast, where the time of displayed is specified
     *
     * @param message      Message to be displayed
     * @param milliseconds Time to show message in milliseconds
     */
    public static void shortToast(final String message,
                                  final int milliseconds, final Context context) {

        final Toast toast = makeText(context, message, Toast.LENGTH_LONG);
        toast.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, milliseconds);
    }


}