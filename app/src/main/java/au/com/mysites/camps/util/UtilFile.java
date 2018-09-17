package au.com.mysites.camps.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import au.com.mysites.camps.R;

/**
 * Methods to read and write files
 */
public class UtilFile {
    private final static String TAG = UtilFile.class.getSimpleName();

    /**
     * Write text to a file. If append is true the text is appended,
     * if append is false the file is overwritten
     *
     * @param context context of calling routine
     * @param path    path for file to write too
     * @param name    name of file to write too
     * @param content text/content to be written to the file
     * @param append  If true the text is appended, if false the file is overwritten
     * @return boolean
     */

    public boolean write(Context context, String path, String name, String content, boolean append) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "write()");

        try {
            File file = new File(path, name);

            // If file does not exists, then create it
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOError e) {
                    e.printStackTrace();
                    Toast.makeText(context, context.getString(R.string.ERROR_File_error),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), append);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

            if (Debug.DEBUG_FILE) Log.d(TAG, "write file success()");
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.ERROR_File_error),
                    Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Reads a text file
     *
     * @param context  Context
     * @param filename The name of the file to read.
     * @param path     Path of the file
     * @return String of the file contents or null if it cannot read the file
     */

    public String read(Context context, String filename, String path) {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "read()");

        String line;
        StringBuilder text = new StringBuilder();

        try {
            FileReader fReader = new FileReader(path + "/" + filename);
            BufferedReader bReader = new BufferedReader(fReader);

            while ((line = bReader.readLine()) != null) {
                text.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, context.getString(R.string.ERROR_File_error));
            return null;
        }
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "file read success");
        return text.toString();
    }

    /**
     * Checks if external storage is available
     */
    public static boolean isExternalStorageAvailable() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "isExternalStorageAvailable()");

        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /*
     * Checks if external storage is available to read
     */
    public static boolean isExternalStorageReadable() {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "isExternalStorageReadable()");

        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * Simple method to convert InputStream to OutputStream.
     *
     * @param in    input stream
     * @param out   output stream
     * @throws IOException  stream error
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "copyStream()");

        byte[] buffer = new byte[1024];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            if (Debug.DEBUG_UTIL) Log.d(TAG, "File copy successful");

        } catch (IOException ioe) {
            Log.e(TAG, "IOException occurred.", ioe);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Simple method to copy a file.
     *
     * @param src   source file
     * @param dst   destination file
     */
    public static void copyFile(File src, File dst) throws IOException {
        if (Debug.DEBUG_METHOD_ENTRY_ACTIVITY) Log.d(TAG, "copyFile()");

        InputStream in;
        OutputStream out;

            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            copyStream(in, out);
    }
}
