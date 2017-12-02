package ir.holugram;

import android.app.Application;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import dev.niekirk.com.instagram4android.Instagram4Android;
import ir.holugram.ui.utils.OkHttpDownloaderDiskCacheFirst;


/**
 * Created by froger_mcs on 05.11.14.
 */
public class HolugramApplication extends Application {


    private Instagram4Android instagram;

    public Instagram4Android getInstagram() {
        return instagram;
    }

    public void setInstagram(Instagram4Android instagram) {
        this.instagram = instagram;
    }


    @Override
    public void onCreate() {
//        Picasso picasso = new Picasso.Builder(this)
//                .downloader(new OkHttp3Downloader(this,Integer.MAX_VALUE))
//                .build();
//        picasso.setIndicatorsEnabled(true);
//        picasso.setLoggingEnabled(true);
//        Picasso.setSingletonInstance(picasso);

        instagram = new Instagram4Android(this);

        OkHttpClient okHttpClient = new OkHttpClient();

        okHttpClient.setCache(new Cache(getCacheDir(), 100 * 1024 * 1024)); //100 MB cache, use Integer.MAX_VALUE if it is too low
        OkHttpDownloader downloader = new OkHttpDownloaderDiskCacheFirst(okHttpClient);

        Picasso.Builder builder = new Picasso.Builder(this);

        builder.downloader(downloader);

        builder.indicatorsEnabled(true);

        Picasso built = builder.build();

        Picasso.setSingletonInstance(built);

        super.onCreate();
//        Timber.plant(new Timber.DebugTree());


    }
}
