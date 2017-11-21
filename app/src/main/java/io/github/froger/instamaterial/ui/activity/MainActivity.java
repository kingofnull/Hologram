package io.github.froger.instamaterial.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.view.animation.OvershootInterpolator;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramTimelineFeedRequest;
import dev.niekirk.com.instagram4android.requests.InstagramUserFeedRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramLoginResult;
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


    @BindView(R.id.rvFeed)
    RecyclerView rvFeed;
    @BindView(R.id.btnCreate)
    FloatingActionButton fabCreate;
    @BindView(R.id.content)
    CoordinatorLayout clContent;

    private FeedAdapter feedAdapter;

    private boolean pendingIntroAnimation;

    public Instagram4Android instagram;
    private Bundle savedInstanceState;

    private String mFeedsMaxId;

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

        SharedPreferences sharedPrefs = getSharedPreferences("USER_INFO", MODE_PRIVATE);
        SharedPreferences.Editor ed = sharedPrefs.edit();

        if (!sharedPrefs.contains("initialized")) {

            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

            setupFeed();

            if (savedInstanceState == null) {
                pendingIntroAnimation = true;
            } else {
                feedAdapter.updateItems(false);
            }

        } else {

            String email = sharedPrefs.getString("Username", "");
            String password = sharedPrefs.getString("Password", "");

            new Worker("Login").execute(email, password);
        }

    }

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

        new Worker("UserFeed").execute((String) null);

        EndlessRecyclerViewScrollListener scrollListener;

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {

                new Worker("UserFeed").execute((String) null);
                //feedAdapter.feedItems.add( new FeedAdapter.FeedItem(222, false));

/*                rvFeed.post(new Runnable() {
                    public void run() {
                        feedAdapter.notifyItemInserted(feedAdapter.feedItems.size() - 1);
                    }
                });*/
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
                case "Login":
                    doLogin(params[0], params[1]);
                    break;
                case "UserFeed":
                    getUserFeed();
                    break;
            }

           /* try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }*/


            // TODO: register the new account here.
            return true;
        }

        // for login
        public void doLogin(String email, String password) {

            Log.i("Hologram", "Try Login ...");
            if (instagram == null || !instagram.isLoggedIn()) {

                instagram = Instagram4Android.builder().username(email).password(password).build();
                instagram.setup();
            } else {
                Log.i("Hologram", "Already Logged in!");
                setupFeed();
                return;
            }

            try {

                InstagramLoginResult instagramLoginResult = instagram.login();
                Log.i("Hologram", "Login success!");

                ((InstaMaterialApplication) MainActivity.this.getApplication()).setInstagram(instagram);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        setupFeed();

                        if (savedInstanceState == null) {
                            pendingIntroAnimation = true;
                        } else {
                            feedAdapter.updateItems(false);
                        }
                    }
                });


            } catch (Exception e) {

                e.printStackTrace();
            }

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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().getDecorView().setBackgroundColor(Color.RED);
//stuff that updates ui

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
                        feedAdapter.add(new FeedAdapter.FeedItem(likeCount, isLiked, imgUrl));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                feedAdapter.notifyItemInserted(feedAdapter.feedItems.size() - 1);
                            }
                        });
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getWindow().getDecorView().setBackgroundColor(Color.GREEN);
//stuff that updates ui

                    }
                });

                mFeedsMaxId = feedResult.getNext_max_id();
//                    Thread.sleep(100);
//                }

               /* runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        feedAdapter.notifyItemInserted(feedAdapter.feedItems.size() - 1);
                    }
                });*/


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