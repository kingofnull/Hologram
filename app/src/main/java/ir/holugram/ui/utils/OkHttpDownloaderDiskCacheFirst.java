package ir.holugram.ui.utils;

import android.net.Uri;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.OkHttpDownloader;

import java.io.IOException;

public class OkHttpDownloaderDiskCacheFirst extends OkHttpDownloader {
    public OkHttpDownloaderDiskCacheFirst(OkHttpClient client) {
        super(client);
    }

    @Override
    public Response load(Uri uri, int networkPolicy) throws IOException {
        Response responseDiskCache = null;
        try {
            responseDiskCache = super.load(uri, 1 << 2); //NetworkPolicy.OFFLINE
        } catch (Exception ignored){} // ignore, handle null later

        if (responseDiskCache == null || responseDiskCache.getContentLength()<=0){
            return  super.load(uri, networkPolicy); //user normal policy
        } else {
            return responseDiskCache;
        }

    }
}