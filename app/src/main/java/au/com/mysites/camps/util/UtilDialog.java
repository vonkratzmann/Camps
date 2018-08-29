package au.com.mysites.camps.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import au.com.mysites.camps.R;

/**
 * Various dialog methods
 */
public class UtilDialog {
    private static final String TAG = UtilDialog.class.getSimpleName();

    public static void showProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog == null) return;

        progressDialog.setMessage(AppContextProvider.getContext().getString(R.string.loading));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    public static void hideProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public static void hideKeyboard(View view) {
        final InputMethodManager imm = (InputMethodManager) AppContextProvider.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}

