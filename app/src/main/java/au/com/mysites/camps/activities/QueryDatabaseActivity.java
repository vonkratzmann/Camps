package au.com.mysites.camps.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import au.com.mysites.camps.R;
import au.com.mysites.camps.util.Debug;

/**
 * General query and testing code to sort out and test issues before moving code to
 * the normal app activities.
 *
 * This is code is not available to normal users
 *
 */
public class QueryDatabaseActivity extends AppCompatActivity {
    private static final String TAG = QueryDatabaseActivity.class.getSimpleName();

    private FirebaseFirestore mFirestore;
    private DocumentReference mDocumentRef;
    private CollectionReference mCommentsCollectionRef;
    Query mQuery;
    Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_database);
        Toolbar toolbar = findViewById(R.id.site_summary_toolbar);
        setSupportActionBar(toolbar);

        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runQuery();
            }
        });

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance();
    }

    void runQuery() {
        if (Debug.DEBUG_METHOD_ENTRY) Log.d(TAG, "runQuery()");

        // Get reference to the activities
        mCommentsCollectionRef = mFirestore
                .collection(getString(R.string.collection_sites))
                .document("Barbour Park Rest Area")
                .collection(getString(R.string.collection_comments));

        mCommentsCollectionRef
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document :  task.getResult()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }
/*

    void compressImage() {


        StorageReference childRef2 = your firebase storage path
        storageRef.child(UserDetails.username+"profilepic.jpg");
        Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 25, baos);
        byte[] data = baos.toByteArray();
        //uploading the image
        UploadTask uploadTask2 = childRef2.putBytes(data);
        uploadTask2.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                pd.dismiss();
                Toast.makeText(Profilepic.this, "Upload successful", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(Profilepic.this, "Upload Failed -> " + e, Toast.LENGTH_LONG).show();
            }
        });`
    }

//take a photo

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    //get a bit map of the photo taken
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                mImageView.setImageBitmap(imageBitmap);
            }

*/





} //end


