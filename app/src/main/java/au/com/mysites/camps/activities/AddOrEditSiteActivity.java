package au.com.mysites.camps.activities;

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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import au.com.mysites.camps.R;
import au.com.mysites.camps.models.Site;
import au.com.mysites.camps.util.Constants;
import au.com.mysites.camps.util.Debug;
import au.com.mysites.camps.util.UtilDatabase;
import au.com.mysites.camps.util.UtilGeneral;
import au.com.mysites.camps.util.UtilImage;
import au.com.mysites.camps.util.UtilMap;
import au.com.mysites.camps.viewmodel.DetailSiteViewModel;

import static android.widget.Toast.makeText;
import static au.com.mysites.camps.util.Constants.TOASTTIMEFACILITIES;
import static au.com.mysites.camps.util.UtilFile.isExternalStorageAvailable;
import static au.com.mysites.camps.util.UtilImage.scaleImageFile;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

/**
 * Allows user to add a new site or edit an existing site
 */
public class AddOrEditSiteActivity extends AppCompatActivity implements
        EventListener<DocumentSnapshot>,
        View.OnClickListener {

    private final static String TAG = AddOrEditSiteActivity.class.getSimpleName();

    //Used to check if a field has changed
    private boolean mSiteHasChanged;

    // Captures the newly entered or edited data before saving to the Firestore database
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

    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_site_add_edit);
        toolbar = findViewById(R.id.add_edit_site_toolbar);
        setSupportActionBar(toolbar);

        /*Reset flag to say data has not changed, any editing this flag
         * is set to true. Used to check the user has done a save before exiting. */
        mSiteHasChanged = false;

        //Initialise Views and set up listeners
        initViews();

        // Check if this is editing an existing site and or adding a new site,
        // check for site ID from extras, provided by the calling activity
        Intent intent = getIntent();
        String siteId;
        if (intent.hasExtra(getString(R.string.intent_site_name))) {
            siteId = Objects.requireNonNull(intent.getExtras()).getString(getString(R.string.intent_site_name));

            //todo update to include case where activity restarted and partway through edit

            //this is an edit of an existing site
            mSite.setName(siteId);

            // Initialize Firestore
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

        //enable toggling of the facility presence, indicating if present or not present
        displayStatusOfAllFacilities(mSite);

        // View models
        mViewModel = ViewModelProviders.of(this).get(DetailSiteViewModel.class);
    }

    /**
     *  Set up a text watcher to monitor if the EditText fields have changed.
     */
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        // Do not use this one
        public void beforeTextChanged (CharSequence s,int start, int count, int after){
        }
        @Override
        // Do not use this one
        public void onTextChanged (CharSequence s,int start, int before, int count){
        }
        @Override
        public void afterTextChanged (Editable s){
            // Record the site has been changed
            mSiteHasChanged = true;
        }
    };

    /* Set up a specific text watcher to monitor if the name edit text field has changed
     * as the name of the site is displayed in the toolbar.
     */
    private final TextWatcher mNameTextWatcher = new TextWatcher() {
        @Override
        // Do not use this one
        public void beforeTextChanged (CharSequence s,int start, int count, int after){
        }
        @Override
        // Do not use this one
        public void onTextChanged (CharSequence s,int start, int before, int count){
        }
        @Override
        public void afterTextChanged (Editable s){
            // Record the site has been changed
            mSiteHasChanged = true;
            // Update the toolbar title
            if (s != null)
                toolbar.setTitle(s.toString());
        }
    };

    /**
     * Initialise views and set up listeners. There is a common listener used for the buttons
     * and there is a common TextWatcher attached to the EditFields to flag if any of the fields
     * have changed.
     */
    private void initViews() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "initViews()");

        //use a common TextChangeListener to monitor for changes to any EditText fields
        (mNameEditText = findViewById(R.id.add_site_name_text)).addTextChangedListener(mNameTextWatcher);
        (mStreetEditText = findViewById(R.id.add_site_street_text)).addTextChangedListener(mTextWatcher);
        (mCityEditText = findViewById(R.id.add_site_city_text)).addTextChangedListener(mTextWatcher);
        (mStateEditText = findViewById(R.id.add_site_state_text)).addTextChangedListener(mTextWatcher);
        (mLatitudeEditText = findViewById(R.id.add_site_map_coordinates_lat)).addTextChangedListener(mTextWatcher);
        (mLongitudeEditText = findViewById(R.id.add_site_map_coordinates_long)).addTextChangedListener(mTextWatcher);
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
            Log.w(TAG, "Error: ", e);
            return;
        }
        if (snapshot.exists()) {
            onSiteLoaded(Objects.requireNonNull(snapshot.toObject(Site.class)));
        }
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
            UtilDatabase.getImageAndDisplay(sitePhoto, mSitePhotoImageView);
        }
        // If we have a file name stored, display the thumbnail of the site
        String thumbnail = site.getThumbnail();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            UtilDatabase.getImageAndDisplay(thumbnail, mThumbnailImageView);
        }
        // Update UI to show if facility is present at site or not
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
     * saves the image files to Firebase storage.
     * Images files also saved in external storage so next time the site is loaded, they do not
     * have to be downloaded from Firebase storage.
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
        UtilDatabase.addOneSiteAndComments(mSite, this);

        return true;
    }

    /**
     * Checks a name has been entered, if not returns false.
     * Must have a name as used as the key for documents in the database.
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
     * Get the text input by user to the UI views and store in the Site instance
     * <p>
     * Called by {@link #getSite()}.
     */
    private void getSiteStoreTextFromViews() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSiteStoreTextFromViews()");

        // get site street
        mSite.setStreet(mStreetEditText.getText().toString());

        // get site city
        mSite.setCity(mCityEditText.getText().toString());

        // get site state
        mSite.setState(mStateEditText.getText().toString());

        /* get the site name, check if it has changed.
         * As siteName is used as the reference to a document in the Firestore database,
         * a new site name will create a new document and the old document has to be deleted */

        // If site name is empty, just copy over the new name from the EditText
        if (UtilGeneral.stringEmpty(mSite.getName())) {
            mSite.setName(mNameEditText.getText().toString());

            //If site name and the new name from the EditText are different, delete old document
        } else if (!mSite.getName().equals(mNameEditText.getText().toString())) {
            UtilDatabase.deleteSite(getString(R.string.collection_sites), mSite.getName());
            //save the new SiteName
            mSite.setName(mNameEditText.getText().toString());
        }
    }

    /**
     * Save site thumbnail and site images here, as images are not save to the remote database
     * when first selected or photo taken, in case user abandons the site editing.
     * <p>
     * Extracts the image from the thumbnail ImageView, stores this image in a newly created file
     * in external storage and saves the file in FireBase storage, then puts a link in the
     * Firestore database to where the file is saved in FireBase storage.
     * Repeats for the sitePhoto ImageView.
     * <p>
     * Upon successful save to database the files are not deleted, so that a future load
     * of the database will pull the files off the external storage if available,
     * rather than from the Firebase storage.
     * <p>
     * Checks external storage is available.
     * <p>
     * Called by {@link #getSite()}.
     */
    private void getSiteSaveImages() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "getSiteSaveImages()");

        // Check external storage is available
        if (!isExternalStorageAvailable()) {  // Warn the user.
            makeText(this, getString(R.string.ERROR_Storage_external_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }
        File file;
        // Store thumbnail image in a new file
        file = UtilImage.imageViewToNewFile(this, mThumbnailImageView);

        if (file != null) {
            //save new file to firestore storage
            UtilDatabase.saveFileFirestore(this, file, getString(R.string.firebase_collection_camps));

            //save path in storage to the database
            mSite.setThumbnail(file.getName());
        }
        // Store site photo image in a new file
        file = UtilImage.imageViewToNewFile(this, mSitePhotoImageView);

        if (file != null) {
            //save new file to firestore storage
            UtilDatabase.saveFileFirestore(this, file, getString(R.string.firebase_collection_camps));

            //save path in storage to the database
            mSite.setSitePhoto(file.getName());
        }
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
        if (!UtilMap.mapCheckLatitudeCoordinate(latitude)
                || !UtilMap.mapCheckLongitudeCoordinate(longitude)) {
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
     * If permission not available requests permission with the results returned
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
            photoFile = UtilImage.createImageFile(this);
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

            /* FileProvider allows secure sharing of file through a content:// URI by granting
             * temporary access to the file, which will be available for the receiver activity */

            // get authority for the file provider, this has to match the manifest authority
            String authority = getApplicationContext().getPackageName() + ".myfileprovider";
            Uri photoURI = FileProvider.getUriForFile(this,
                    authority,
                    photoFile);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePhotoIntent, Constants.RC_IMAGE_CAPTURE);
        }
    }

    /**
     * Checks have permission to access external storage.
     * If permission granted, calls the method {@link #selectPhotoUpdateImageViews()}.
     * If camera permission not available requests permission with the result
     * returned by {@link #onRequestPermissionsResult(int, String[], int[])}
     * <p>
     * Called from {@link #onClick(View)} which is a general handler of listeners from the buttons.
     */
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

    private void selectPhoto() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "selectPhoto()");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //request permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,}
                    , Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE_GETPHOTO);
        } else {
            //permission already granted
            selectPhotoUpdateImageViews();
        }
    }

    /**
     * Start an intent to select a photo
     */
    private void selectPhotoUpdateImageViews() {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "selectPhotoUpdateImageView()");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, Constants.RC_PICK_IMAGE);
        } else {       //warn the user
            makeText(this, getString(R.string.ERROR_Photo_not_available), Toast.LENGTH_SHORT).show();
            if (Debug.DEBUG_SITE)
                Log.d(TAG, "selectPhotoUpdateImageViews(): ERROR_Photo_not_available");
        }
    }

    /**
     * Callback result from the intents
     * Camera intent started in {@link #takePhotoDispatchIntent()}.
     *
     * @param requestCode calling intent request code
     * @param resultCode  results of intent
     * @param data        results form intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "onActivityResult()");

        switch (requestCode) {
            case Constants.RC_IMAGE_CAPTURE:
                /* Camera return with image file in mPhotoPath.
                 * Scales and then displays the photo in the thumbnail & site photo ImageViews.
                 * Files of the scaled images are not saved in Firestore storage at this stage,
                 * because the user may choose a different image or abandon the edit of the site. */

                if (resultCode != Activity.RESULT_OK || data == null) {    // Warn the user
                    makeText(this, getString(R.string.ERROR_Camera_unavailable), Toast.LENGTH_SHORT).show();
                    break;
                }
                if (updateImageViewsFromFile(mPhotoPath)) {  // Process the file
                    mSiteHasChanged = true;              // If Ok, set flag site details have changed
                } else { // Warn the user
                    makeText(this, getString(R.string.ERROR_Camera_unavailable), Toast.LENGTH_SHORT).show();
                }
                break;

            case Constants.RC_PICK_IMAGE:
                // For the chosen image, scale & display in thumbnail and sitePhoto ImageViews.
                if (resultCode != Activity.RESULT_OK || data == null) {    // Warn the user
                    makeText(this, getString(R.string.ERROR_Photo_not_available), Toast.LENGTH_SHORT).show();
                    if (Debug.DEBUG_SITE) Log.d(TAG, "Result code failure or data = null");
                    break;
                }
                // Extract the file path from the returned data
                String filePath = UtilImage.getRealPathFromUri(this, data.getData());

                if (filePath == null) {
                    makeText(this, getString(R.string.ERROR_Photo_not_available), Toast.LENGTH_SHORT).show();
                    if (Debug.DEBUG_SITE) Log.d(TAG, "filePath = null");
                    break;
                }
                if (updateImageViewsFromFile(filePath)) {  // Process the file
                    mSiteHasChanged = true;            // If Ok, set flag site details have changed
                } else { // Warn the user
                    makeText(this, getString(R.string.ERROR_Photo_not_available), Toast.LENGTH_SHORT).show();
                    if (Debug.DEBUG_SITE) Log.d(TAG, "Update image views failure");
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
    private boolean updateImageViewsFromFile(String photoPath) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "updateImageViewsFromFile()");

        // Scale thumbnail
        Bitmap bitmap = scaleImageFile(photoPath, mThumbnailImageView);
        if (bitmap == null) {
            if (Debug.DEBUG_SITE) Log.d(TAG, "Thumbnail image scaling failure");
            return false;
        }
        mThumbnailImageView.setImageBitmap(bitmap);

        // Scale site photo
        bitmap = scaleImageFile(photoPath, mSitePhotoImageView);
        if (bitmap == null) {
            if (Debug.DEBUG_SITE) Log.d(TAG, "Site photo image scaling failure");
            return false;
        }
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
     * at the site. Setups a listener, if the facility icon is touched, shows a short message
     * specifying the type of facility, toggles the status of the facility
     * ie present or not and updates the UI.
     *
     * @param site                     facility to be displayed
     * @param imageViewFacilityIcon    icon for this facility
     * @param imageViewFacilityPresent flag indicating if the facility is present or not
     * @param description              a short description of the facility
     * @param type                     the type of facility
     */
    private void displayFacility(final Site site,
                                 final ImageView imageViewFacilityIcon,
                                 final ImageView imageViewFacilityPresent,
                                 final int description, final Site.Facility type) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "displayFacility()");

        //Show if facility is present or not, by displaying a tick or cross
        imageViewFacilityPresent.setImageResource((site.checkIfFacilityPresent(type))
                ? R.mipmap.tick : R.mipmap.cross);

        //Set up listener for changes to the facility
        imageViewFacilityIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Short message to show type of facility
                UtilDatabase.shortToast(getString(description),
                        TOASTTIMEFACILITIES, AddOrEditSiteActivity.this);

                // Toggle status of the facility
                site.setFacility(type, !site.checkIfFacilityPresent(type));
                // update display
                imageViewFacilityPresent.setImageResource((site.checkIfFacilityPresent(type))
                        ? R.mipmap.tick : R.mipmap.cross);
                // record the facility has been changed
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

        mSiteHasChanged = true;
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
                    Toast.makeText(this, getString(R.string.ERROR_Storage_external_unavailable),
                            Toast.LENGTH_LONG).show();
                }
                break;

            case Constants.PERMISSIONS_REQUEST_EXTERNAL_STORAGE_GETPHOTO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission granted, continue to get the photo from storage
                    selectPhotoUpdateImageViews();
                } else {
                    // permission denied exit, tell user
                    Toast.makeText(this, getString(R.string.ERROR_Storage_external_unavailable),
                            Toast.LENGTH_LONG).show();
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
                selectPhoto();
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
                if (UtilMap.mapCheckLatLongSet(mSite)) {
                    // Ok, display map with a marker at this site's location by creating an intent
                    UtilMap.mapShow(mSite, this);

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


