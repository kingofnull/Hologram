package io.github.froger.instamaterial;

import android.app.Application;

import dev.niekirk.com.instagram4android.Instagram4Android;
import timber.log.Timber;

/**
 * Created by froger_mcs on 05.11.14.
 */
public class InstaMaterialApplication extends Application {

    private Instagram4Android instagram;

    public Instagram4Android getInstagram(){
        return instagram;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
