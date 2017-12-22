package ir.holugram.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramLikeRequest;
import dev.niekirk.com.instagram4android.requests.InstagramTimelineFeedRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramTimelineFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramTimelineFeedResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramVideoVersions;
import ir.holugram.HolugramApplication;
import ir.holugram.R;
import ir.holugram.Utils;
import ir.holugram.ui.adapter.FeedAdapter;
import ir.holugram.ui.adapter.FeedItemAnimator;
import ir.holugram.ui.utils.EndlessRecyclerViewScrollListener;
import ir.holugram.ui.view.FeedContextMenu;
import ir.holugram.ui.view.FeedContextMenuManager;


public class MainActivity extends BaseDrawerActivity implements FeedAdapter.OnFeedItemClickListener,
        FeedContextMenu.OnFeedContextMenuItemClickListener {
    public static final String ACTION_SHOW_LOADING_ITEM = "action_show_loading_item";

    private static final int ANIM_DURATION_TOOLBAR = 300;
    private static final int ANIM_DURATION_FAB = 400;
    private static final int AFTER_LOGIN_SUCCESS = 500;
    EndlessRecyclerViewScrollListener scrollListener;

    @BindView(R.id.rvFeed)
    RecyclerView rvFeed;

    @BindView(R.id.feedProgressBar)
    ProgressBar progressBar;


    @BindView(R.id.btnCreate)
    FloatingActionButton fabCreate;
    @BindView(R.id.content)
    CoordinatorLayout clContent;

    @BindView(R.id.feedsSwipeRefreshLayout)
    SwipeRefreshLayout feedsSwipeRefreshLayout;

    private FeedAdapter feedAdapter;

    private boolean pendingIntroAnimation;

    public Instagram4Android instagram;
    private Bundle savedInstanceState;

    private String mFeedsMaxId = null;
//    LinearLayoutManager linearLayoutManager;

    boolean isLoading = false;
    boolean isLastPage = false;
    boolean isRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        this.savedInstanceState = savedInstanceState;

        this.instagram = ((HolugramApplication) this.getApplication()).getInstagram();

        Log.i("Hologram", "before launch!");

        if (instagram != null && instagram.isLoggedIn()) {

            Log.i("Hologram", "setup feed");
            setupFeed();

            if (savedInstanceState == null) {
                pendingIntroAnimation = true;
            } else {
                feedAdapter.updateItems(false);
            }
        } else {

            Log.i("Hologram", "launch");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, AFTER_LOGIN_SUCCESS);
        }

        Log.i("Hologram", "after launch!");
        progressBar.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
    }

    private void resetLoad() {
        isLoading = false;
        isLastPage = false;
        isRefresh = false;
        mFeedsMaxId = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AFTER_LOGIN_SUCCESS) {

            this.instagram = ((HolugramApplication) this.getApplication()).getInstagram();

            Log.i("Hologram", "set instagram " + instagram.toString());

            setupFeed();

            if (savedInstanceState == null) {
                pendingIntroAnimation = true;
            } else {
                feedAdapter.updateItems(false);
            }

        }

    }

    @SuppressLint("StaticFieldLeak")
    private void setupFeed() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                return 300;
            }
        };
        rvFeed.setLayoutManager(linearLayoutManager);


        feedAdapter = new FeedAdapter(this);
        feedAdapter.setOnFeedItemClickListener(this);
        rvFeed.setAdapter(feedAdapter);

        resetLoad();
        progressBar.setVisibility(View.VISIBLE);
        new UserFeedTask() {
            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
//                progressBar.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
//                rvFeed.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }.execute();


        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (!isLastPage) {
                    new UserFeedTask().execute();
                }
            }
        };

        rvFeed.addOnScrollListener(scrollListener);
        rvFeed.addOnScrollListener(FeedContextMenuManager.getInstance());
        rvFeed.setItemAnimator(new FeedItemAnimator());

        feedsSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onRefresh() {
                scrollListener.resetState();
                feedAdapter.feedItems.clear();
                feedAdapter.notifyDataSetChanged();
                isRefresh = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
                resetLoad();
                new UserFeedTask() {
                    @Override
                    protected void onPostExecute(Boolean aBoolean) {
                        super.onPostExecute(aBoolean);
                        feedsSwipeRefreshLayout.setRefreshing(false);
                        isRefresh = false;
                    }
                }.execute();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_SHOW_LOADING_ITEM.equals(intent.getAction())) {
            showFeedLoadingItemDelayed();
        }
    }

    private void showFeedLoadingItemDelayed() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                rvFeed.smoothScrollToPosition(0);
                feedAdapter.showLoadingView();
            }
        }, 500);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (pendingIntroAnimation) {
            pendingIntroAnimation = false;
            startIntroAnimation();
        }
        return true;
    }

    private void startIntroAnimation() {
        fabCreate.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));

        int actionbarSize = Utils.dpToPx(56);
        getToolbar().setTranslationY(-actionbarSize);
        getIvLogo().setTranslationY(-actionbarSize);
        getInboxMenuItem().getActionView().setTranslationY(-actionbarSize);

        getToolbar().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(300);
        getIvLogo().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(400);
        getInboxMenuItem().getActionView().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startContentAnimation();
                    }
                })
                .start();
    }

    private void startContentAnimation() {
        fabCreate.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setStartDelay(300)
                .setDuration(ANIM_DURATION_FAB)
                .start();
        feedAdapter.updateItems(true);
    }

    @Override
    public void onCommentsClick(View v, int position) {
        FeedAdapter.FeedItem item = feedAdapter.feedItems.get(position);
        final Intent intent = new Intent(this, CommentsActivity.class);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.MEDIA_ID, item.itemId);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[0]);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[1]);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onFeedBottomClick(View v, int position) {
        FeedAdapter.FeedItem item = feedAdapter.feedItems.get(position);
        TextView tv = (TextView) v.findViewById(R.id.ivFeedBottom);
        tv.setText(item.caption);
    }

    @Override
    public void onMoreClick(View v, int itemPosition) {
        FeedContextMenuManager.getInstance().toggleContextMenuFromView(v, itemPosition, this);
    }

    @Override
    public void onProfileClick(View v, int position) {
        FeedAdapter.FeedItem item = feedAdapter.feedItems.get(position);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        startingLocation[0] += v.getWidth() / 2;
        long userId = item.userId;
        UserProfileActivity.startUserProfileFromLocation(startingLocation, this, userId);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onDownloadClick(int pos) {
        FeedAdapter.FeedItem f = feedAdapter.feedItems.get(pos);
        String url = null;
        String fileName = null;

        if (f.feedData.getMedia_type() == 1) {
            url = f.feedData.getImage_versions2().getCandidates().get(0).getUrl();
//            fileName=f.feedData.getCaption()+".jpg";
            fileName=f.itemId+".jpg";
            downloadFromUrl(url,fileName,"در حال دانلود تصویر . . .");
        }else if(f.feedData.getMedia_type()==2){
            url = f.feedData.video_versions.get(0).getUrl();
//            fileName=f.feedData.getCaption()+".mp4";
            fileName=f.itemId+".mp4";
            downloadFromUrl(url,fileName,"در حال دانلود ویدیو . . .");
        }



        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onReportClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onSharePhotoClick(int pos) {
        FeedAdapter.FeedItem f=feedAdapter.feedItems.get(pos);
        String url="https://www.instagram.com/p/"+f.feedData.code;
        shareText("", url);
//        Toast.makeText(this, f.feedData.code, Toast.LENGTH_SHORT).show();
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onCopyShareUrlClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onCancelClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @OnClick(R.id.btnCreate)
    public void onTakePhotoClick() {
        int[] startingLocation = new int[2];
        fabCreate.getLocationOnScreen(startingLocation);
        startingLocation[0] += fabCreate.getWidth() / 2;
        TakePhotoActivity.startCameraFromLocation(startingLocation, this);
        overridePendingTransition(0, 0);
    }

    public void showLikedSnackbar(final FeedAdapter.FeedItem feedItem) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InstagramLikeRequest request = new InstagramLikeRequest(feedItem.itemId);
                try {
                    instagram.sendRequest(request);
                } catch (IOException e) {
                    Log.e("Hologram", Log.getStackTraceString(e));
                }

            }
        }).start();
        Snackbar.make(clContent, "Liked!", Snackbar.LENGTH_SHORT).show();
    }


    public class UserFeedTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... prams) {
            getUserFeed();

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoading = true;

            if (!isRefresh) {
//                progressBar.setVisibility(View.VISIBLE);
                rvFeed.post(new Runnable() {
                    public void run() {
                        feedAdapter.notifyDataSetChanged();
                    }
                });


            }

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
//            progressBar.setVisibility(View.GONE);

            rvFeed.post(new Runnable() {
                public void run() {
                    feedAdapter.notifyDataSetChanged();
                }
            });

            isLoading = false;
        }


        // fetch user feed
        public void getUserFeed() {

            Log.i("Hologram", "Read User Feeds");
                /*
                InstagramUserFeedRequest request=new InstagramUserFeedRequest(instagram.getUserId(), mFeedsMaxId, (System.currentTimeMillis()/1000)-86400*7);
                InstagramFeedResult result = instagram.sendRequest(request);
                List<InstagramFeedItem> items = result.getItems();
                Log.i("Hologram","Fetched Count: "+items.size());
                for (InstagramFeedItem item : items) {
                Log.i("Hologram", "Add Feeds" + item.getImage_versions2().getCandidates().get(0).getUrl());
                    feedAdapter.add(new FeedAdapter.FeedItem(item));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            feedAdapter.notifyItemInserted(feedAdapter.feedItems.size() - 1);
                        }
                    });


                    Thread.sleep(100);

                }
                mFeedsMaxId = result.getNext_max_id();
                */

            feedAdapter.showLoading(true);
            try {
                InstagramTimelineFeedResult feedResult = instagram.sendRequest(new InstagramTimelineFeedRequest(mFeedsMaxId, null));
                feedAdapter.showLoading(false);
                Log.i("Hologram", "User feeds loaded!");
                for (InstagramTimelineFeedItem item : feedResult.getFeed_items()) {
                    if (item.getMedia_or_ad() == null || item.getMedia_or_ad().getImage_versions2() == null ||
                            item.getMedia_or_ad().getImage_versions2().getCandidates() == null) {
                        Log.i("Hologram", "Add Feeds Skip");
                    } else {
                        Log.i("Hologram", "Add Feeds" + item.getMedia_or_ad().getImage_versions2().getCandidates().get(0).getUrl());
                        feedAdapter.add(new FeedAdapter.FeedItem(item.getMedia_or_ad()));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                feedAdapter.notifyItemInserted(feedAdapter.feedItems.size() - 1);
                            }
                        });


                        Thread.sleep(100);


                    }
                }
                mFeedsMaxId = feedResult.getNext_max_id();


                if (mFeedsMaxId == null) {
                    isLastPage = true;
                }
                Log.i("Hologram MaxId", mFeedsMaxId + "");


            } catch (final JsonMappingException e1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //(String)
                        Toast.makeText(getApplication(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                        Log.e("Hologram ", Log.getStackTraceString(e1));
                    }
                });
            } catch (Exception e) {
                Log.e("Hologram", Log.getStackTraceString(e));
            }


        }

    }

    public class LikePostTask extends AsyncTask<FeedAdapter.FeedItem, Void, Void> {
        @Override
        protected Void doInBackground(FeedAdapter.FeedItem... feedItems) {
            return null;
        }
    }

    @Override
    public void onVideoClick(View v, int position) {
        FeedAdapter.FeedItem feedItem = feedAdapter.feedItems.get(position);
        InstagramVideoVersions video = feedItem.feedData.video_versions.get(feedItem.feedData.video_versions.size() - 1);
        String videoUrl = video.getUrl();
//        Log.i("VideoUrl",videoUrl);
        showVideo(videoUrl);
    }

    public void showVideo(String Url) {
        Intent videoIntent = new Intent(this, VideoPlayerActivity.class);
//        videoIntent.putExtra("url", "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        videoIntent.putExtra("url", Url);
        startActivity(videoIntent);
    }

    public void downloadFromUrl(String url, String fileName,String caption) {
//        Log.e("DOWNLOAD-TRY",url);
//        Log.e("DOWNLOAD-TRY",fileName);
        url = url.replace(" ","%20");
        DownloadManager downloadManager = (DownloadManager) ((Activity) this).getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
//                .setTitle("Demo")
                .setDescription(caption)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        downloadManager.enqueue(request);

    }


    public void shareText(String subject, String text) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(i, getString(R.string.share_choose)));
        } catch (Exception e) {
            //e.toString();
            Log.e("ShareException", Log.getStackTraceString(e));
        }
    }

}