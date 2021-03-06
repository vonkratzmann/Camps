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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

import au.com.mysites.camps.R;

import static android.widget.Toast.makeText;

/**
 * Methods to read and write firestore database
 */

public class UtilDatabase {
    private final static String TAG = UtilDatabase.class.getSimpleName();

    // Initialize Firestore database
    private final static FirebaseFirestore mFirestore = FirebaseFirestore.getInstance();


    /**
     * Write one document to the firebase database
     *
     * @param documentName Name of document to be written to the database, can be null
     * @param document     to be written to the database
     * @param context      of calling activity
     * @param collection   to store the document in
     */
    public static void addDocument(final String documentName, final Object document,
                                   final Context context, final String collection) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "addDocument()");

        if (documentName == null) {
            mFirestore.collection(collection).add(document)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            if (Debug.DEBUG_UTIL)
                                Log.d(TAG, context.getString(R.string.Document_saved) + documentReference.getId());
                            makeText(context, context.getString(R.string.Site_saved), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (Debug.DEBUG_UTIL)
                                Log.d(TAG, context.getString(R.string.Document_not_saved));
                            makeText(context, context.getString(R.string.ERROR_Site_not_saved), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {  // Specify the name of the document
            mFirestore.collection(collection).document(documentName).set(document)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (Debug.DEBUG_UTIL)
                                Log.d(TAG, context.getString(R.string.Document_saved));
                            makeText(context, context.getString(R.string.Site_saved), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (Debug.DEBUG_UTIL)
                                Log.d(TAG, context.getString(R.string.Document_not_saved));
                            makeText(context, context.getString(R.string.ERROR_Site_not_saved), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    /**
     * Saves a file to Firebase storage, at the supplied path.
     *
     * @param context context from calling activity
     * @param file    file to be saved to storage
     * @param path    file path to use in firebase
     */
    public static void saveFileFirebaseStorage(final Context context, final File file,
                                               final String path) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "saveFileFirebaseStorage()");

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
                shortToast(context.getString(R.string.ERROR_File_not_saved)
                        + "e:" + e, 2000, context);
            }
        });
    }

    /**
     * Checks if file exits on the local device, if yes display it into imageView, else
     * download the file from Firebase Storage and display it in the imageView, so that
     * future loads of the site will not require the file to be downloaded again.
     *
     * @param context   of calling activity
     * @param fileName  name of file in firebase storage containing image
     * @param imageView view where image is to be displayed
     */
    public static void getImageAndDisplay(Context context, String fileName, ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getImageAndDisplay()");

        // Get the local storage directory
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // Create instance of local file with directory and name of the file
        final File localFile = new File(storageDir, fileName);

        try {
            if (localFile.exists()) {
                // File exists on local device
                if (Debug.DEBUG_UTIL) Log.d(TAG, "local file exists: " +
                        localFile.toString());
                // Display the file
                Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                imageView.setImageBitmap(bitmap);
            } else {
                // Check the local storage directory exits
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }
                // Create an empty local file
                localFile.createNewFile();
                // Get the file from Firebase storage, display it, and store locally
                UtilDatabase.getFileOffFirebaseStorageAndDisplay(context, fileName, localFile,
                        storageDir, imageView);
                if (Debug.DEBUG_UTIL) {
                    Log.d(TAG, "get file from Firebase storage: " + fileName);
                    Log.d(TAG, "get file from Firebase store in: " + localFile.toString());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getImageAndDisplay() exception e: " + e +
                    " File: " + localFile.toString());
        }
    }

    /**
     * Gets a file from storage, store on the local storage directory,
     * and displays it on the imageView.
     *
     * @param context    of calling routine
     * @param fileName   name of file in firebase storage containing image
     * @param localFile  store image retrieved from firebase storage in this file
     * @param storageDir directory where the local file is to be stored
     * @param imageView  view where image is to be displayed
     */
    private static void
    getFileOffFirebaseStorageAndDisplay(final Context context, final String fileName,
                                        final File localFile,
                                        final File storageDir, final ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "getFileFirebaseOffStorageDisplay()");
        // Get a reference to the remote file
        try {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference pathReference = storageRef.child(context.getString(R.string.firebase_photos)
                    + "/" +fileName);
            pathReference.getFile(localFile).addOnSuccessListener(
                    new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {

                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            if (Debug.DEBUG_UTIL)
                                Log.d(TAG, "image read from firebase storage: " + fileName);

                            // Display the file
                            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                            imageView.setImageBitmap(bitmap);
                        }
                    }).addOnFailureListener(new OnFailureListener() {

                @Override
                public void onFailure(@NonNull Exception exception) {
                    if (Debug.DEBUG_UTIL)
                        Log.w(TAG, "error reading from firebase storage: "
                                + localFile.toString() + exception.toString());
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "exception: " + e);
        }
    }

    /**
     * @param docRefId           Document to be deleted
     * @param collectionRefSites Reference to collection
     */
    public static void deleteSite(final String collectionRefSites, final String docRefId) {
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
                        if (Debug.DEBUG_UTIL)
                            Log.w(TAG, "Error deleting document: " + docRefId, e);
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
