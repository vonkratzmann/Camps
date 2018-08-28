package au.com.mysites.camps.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import au.com.mysites.camps.R;

/**
 * Methods used in manipulating images
 */
public class OperationsImage {

    private final static String TAG = OperationsImage.class.getSimpleName();

    /**
     * Creates a collision resistance file in the public storage directory for pictures in a
     * sub-directory with the same name as the application name.
     *
     * @return newly created file
     * @throws IOException I/O error occurred
     */
    public static File createImageFile(Context context) throws IOException {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL) Log.d(TAG, "createImageFile()");

        // Create a unique image file name
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat(context.getString(R.string.Image_File_Name)).format(new Date());
        String imageFileName = timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //Creates a new empty file in the specified directory
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    /**
     * Saves a bitmap to a jpeg file.
     *
     * @param bitmap    bitmap to save to file
     * @param photoPath save to file pointed to by this pathname
     * @return true if no errors
     */
    private static boolean saveBitmapToFile(Bitmap bitmap, String photoPath) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_OPERATIONSIMAGE) Log.d(TAG, "saveBitmapToFile()");

        //convert the decoded bitmap to stream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(photoPath);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (
                IOException e) {
            Log.e(TAG, "File I/O error");
            return false;
        }
        return true;
    }

    /**
     * Scales the file in the photoPath into a bit map to suit the size of the imageView.
     * Checks valid photo and imageView first.
     * Does not write the scaled photo to the imageView, but returns a bitmap of the scaled image.
     *
     * @param photoPath path to photo to scale
     * @param imageView view to use to determine scale size
     * @return bitmap       the new scaled image
     */
    public static Bitmap scaleImageFile(String photoPath, ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_OPERATIONSIMAGE) Log.d(TAG, "scaleImageFile()");

        //Check we have valid photo and image View
        if ((imageView == null) || (photoPath == null) || photoPath.isEmpty()) {
            Log.e(TAG, "ERROR invalid photo or image");
            return null;
        }
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Decode the photo
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);

        // Determine how much to scale down the imageView
        int scaleFactor = calculateInSampleSize(bmOptions, targetW, targetH);

        // Decode the imageView file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        //generate the bitmap
        return BitmapFactory.decodeFile(photoPath, bmOptions);
    }

    /**
     * Calculates the scaling from an image to the required width and height
     *
     * @param options   bitmap with options
     * @param reqWidth  target width
     * @param reqHeight target height
     * @return the scaling required
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_OPERATIONSIMAGE) Log.d(TAG, "calculateInSampleSize()");

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Extract path from URI
     *
     * @param context    context of caller
     * @param contentUri URI of file
     * @return path of file
     */
    public static String getRealPathFromUri(Context context, Uri contentUri) {
        if (Debug.DEBUG_METHOD_ENTRY_UTIL_OPERATIONSIMAGE) Log.d(TAG, "getRealPathFromUri()");

        if (contentUri.getAuthority() != null)
            if (Debug.DEBUG_UTIL) Log.d(TAG, "contentUri.getAuthority(): " + contentUri.getAuthority());
        else
            if (Debug.DEBUG_UTIL) Log.d(TAG, "contentUri.getAuthority(): " + contentUri.getAuthority());

        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri,
                    projection,   // Which columns to return
                    null,   // WHERE clause; which rows to return (all rows)
                    null,   // WHERE clause selection arguments (none)
                    null);  // Order-by clause (ascending by name)

            if (cursor == null) { // Source is Dropbox or other similar local filepath
                if (Debug.DEBUG_UTIL)
                    Log.d(TAG, "getRealPathFromURI : cursor null. Path: " + contentUri.getPath());
                return contentUri.getPath();
            }

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Extracts bitmap from an ImageView and stores the bitmap in a new file.
     * The new file has a unique system generated name.
     *
     * @param imageView target image to be saved
     * @return new file with image, otherwise return null
     */
    public static File imageViewToNewFile(Context context, ImageView imageView) {
        if (Debug.DEBUG_METHOD_ENTRY_SITE) Log.d(TAG, "imageViewToNewFile()");

        File file;
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        if (drawable == null) return null;

        Bitmap bitmap = drawable.getBitmap();
        if (bitmap == null) return null;

        //create a new file
        try {
            file = OperationsImage.createImageFile(context);
        } catch (IOException e) {
            Log.e(TAG, " IOException");
            return null;
        }
        if (OperationsImage.saveBitmapToFile(bitmap, file.getAbsolutePath()))
            return file;
        else
            return null;
    }
}
