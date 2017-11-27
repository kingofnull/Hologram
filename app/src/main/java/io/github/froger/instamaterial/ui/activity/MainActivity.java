package io.github.froger.instamaterial.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramTimelineFeedRequest;
import dev.niekirk.com.instagram4android.requests.InstagramUserFeedRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramTimelineFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramTimelineFeedResult;
import io.github.froger.instamaterial.InstaMaterialApplication;
import io.github.froger.instamaterial.R;
import io.github.froger.instamaterial.Utils;
import io.github.froger.instamaterial.ui.adapter.FeedAdapter;
import io.github.froger.instamaterial.ui.adapter.FeedItemAnimator;
import io.github.froger.instamaterial.ui.utils.EndlessRecyclerViewScrollListener;
import io.github.froger.instamaterial.ui.view.FeedContextMenu;
import io.github.froger.instamaterial.ui.view.FeedContextMenuManager;


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

    private FeedAdapter feedAdapter;

    private boolean pendingIntroAnimation;

    public Instagram4Android instagram;
    private Bundle savedInstanceState;

    private String mFeedsMaxId;
    LinearLayoutManager linearLayoutManager;

    boolean isLoading=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        this.savedInstanceState = savedInstanceState;

        this.instagram = ((InstaMaterialApplication) this.getApplication()).getInstagram();

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
        progressBar.getLayoutParams().height=ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AFTER_LOGIN_SUCCESS) {

            this.instagram = ((InstaMaterialApplication) this.getApplication()).getInstagram();

            Log.i("Hologram", "set instagram " + instagram.toString());

            setupFeed();

            if (savedInstanceState == null) {
                pendingIntroAnimation = true;
            } else {
                feedAdapter.updateItems(false);
            }

        }

    }

    private void setupFeed() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this) {
//            @Override
//            protected int getExtraLayoutSpace(RecyclerView.State state) {
//                return 300;
//            }
        };
        rvFeed.setLayoutManager(linearLayoutManager);



        feedAdapter = new FeedAdapter(this);
        feedAdapter.setOnFeedItemClickListener(this);
        rvFeed.setAdapter(feedAdapter);

        new Worker("UserFeed") {
            @Override
            protected void onPostExecute(Boolean success) {
                super.onPostExecute(success);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        progressBar.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
                        rvFeed.setVisibility(View.VISIBLE);
                    }
                });
            }
        }.execute((String) null);



        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if(!isLoading){
                    new Worker("UserFeed").execute((String) null);
                }
            }
        };

        rvFeed.addOnScrollListener(scrollListener);
        rvFeed.setItemAnimator(new FeedItemAnimator());
    }

    public void loadNextDataFromApi(int offset) {

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

        final Intent intent = new Intent(this, CommentsActivity.class);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.ARG_DRAWING_START_LOCATION, startingLocation[1]);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    public void onMoreClick(View v, int itemPosition) {
        FeedContextMenuManager.getInstance().toggleContextMenuFromView(v, itemPosition, this);
    }

    @Override
    public void onProfileClick(View v) {
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        startingLocation[0] += v.getWidth() / 2;
        UserProfileActivity.startUserProfileFromLocation(startingLocation, this);
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

    public void showLikedSnackbar() {
        Snackbar.make(clContent, "Liked!", Snackbar.LENGTH_SHORT).show();
    }

    public class Worker extends AsyncTask<String, Void, Boolean> {

        private final String option;

        Worker(String option) {
            this.option = option;
        }

        // return current main instance
        public MainActivity getMain() {
            return MainActivity.this;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            switch (option) {
                case "UserFeed":
                    getUserFeed();
                    break;
            }

            // TODO: register the new account here.
            return true;
        }


        // fetch user feed
        public void getUserFeed() {
            try {

                Log.i("Hologram", "Read User Feeds");

                InstagramFeedResult result = instagram.sendRequest(new InstagramUserFeedRequest(instagram.getUserId(), null, 0L));
                List<InstagramFeedItem> items = result.getItems();

//                String maxId = null;
//                for (int i = 0; i < 4; i++) {
//                    if (i > 0) {
//                        System.out.println("MAX ID: " + maxId);
//                    }

                isLoading=true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });

                InstagramTimelineFeedResult feedResult = instagram.sendRequest(new InstagramTimelineFeedRequest(mFeedsMaxId, null));
                Log.i("Hologram", "User feeds loaded!");
                for (InstagramTimelineFeedItem item : feedResult.getFeed_items()) {
                    if (item.getMedia_or_ad() == null || item.getMedia_or_ad().getImage_versions2() == null ||
                            item.getMedia_or_ad().getImage_versions2().getCandidates() == null) {
                        Log.i("Hologram", "Add Feeds Skip");
                    } else {
                        Log.i("Hologram", "Add Feeds" + item.getMedia_or_ad().getImage_versions2().getCandidates().get(0).getUrl());

                        String imgUrl = item.getMedia_or_ad().getImage_versions2().getCandidates().get(0).getUrl();
                        int likeCount = item.getMedia_or_ad().getLike_count();
                        boolean isLiked = item.getMedia_or_ad().isHas_liked();

//                            feedAdapter.feedItems.add(new FeedAdapter.FeedItem(likeCount, isLiked, imgUrl));
//                        feedAdapter.add(new FeedAdapter.FeedItem(likeCount, isLiked, imgUrl));
//                        scrollListener.visibleThreshold=feedResult.getFeed_items().size();
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



                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });

                isLoading=false;
                mFeedsMaxId = feedResult.getNext_max_id();


            } catch (Exception e) {
                Log.e("Hologram", Log.getStackTraceString(e));
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {

            } else {

            }
        }

        @Override
        protected void onCancelled() {

        }
    }
}