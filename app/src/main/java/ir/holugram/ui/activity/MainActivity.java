package ir.holugram.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramLikeRequest;
import dev.niekirk.com.instagram4android.requests.InstagramTimelineFeedRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramTimelineFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramTimelineFeedResult;
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

        mFeedsMaxId = null;

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

    private void resetLoad(){
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
                if (!isLastPage && !isLoading) {
                    new UserFeedTask().execute();
                }
            }
        };

        rvFeed.addOnScrollListener(scrollListener);
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
    public void onReportClick(int feedItem) {
        FeedContextMenuManager.getInstance().hideContextMenu();
    }

    @Override
    public void onSharePhotoClick(int feedItem) {
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

                feedAdapter.notifyDataSetChanged();

            }

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
//            progressBar.setVisibility(View.GONE);

            feedAdapter.notifyDataSetChanged();

            isLoading = false;
        }


        // fetch user feed
        public void getUserFeed() {

            try {
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

}