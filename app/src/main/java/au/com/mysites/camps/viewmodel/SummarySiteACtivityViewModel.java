package au.com.mysites.camps.viewmodel;

import android.arch.lifecycle.ViewModel;

import au.com.mysites.camps.models.Filters;

public class SummarySiteACtivityViewModel extends ViewModel {

    private boolean mIsSigningIn;
    private Filters mFilters;

    public SummarySiteACtivityViewModel() {
        mIsSigningIn = false;
        mFilters = Filters.getDefault();
    }

    public boolean getIsSigningIn() {
        return mIsSigningIn;
    }

    public void setIsSigningIn(boolean mIsSigningIn) {
        this.mIsSigningIn = mIsSigningIn;
    }

    public Filters getFilters() {
        return mFilters;
    }

    public void setFilters(Filters mFilters) {
        this.mFilters = mFilters;
    }
}

