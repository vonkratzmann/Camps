package au.com.mysites.camps.viewmodel;

import android.arch.lifecycle.ViewModel;

public class DetailSiteViewModel extends ViewModel {

    private boolean mIsSigningIn;

    public DetailSiteViewModel() {
        mIsSigningIn = false;
    }

    public boolean getIsSigningIn() {
        return mIsSigningIn;
    }

    public void setIsSigningIn(boolean mIsSigningIn) {
        this.mIsSigningIn = mIsSigningIn;
    }

}
