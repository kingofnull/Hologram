package ir.holugram.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

import ir.holugram.R;


public class VideoPlayerActivity extends AppCompatActivity {
    SimpleExoPlayerView sV;
    TrackSelector trackSelector;
    SimpleExoPlayer player;
    DataSource.Factory dataSourceFactory;
    ExtractorsFactory extractorsFactory;
    MediaSource videoSource;


    String position = "0";

    private static final String TAG = "VideoPlayerActivityTag";
    private static final long CACHE_SIZE_BYTES = 200 * 1024 * 1024;
    private static final String USER_AGENT = "---";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Bundle b = getIntent().getExtras();
//        int id = b.getString("url");
//        String vidAddress = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
        Uri vidUri = Uri.parse(b.getString("url"));

        sV = findViewById(R.id.player);

        trackSelector = new DefaultTrackSelector();
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        sV.setPlayer(player);
        DefaultBandwidthMeter dBM=new DefaultBandwidthMeter();
        CacheDataSource.EventListener cEL=new CacheDataSource.EventListener() {
            @Override
            public void onCachedBytesRead(long cacheSizeBytes, long cachedBytesRead) {
                Log.d(TAG, "onCachedBytesRead[cachedBytes:" + cacheSizeBytes + ", cachedBytesRead: " + cachedBytesRead + "]");
            }
        };
        dataSourceFactory= new DefaultDataSourceFactory(this,"---");


//        extractorsFactory = buildDataSourceFactory(true, false);
        extractorsFactory=new DefaultExtractorsFactory() ;

        DataSource.Factory f=buildDataSourceFactory(true, dBM,cEL);
        videoSource = new ExtractorMediaSource(vidUri, f, extractorsFactory, null, null);

        player.prepare(videoSource);

        player.setPlayWhenReady(true);
    }

    //-------------------------------------------------------ANDROID LIFECYCLE---------------------------------------------------------------------------------------------

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop()...");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart()...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()...");
        player.stop();
        player.release();
    }


  /*  @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("position",position );
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onRestoreInstanceState(savedInstanceState);
        myClass=savedInstanceState.getParcelable("obj"));
    }*/

    DataSource.Factory buildDataSourceFactory(boolean cache, final DefaultBandwidthMeter bandwidthMeter, final CacheDataSource.EventListener listener) {

        if (!cache) {
            return new DefaultDataSourceFactory(this, bandwidthMeter,
                    buildHttpDataSourceFactory(bandwidthMeter));
        }

        return new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES);
                SimpleCache simpleCache = new SimpleCache(new File(getCacheDir(), "media_cache"), evictor);


                return new CacheDataSource(simpleCache, buildCachedHttpDataSourceFactory(bandwidthMeter).createDataSource(),
                        new FileDataSource(), new CacheDataSink(simpleCache, 10 * 1024 * 1024),
                        CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, listener);
            }
        };
    }


    private DefaultDataSource.Factory buildCachedHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(this, bandwidthMeter, buildHttpDataSourceFactory(bandwidthMeter));
    }

    HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(USER_AGENT, bandwidthMeter);
    }

}
