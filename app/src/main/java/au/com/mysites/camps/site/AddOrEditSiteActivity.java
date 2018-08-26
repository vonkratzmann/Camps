package au.com.mysites.camps.site;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.model.Site;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.OperationsDatabase;
import au.com.mysites.camps.util.OperationsImage;
import au.com.mysites.camps.util.OperationsMap;
import au.com.mysites.camps.viewmodel.DetailSiteViewModel;

import static android.widget.Toast.makeText;
import static au.com.mysites.camps.util.Constants.TOASTTIMEFACILITIES;
import static au.com.mysites.camps.util.OperationsFile.isExternalStorageAvailable;
import static au.com.mysites.camps.util.OperationsImage.scaleImageFile;
import static au.com.mysites.camps.util.OperationsImage.scaleImageInputStream;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

/**
 * Allows user to add a new site or edit an existing site
 */
public class AddOrEditSiteActivity extends AppCompatActivity implements
        EventListener<DocumentSnapshot>, View.OnClickListener {
    private final static String TAG = AddOrEditSiteActivity.class.getSimpleName();

    //Used to check if a field has changed
    private boolean mSiteHasChanged;

    // site to capture the newly entered or edited site data before saving to the database
    private Site mSite = new Site();

    //setup the views
    private EditText mNameEditText;
    private EditText mStreetEditText;
    private EditText mCityEditText;
    private EditText mStateEditText;
    private EditText mLatitudeEditText;
    private EditText mLongitudeEditText;

    private ImageView dumppointImageView;
    private ImageView dumppointpresentImageView;
    private ImageView freeImageView;
    private ImageView freepresentImageView;
    private ImageView mobileImageView;
    private ImageView mobilepresentImageView;
    private ImageView playequipmentImageView;
    private ImageView playequipmentpresentImageView;
    private ImageView scenicImageView;
    private ImageView scenicpresentImageView;
    private ImageView showersImageView;
    private ImageView showerspresentImageView;
    private ImageView swimmingImageView;
    private ImageView swimmingpresentImageView;
    private ImageView toiletsImageView;
    private ImageView toiletspresentImageView;
    private ImageView tvreceptionImageView;
    private ImageView tvreceptionpresentImageView;
    private ImageView waterImageView;
    private ImageView waterpresentImageView;

    private ImageView mSitePhotoImageView;
    private ImageView mThumbnailImageView;

    //used to store path to store image from camera
    private String mPhotoPath;

    private FusedLocationProviderClient mFusedLocationClient;
    private AddressResultReceiver mResultReceiver;

    FirebaseFirestore mFirestore;
    private DocumentReference mSiteDocumentRef;
    private ListenerRegistration mSiteRegistration;

    Toolbar toolbar;

    private DetailSiteViewModel mViewModel;

    // set up a text watcher to monitor if the edit text fields have changed
    private TextWatcher mEditTextWatcher = new TextWatcher() {
        @Override
        // do not use this one
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        // do not use this one
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            // record the site has been changed
            mSiteHasChanged = true;
        }
    };

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_site_add_edit);
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        /*Reset flag to say site has not changed, any editing changes to site this flag
         * is set to true. Used to check the user has done a save before exiting. */
        mSiteHasChanged = false;

        // Check if this is editing an existing site and or adding a new site,
        // check site ID from extras, provided by the calling activity
        Intent intent = getIntent();
        String siteId;
        if (intent.hasExtra(getString(R.string.intent_site_name))) {
            siteId = Objects.requireNonNull(intent.getExtras()).getString(getString(R.string.intent_site_name));

            //todo update to include case where activity restarted and partway through edit

            //this is an edit of an existing site
            mSite.setName(siteId);

            // Initialize Firestore, views and listeners
            mFirestore = FirebaseFirestore.getInstance();

            // Get reference to this site in the database
            assert siteId != null;
            /* In {@link #onStart()} implements snapshot listener using mSiteDocumentRef
             * as the reference, the initial call to the callback {@link #onEvent()} as a result
             * of using addSnapshotListener() immediately creates a document snapshot with the
             * current contents of the single document. */
            mSiteDocumentRef = mFirestore
                    .collection(getString(R.string.collection_sites))
                    .document(siteId);
        }
        //Initialise Views and set up listeners
        initViews();

        //enable toggling of the facility presence, indicating if present or not present
        displayStatusOfAllFacilities(mSite);

        // View model
        mViewModel = ViewModelProviders.of(this).get(DetailSiteViewModel.class);
    }

    /**
     * Initialise views and set up listeners. There is a common listener used for the buttons
     * and there is a common TextWatcher attached to the EditFields to flag if any of the fields
     * have changed.
     */
    private void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "initViews()");

        //use a common TextChangeListener to monitor for changes to any EditText fields
        (mNameEditText = findViewById(R.id.add_site_name_text)).addTextChangedListener(mEditTextWatcher);
        (mStreetEditText = findViewById(R.id.add_site_street_text)).addTextChangedListener(mEditTextWatcher);
        (mCityEditText = findViewById(R.id.add_site_city_text)).addTextChangedListener(mEditTextWatcher);
        (mStateEditText = findViewById(R.id.add_site_state_text)).addTextChangedListener(mEditTextWatcher);
        (mLatitudeEditText = findViewById(R.id.add_site_map_coordinates_lat)).addTextChangedListener(mEditTextWatcher);
        (mLongitudeEditText = findViewById(R.id.add_site_map_coordinates_long)).addTextChangedListener(mEditTextWatcher);
        //where to display the image
        mSitePhotoImageView = findViewById(R.id.add_site_photo);
        mThumbnailImageView = findViewById(R.id.add_site_thumbnail);

        //Get view that requests a save of the new site
        findViewById(R.id.add_site_save).setOnClickListener(this);

        //Get button that takes a photo
        findViewById(R.id.add_site_button_take_photo).setOnClickListener(this);

        //Get button that selects a photo
        findViewById(R.id.add_site_button_grab_photo).setOnClickListener(this);

        //Get button that requests a photo delete
        findViewById(R.id.add_site_button_delete_photo).setOnClickListener(this);

        //Get button that gets latitude and longitude
        findViewById(R.id.add_site_button_get_location).setOnClickListener(this);

        //Get button that requests address look up for the latitude and longitude
        findViewById(R.id.add_site_button_get_address).setOnClickListener(this);

        //Get button that requests a map with location marker for latitude and longitude
        findViewById(R.id.add_site_button_show_map).setOnClickListener(this);

        // Do the facilities
        dumppointImageView = findViewById(R.id.dumppoint);
        dumppointpresentImageView = findViewById(R.id.dumppointpresent);
        freeImageView = findViewById(R.id.free);
        freepresentImageView = findViewById(R.id.freepresent);
        mobileImageView = findViewById(R.id.mobile);
        mobilepresentImageView = findViewById(R.id.mobilepresent);
        playequipmentImageView = findViewById(R.id.playequipment);
        playequipmentpresentImageView = findViewById(R.id.playequipmentpresent);
        scenicImageView = findViewById(R.id.scenic);
        scenicpresentImageView = findViewById(R.id.scenicpresent);
        showersImageView = findViewById(R.id.showers);
        showerspresentImageView = findViewById(R.id.showerspresent);
        swimmingImageView = findViewById(R.id.swimming);
        swimmingpresentImageView = findViewById(R.id.swimmingpresent);
        toiletsImageView = findViewById(R.id.toilets);
        toiletspresentImageView = findViewById(R.id.toiletspresent);
        tvreceptionImageView = findViewById(R.id.tvreception);
        tvreceptionpresentImageView = findViewById(R.id.tvreceptionpresent);
        waterImageView = findViewById(R.id.water);
        waterpresentImageView = findViewById(R.id.waterpresent);

        mFusedLocationClient = getFusedLocationProviderClient(this);
        mResultReceiver = new AddressResultReceiver(new Handler());
    }


    /**
     * Provides realtime updates with Cloud Firestore.
     * An initial call of this callback after using addSnapshopListener() immediately creates
     * a document snapshot with the current contents of the single document.
     * Then, each time the contents change, another call updates the document snapshot.
     */
    @Override
    public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onEvent()");

        if (e != null) {
            Log.w(TAG, "site:onEvent", e);
            return;
        }
        onSiteLoaded(Objects.requireNonNull(snapshot.toObject(Site.class)));
    }

    /**
     * Using the site loaded from the database, update the views on the UI.
     * Called from {@link #onEvent(DocumentSnapshot, FirebaseFirestoreException)}
     *
     * @param site site that has been loaded
     */
    private void onSiteLoaded(Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onSiteLoaded()");

        mSite = site;   //so we can access site from any method
        mNameEditText.setText(site.getName());
        mStreetEditText.setText(site.getStreet());
        mCityEditText.setText(site.getCity());
        mStateEditText.setText(site.getState());
        mLatitudeEditText.setText(site.getLatitude());
        mLongitudeEditText.setText(site.getLongitude());
        //todo finish
        // mRatingIndicator.setRating((float) site.getAvgRating());
        // mNumRatingsView.setText(getString(R.string.fmt_num_ratings, site.getNumRatings()));

        // If we have a file name stored, display the image of the site
        String sitePhoto = site.getSitePhoto();
        if (sitePhoto != null && !sitePhoto.isEmpty()) {
            OperationsDatabase.getImageAndDisplay(sitePhoto, mSitePhotoImageView);
        }
        // If we have a file name stored, display the thumbnail of the site
        String thumbnail = site.getThumbnail();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            OperationsDatabase.getImageAndDisplay(thumbnail, mThumbnailImageView);
        }
        // Update UI to show if facility is present as site or not
        dumppointpresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.DUMPPOINT))
                ? R.mipmap.tick : R.mipmap.cross);
        freepresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.FREE))
                ? R.mipmap.tick : R.mipmap.cross);
        mobilepresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.MOBILE))
                ? R.mipmap.tick : R.mipmap.cross);
        playequipmentpresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.PLAYEQUIPMENT))
                ? R.mipmap.tick : R.mipmap.cross);
        scenicpresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.SCENIC))
                ? R.mipmap.tick : R.mipmap.cross);
        showerspresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.SHOWERS))
                ? R.mipmap.tick : R.mipmap.cross);
        swimmingpresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.SWIMMING))
                ? R.mipmap.tick : R.mipmap.cross);
        toiletspresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.TOILETS))
                ? R.mipmap.tick : R.mipmap.cross);
        tvreceptionpresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.TVRECEPTION))
                ? R.mipmap.tick : R.mipmap.cross);
        waterpresentImageView.setImageResource((site.checkIfFacilityPresent(Site.Facility.WATER))
                ? R.mipmap.tick : R.mipmap.cross);

        //Reset as loading data from the Site instance to editText views, sets off the TextWatcher
        mSiteHasChanged = false;
    }

    /**
     * User has requested a save of the newly entered site or edit of existing site.
     * Gets the entered data for the new site or edited site, if entered data ok,
     * puts the data into a Site object, then saves it to the Firestore database and
     * saves the image files to theFirebase storage.
     * Uses external storag to store image files.
     * <p>
     * Does not reflect if database write was successful as database write is done asynchronously.
     * Called by a press of the "ok" button.
     *
     * @return true if entered data is valid
     */
    public boolean getSite() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSite()");

        if (!mSiteHasChanged) {
            // Warn user nothing entered into new site
            makeText(this, getString(R.string.ERROR_Nothing_entered), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!getSiteCheckNameEntered()) {
            // Name must be present to proceed, warn the user
            makeText(this, getString(R.string.No_Name_Entered), Toast.LENGTH_SHORT).show();
            return false;
        }
        getSiteStoreTextFromViews();

        getSiteSaveImages();

        // facilities already stored in site object, so need for anything

        if (!getSiteStoreLatAndLong()) {
            // If coordinates invalid, warn the user
            makeText(this, R.string.ERROR_Latitude_longitude_format_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        //add site to the database
        OperationsDatabase.addOneSiteAndComments(mSite, this);

        return true;
    }

    /**
     * Checks a name has been entered, if not returns false.
     * Must have a name asused as the key for documents in the database.
     * <p>
     * Called by {@link #getSite()}.
     *
     * @return Boolean  true if data successfully stored in the Site instance
     */
    boolean getSiteCheckNameEntered() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSiteCheckNameEntered()");

        boolean state = true;
        // check name of site has been entered
        String text = mNameEditText.getText().toString();

        if (text.equals("")) {
            state = false;
        }
        return state;
    }

    /**
     * Get the text input by user to the UI views and store in Site instance
     * <p>
     * Called by {@link #getSite()}.
     */
    private void getSiteStoreTextFromViews() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSiteStoreTextFromViews()");

        //get the name
        mSite.setName(mNameEditText.getText().toString());

        // get site street
        mSite.setStreet(mStreetEditText.getText().toString());

        // get site city
        mSite.setCity(mCityEditText.getText().toString());

        // get site state
        mSite.setCity(mStateEditText.getText().toString());
    }

    /**
     * Images are not save to the remote database when first selected or photo taken, in case user
     * abandons the photo selected or abandons the photo taken.
     * <p>
     * Gets the size of the thumbnail ImageView, scales the original image or photo to the size
     * of the ImageView, stores this scaled image in a newly created file in external storage and
     * saves the file in FireBase storage, then puts a link in the Firestore database to where
     * the file is saved in FireBase storage. Repeats for the sitePhoto ImageView.
     * <p>
     * Upon successful save to database the files are not deleted, so that a future load
     * of the database will pull the files off the external storage if available,
     * rather than from the Firebase storage.
     * <p>
     * Checks original photo exists and external storage is available.
     * <p>
     * Called by {@link #getSite()}.
     */
    private void getSiteSaveImages() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSiteSaveImages()");

        if (mPhotoPath == null) return;

        // Check external storage is available
        if (!isExternalStorageAvailable()) {
            // Warn the user.
            makeText(this, getString(R.string.ERROR_Storage_external_unavailable),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        File file;
        // Scale image and store it in a new file
        file = scaleImageFileToNewFile(mPhotoPath, mThumbnailImageView);

        if (file != null) {
            //save new file to firestore storage
            OperationsDatabase.saveFileFirestore(this, file, getString(R.string.firebase_storage_path));

            //save path in storage to the database
            mSite.setThumbnail(file.getName());
        }
        // Scale image and store it in a new file
        file = scaleImageFileToNewFile(mPhotoPath, mSitePhotoImageView);

        if (file != null) {
            //save new file to firestore storage
            OperationsDatabase.saveFileFirestore(this, file, getString(R.string.firebase_storage_path));

            //save path in storage to the database
            mSite.setSitePhoto(file.getName());
        }
    }

    /**
     * Takes an image file, scales the image to the size of the ImageView
     * and stores the scaled image in a new file. The new file has a unique
     * system generated name.
     * <p>
     * Called by {@link #getSiteSaveImages()}
     *
     * @param srcPhotoPath source file
     * @param imageView    target image to be used in sizing
     * @return new file with scaled image, otherwise return null
     */
    private File scaleImageFileToNewFile(String srcPhotoPath, ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "scaleImageFileToNewFile()");

        File file;

        Bitmap bitmap = scaleImageFile(srcPhotoPath, imageView);
        if (bitmap == null) return null;
        try {
            file = OperationsImage.createImageFile(this);
        } catch (IOException e) {
            Log.e(TAG, " IOexception");
            return null;
        }
        if (OperationsImage.saveBitmapToFile(bitmap, file.getAbsolutePath()))
            return file;
        else
            return null;
    }


    /**
     * Get latitude and longitude, if ok store in site instance.
     * If entered in the incorrect format or invalid data range
     * warns the user and return false.
     *
     * @return false if format or range invalid
     */
    private boolean getSiteStoreLatAndLong() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSiteStoreLatAndLong()");

        //Get GPS coordinates
        String latitude = mLatitudeEditText.getText().toString();
        String longitude = mLongitudeEditText.getText().toString();

        //Both empty is a valid condition
        if (latitude.isEmpty() && longitude.isEmpty()) {
            return true;
        }

        //If not empty, both must contain text
        if (latitude.isEmpty() || longitude.isEmpty()) {
            return false;
        }

        //check both have a valid format and values
        if (!OperationsMap.mapCheckLatitudeCoordinate(latitude)
                || !OperationsMap.mapCheckLongitudeCoordinate(longitude)) {
            return false;   //format or invalid value
        }
        mSite.setLatitude(latitude); //valid format and range
        mSite.setLongitude(longitude);
        return true;
    }

    /*
     * Dialog to prompt user for yes/no if they want to exit the activity
     * <p>
     * Only display prompt if the site has been changed
     */
    @Override
    public void onBackPressed() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onBackPressed()");

        //Check for changes
        if (mSiteHasChanged) {
            //Changed made so prompt user if they want to exit
            new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.warning)
                    .setTitle("Exit Without Saving Changes?")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else
            finish();
    }

    /**
     * Checks have permission to access camera, if yes take a photo.
     * If permission granted, calls the method {@link #takePhotoCheckStoragePermissions()}.
     * which takes the photo if access to storage is available.
     * If camera permission denied requests permission with the result
     * returned by {@link #onRequestPermissionsResult(int, String[], int[])}
     * <p>
     * Called from {@link #onClick(View)} which is a general handler of listeners from the buttons.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    private void takePhoto() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "takePhoto()");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            //request permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,}
                    , Constants.PERMISSIONS_REQUEST_CAMERA);
        } else {
            //permission already granted
            takePhotoCheckStoragePermissions();
        }
    }

    /**
     * Takes a photo stores it in the external public directory,
     * before taking a photo, checks external storage available ie mounted and
     * user has been granted write permission to access external storage.
     * If permission granted calls the method {@link #takePhotoDispatchIntent()}
     * The results are returned in the method {@link #onActivityResult(int, int, Intent)}
     * If permission denied requests permission with the results returned
     * in the method {@link #onRequestPermissionsResult(int, String[], int[])}.
     * <p>
     * Called from {@link #takePhoto()}.
     */
    private void takePhotoCheckStoragePermissions() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "takePhotoCheckStoragePermissions()");

        //check if external storage mounted and available
        if (!isExternalStorageAvailable()) {
            //not available
            makeText(this, getString(R.string.ERROR_Storage_external_unavailable),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Check permissions to access camera
        if (checkPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE_CAMERA)) {
            //permission granted
            takePhotoDispatchIntent();
        }
    }

    /**
     * Finally takes the photo making sure camera resource is available. Creates a unique file
     * on external storage ready to store the photo.
     * Photo is returned in {@link #onActivityResult}
     * <p>
     * Called from {@link #takePhotoCheckStoragePermissions()}.
     */
    private void takePhotoDispatchIntent() {
        Log.d(TAG, "takePhotoDispatchIntent()");

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePhotoIntent.resolveActivity(getPackageManager()) == null) {
            //no app can handle the intent, tell the user
            Toast.makeText(this, getString(R.string.ERROR_Camera_unavailable),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile = null;
        try {
            // Create the File in external storage where the photo should go
            // with a collision resistant file name.
            photoFile = OperationsImage.createImageFile(this);
            mPhotoPath = photoFile.getAbsolutePath();
        } catch (IOException ex) {
            // Error occurred while creating the File, tell the user
            Toast.makeText(this, getString(R.string.ERROR_File_Error), Toast.LENGTH_SHORT).show();
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            /* NOTE: configure the FileProvider in the manifest and
             * configure the eligible paths in the dedicated resource file,
             * res/xml/file_paths.xml. */

            // get authority for the file provider, this has to match the manifest authority
            String authority = getApplicationContext().getPackageName() + ".myfileprovider";
            Uri photoURI = FileProvider.getUriForFile(this,
                    authority,
                    photoFile);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePhotoIntent, Constants.REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Callback result from the intents
     * <p>
     * <p>
     * Camera intent started in {@link #takePhotoDispatchIntent()}.
     *
     * @param requestCode calling intent request code
     * @param resultCode  camera results
     * @param intent      calling intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onActivityResult()");

        switch (requestCode) {
            case Constants.REQUEST_IMAGE_CAPTURE:
                /* Camera return with image file in mPhotoPath.
                 * Scales and then displays the photo in the thumbnail & site photo ImageViews.
                 * Files of the scaled images are not saved in Firestore storage at this stage,
                 * because the user may choose a different image or abandon the edit of the site. */

                if (resultCode != Activity.RESULT_OK || intent == null) {    // Warn the user
                    makeText(this, getString(R.string.ERROR_Camera_unavailable), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (updateImageViewsFile(mPhotoPath)) {     //process the file
                    mSiteHasChanged = true;                 //Ok, set flag site details have changed
                } else { // Warn the user
                    makeText(this, getString(R.string.ERROR_Camera_unavailable), Toast.LENGTH_SHORT).show();
                }
                break;

            case Constants.PICK_IMAGE:
                // For the chosen image, scale & display in thumbnail and sitePhoto ImageViews.
                if (resultCode != Activity.RESULT_OK || intent == null) {    // Warn the user
                    makeText(this, getString(R.string.ERROR_Photos_not_available), Toast.LENGTH_SHORT).show();
                    break;
                }
                InputStream in = null;
                boolean result;
                try {
                    in = this.getContentResolver().openInputStream(intent.getData());
                    result = updateImageViewsStream(in);
                } catch (IOException e) {  // Warn the user
                    makeText(this, getString(R.string.ERROR_Photos_not_available), Toast.LENGTH_SHORT).show();
                    break;
                } finally {
                    try {
                        if (in != null) in.close();
                    } catch (IOException e) {  // Warn the user
                        Log.e(TAG, getString(R.string.ERROR_File_Error));
                    }
                }
                if (result) {
                    mSiteHasChanged = true;                 //Ok, set flag site details have changed
                } else { // Warn the user
                    makeText(this, getString(R.string.ERROR_Camera_unavailable), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Scales image from file in photoPath to thumbnail and sitePhoto ImageViews
     * then displays scaled image in thumbnail and sitePhoto ImageViews
     *
     * @param photoPath file containing image to be processed
     * @return true if operation successful
     */
    private boolean updateImageViewsFile(String photoPath) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "updateImageViewsFile()");

        Bitmap bitmap = scaleImageFile(photoPath, mThumbnailImageView);
        if (bitmap == null) return false;

        mThumbnailImageView.setImageBitmap(bitmap);

        //scale site photo
        bitmap = scaleImageFile(photoPath, mSitePhotoImageView);
        if (bitmap == null) return false;

        mSitePhotoImageView.setImageBitmap(bitmap);
        return true;
    }

    /**
     * Scales image from the InputStream to thumbnail and sitePhoto ImageViews
     * then displays scaled image in thumbnail and sitePhoto ImageViews
     *
     * @param inputStream stream containing image to be processed
     * @return true if operation successful
     */
    private boolean updateImageViewsStream(InputStream inputStream) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "updateImageStream()");

        Bitmap bitmap = scaleImageInputStream(inputStream, mThumbnailImageView);
        if (bitmap == null) return false;

        mThumbnailImageView.setImageBitmap(bitmap);

        //scale site photo
        bitmap = scaleImageInputStream(inputStream, mSitePhotoImageView);
        if (bitmap == null) return false;

        mSitePhotoImageView.setImageBitmap(bitmap);
        return true;
    }

    public void displayStatusOfAllFacilities(final Site site) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "displayStatusOfAllFacilities()");

        displayFacility(site, dumppointImageView, dumppointpresentImageView, R.string.dumppoint
                , Site.Facility.DUMPPOINT);
        displayFacility(site, freeImageView, freepresentImageView, R.string.free
                , Site.Facility.FREE);
        displayFacility(site, mobileImageView, mobilepresentImageView, R.string.mobile
                , Site.Facility.MOBILE);
        displayFacility(site, playequipmentImageView, playequipmentpresentImageView
                , R.string.playequipment, Site.Facility.PLAYEQUIPMENT);
        displayFacility(site, scenicImageView, scenicpresentImageView, R.string.scenic
                , Site.Facility.SCENIC);
        displayFacility(site, showersImageView, showerspresentImageView, R.string.showers
                , Site.Facility.SHOWERS);
        displayFacility(site, swimmingImageView, swimmingpresentImageView, R.string.swimming
                , Site.Facility.SWIMMING);
        displayFacility(site, toiletsImageView, toiletspresentImageView, R.string.toilets
                , Site.Facility.TOILETS);
        displayFacility(site, tvreceptionImageView, tvreceptionpresentImageView, R.string.tvreception
                , Site.Facility.TVRECEPTION);
        displayFacility(site, waterImageView, waterpresentImageView, R.string.water
                , Site.Facility.WATER);
    }

    /**
     * Displays all facilities on the UI, indicating if the facility is present or not
     * at the site. Setups a listener, if the site icon is touched toggles the status of the site
     * being present or not and updates the UI.
     *
     * @param site             site to be displayed
     * @param imageViewIcon    icon for this facility
     * @param imageViewPresent flag indicating if the facility is present or not
     * @param description      a short description of the facility
     * @param type             the type of facility
     */
    private void displayFacility(final Site site,
                                 final ImageView imageViewIcon, final ImageView imageViewPresent,
                                 final int description, final Site.Facility type) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "displayFacility()");

        //Show if facility is present or not, by displaying a tick or cross
        imageViewPresent.setImageResource((site.checkIfFacilityPresent(type))
                ? R.mipmap.tick : R.mipmap.cross);

        //Set up listener for changes to the facility
        imageViewIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Free icon pressed
                // Short message to show type of facility
                OperationsDatabase.shortToast(getString(description)
                        , TOASTTIMEFACILITIES, AddOrEditSiteActivity.this);
                // Toggle status of the facility
                site.setFacility(type, !site.checkIfFacilityPresent(type));
                // update display
                imageViewPresent.setImageResource((site.checkIfFacilityPresent(type))
                        ? R.mipmap.tick : R.mipmap.cross);
                // record the site has been changed
                mSiteHasChanged = true;
            }
        });
    }

    /**
     * Get current location, but first checks have permission to access location resources.
     * <p>
     * Called from {@link #onClick(View)} which is general handler for button listeners
     * <p>
     * If permission granted, calls the method {@link #getLocationFusedProviderClient()}
     * to continue processing.
     * If permission denied requests the permission, with the result returned in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private void getLocation() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getLocation()");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //request permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,},
                    Constants.PERMISSIONS_REQUEST_LOCATION);
        } else {
            //permission already granted
            getLocationFusedProviderClient();
        }
    }

    /**
     * Gets a fused location client, which is an API from Google Play Services
     * adds listeners for success and failure.
     * If success calls {@link #getLocationDisplayCoordinates(Location)}
     * which displays longitude and latitude.
     * If fails displays a message to the user.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void getLocationFusedProviderClient() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getLocationFusedProviderClient()");

        //sets up fused location client, which is API from Google Play Services
        mFusedLocationClient = getFusedLocationProviderClient(this);
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                getLocationDisplayCoordinates(location);
                            } else {
                                Toast.makeText(getApplicationContext()
                                        , getString(R.string.ERROR_GPS_Null), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext()
                                    , getString(R.string.ERROR_GPS_Error), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, getString(R.string.ERROR_Security_exception),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Displays latitude and longitude for the supplied location
     * Display format is "00:00::00.0000"
     *
     * @param location Display location coordinates
     */
    private void getLocationDisplayCoordinates(Location location) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getLocationDisplayCoordinates()");

        TextView textView = findViewById(R.id.add_site_map_coordinates_lat);
        textView.setText(Location.convert(location.getLatitude(), Location.FORMAT_SECONDS));

        textView = findViewById(R.id.add_site_map_coordinates_long);
        textView.setText(Location.convert(location.getLongitude(), Location.FORMAT_SECONDS));
    }


    /**
     * Clears photos from the display and clears links in the database which point to
     * where the image files are stored in Firebase storage.
     * <p>
     * The image files are not deleted here as the user may abandon this editing session
     * and not save the changes.
     * <p>
     * Called by the generic {@link #onClick(View)} handler
     */
    private void deletePhotos() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "deletePhotos()");

        // Clear the images from the views
        mThumbnailImageView.setImageDrawable(null);
        mSitePhotoImageView.setImageDrawable(null);

        // Clear the links to where the images files are stored
        mSite.setThumbnail(null);
        mSite.setSitePhoto(null);
    }

    /**
     * Checks we have permissions
     * If we have permission return true.
     * If permission not granted call {@link #requestPermissions(String[], int)}, and
     * return false.
     *
     * @param permissions Permissions being checked
     * @param requestId   Used to identify calling method
     */
    private boolean checkPermissions(String[] permissions, int requestId) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "checkPermissions()");
        //Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //no access, request permissions
            ActivityCompat.requestPermissions(this, permissions, requestId);
            return false;
        } else {
            //permission already granted
            return true;
        }
    }

    /**
     * Handles request permission results
     *
     * @param requestCode  code to allow identification of original request permissions
     * @param permissions  permissions requested
     * @param grantResults results of permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onRequestPermissionsResult()");

        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted, take a photo
                    takePhotoCheckStoragePermissions();
                } else {
                    // permission denied exit, tell user
                    Toast.makeText(this, getString(R.string.ERROR_Camera_unavailable)
                            , Toast.LENGTH_LONG).show();
                }
                break;
            case Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted, continue to take a photo
                    takePhotoDispatchIntent();
                } else {
                    // permission denied exit, tell user
                    Toast.makeText(this, getString(R.string.ERROR_Storage_external_unavailable)
                            , Toast.LENGTH_LONG).show();
                }
                break;

            case Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE_GETPHOTO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted, continue to get the photo from storage
                    getSiteSaveImages();
                } else {
                    // permission denied exit, tell user
                    Toast.makeText(this, getString(R.string.ERROR_Storage_external_unavailable)
                            , Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onResume()");
    }

    /**
     *
     */
    @Override
    public void onStart() {
        super.onStart();
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onStart()");

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn();
            return;
        }
        /* Implement snapshot listener, the initial call of the callback {@link #onEvent()}
         * as a result of using addSnapshotListener() immediately creates a document snapshot with the
         * current contents of the single document.
         * Then, each time the contents change, another call updates the document snapshot.
         * Results are returned in the overridden {@link #onEvent} method. */
        if (mSiteDocumentRef != null) {
            //if this a new site mSiteDocumentRef will be null, so don't start the listener
            mSiteRegistration = mSiteDocumentRef.addSnapshotListener(this);
        }
    }

    /**
     *
     */
    @Override
    public void onStop() {
        super.onStop();
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onStop()");

        if (mSiteRegistration != null) {
            mSiteRegistration.remove();
            mSiteRegistration = null;
        }
    }

    private boolean shouldStartSignIn() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "shouldStartSignIn()");

        return (!mViewModel.getIsSigningIn() && FirebaseAuth.getInstance().getCurrentUser() == null);
    }

    private void startSignIn() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "startSignIn()");

        // Sign in with FirebaseUI
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build()))
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(intent, Constants.RC_SIGN_IN);
        mViewModel.setIsSigningIn(true);
    }

    /**
     * General handler for onClick listener
     *
     * @param v source of click
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onClick(View v) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onClick");

        switch (v.getId()) {
            case R.id.add_site_save:
                /* Flag mSiteHasBeenChanged is set to false
                 * if no errors during processing by GetNewSiteIfOkSave().
                 * Does not reflect if write to database was successful
                 * as database write is done asynchronously. */
                if (getSite())
                    mSiteHasChanged = false;
                break;

            case R.id.add_site_button_take_photo:
                takePhoto();
                break;

            case R.id.add_site_button_grab_photo:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null)
                    startActivityForResult(intent, Constants.PICK_IMAGE);
                break;

            case R.id.add_site_button_delete_photo:
                deletePhotos();
                break;

            case R.id.add_site_button_get_location:
                getLocation();
                break;

            case R.id.add_site_button_get_address:
                //getAddress();
                break;

            case R.id.add_site_button_show_map:
                // Checks latitude and longitude are set,
                if (OperationsMap.mapCheckLatLongSet(mSite)) {
                    // Ok, display map with a marker at this site's location by creating an intent
                    OperationsMap.mapShow(mSite, this);

                } else {
                    //warn the user not valid
                    makeText(this, getString(R.string.ERROR_GPS_Coordinates),
                            Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;
        }
    }

    /**
     * Handles results from the Geocoder
     */
    class AddressResultReceiver extends ResultReceiver {
        @SuppressLint("RestrictedApi")
        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        // private static final String TAG = "AddressResultReceiver";

        /*     @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onReceiveResult()");
            if (resultData == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.address_fail)
                        ,Toast.LENGTH_SHORT).show();
                return;
            }

            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(Constant.RESULT_DATA_KEY);
            if (mAddressOutput == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.address_fail)
                        ,Toast.LENGTH_SHORT).show();
                mAddressOutput = "";
            }
            if (resultCode == Constant.SUCCESS_RESULT) {
                displayAddress(mAddressOutput);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.address_fail)
                        ,Toast.LENGTH_SHORT).show();
            }
        }*/
    }
}


