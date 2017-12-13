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
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import ir.holugram.R;



public class VideoPlayerActivity extends AppCompatActivity {
    SimpleExoPlayerView sV;
    TrackSelector trackSelector;
    SimpleExoPlayer player;
    DataSource.Factory dataSourceFactory;
    ExtractorsFactory extractorsFactory;
    MediaSource videoSource;


    String position="0";

    private static final String TAG = "VideoPlayerActivityTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Bundle b = getIntent().getExtras();
//        int id = b.getString("url");
//        String vidAddress = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
        Uri vidUri = Uri.parse( b.getString("url"));

        sV=findViewById(R.id.player);

        trackSelector =new DefaultTrackSelector();
        player= ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        sV.setPlayer(player);

        dataSourceFactory= new DefaultDataSourceFactory(this,"---");

        extractorsFactory= new DefaultExtractorsFactory();

        videoSource = new ExtractorMediaSource(vidUri,dataSourceFactory, extractorsFactory, null, null);

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
}
