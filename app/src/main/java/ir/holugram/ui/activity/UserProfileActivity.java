package ir.holugram.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramGetUserInfoRequest;
import dev.niekirk.com.instagram4android.requests.InstagramUserFeedRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramSearchUsernameResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramUser;
import ir.holugram.HolugramApplication;
import ir.holugram.R;
import ir.holugram.ui.adapter.UserProfileAdapter;
import ir.holugram.ui.utils.CircleTransformation;
import ir.holugram.ui.utils.EndlessRecyclerViewScrollListener;
import ir.holugram.ui.view.RevealBackgroundView;


/**
 * Created by Miroslaw Stanek on 14.01.15.
 */
public class UserProfileActivity extends BaseDrawerActivity implements RevealBackgroundView.OnStateChangeListener {
    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";
    public static final String USER_ID = "current_user_id";

    private static final int USER_OPTIONS_ANIMATION_DELAY = 300;
    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();
    public Instagram4Android instagram;
    @BindView(R.id.vRevealBackground)
    RevealBackgroundView vRevealBackground;
    @BindView(R.id.rvUserProfile)
    RecyclerView rvUserProfile;
    @BindView(R.id.tlUserProfileTabs)
    TabLayout tlUserProfileTabs;
    @BindView(R.id.ivUserProfilePhoto)
    ImageView ivUserProfilePhoto;
    @BindView(R.id.tvProfileUserName)
    TextView tvProfileUserName;
    @BindView(R.id.tvProfileUserInfo)
    TextView tvProfileUserInfo;
    @BindView(R.id.tvProfileFullName)
    TextView tvProfileFullName;
    @BindView(R.id.tvProfilePosts)
    TextView tvProfilePosts;
    @BindView(R.id.tvProfileFollowers)
    TextView tvProfileFollowers;
    @BindView(R.id.tvProfileFollowing)
    TextView tvProfileFollowing;
    @BindView(R.id.vUserDetails)
    View vUserDetails;
    @BindView(R.id.btnFollow)
    Button btnFollow;
    @BindView(R.id.vUserStats)
    View vUserStats;
    @BindView(R.id.vUserProfileRoot)
    View vUserProfileRoot;
    @BindView(R.id.profileProgressBar)
    View progressBar;
    private int avatarSize;
    private String profilePhoto;
    private UserProfileAdapter userPhotosAdapter;
    private long userId;
    private String maxFeedId = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;

    public static void startUserProfileFromLocation(int[] startingLocation, Activity startingActivity, long userId) {
        Intent intent = new Intent(startingActivity, UserProfileActivity.class);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        intent.putExtra(USER_ID, userId);
        startingActivity.startActivity(intent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userId = getIntent().getLongExtra(USER_ID, 0);

        this.instagram = ((HolugramApplication) this.getApplication()).getInstagram();

        setProfileInfo();

        setupTabs();
        setupUserProfileGrid();
        setupRevealBackground(savedInstanceState);
    }

    private void setProfileInfo() {
        this.avatarSize = getResources().getDimensionPixelSize(R.dimen.user_profile_avatar_size);

        new Worker("Info").execute(new String[]{(userId + "")});


    }

    private void setupTabs() {
        tlUserProfileTabs.addTab(tlUserProfileTabs.newTab().setIcon(R.drawable.ic_grid_on_white));
        tlUserProfileTabs.addTab(tlUserProfileTabs.newTab().setIcon(R.drawable.ic_list_white));
        tlUserProfileTabs.addTab(tlUserProfileTabs.newTab().setIcon(R.drawable.ic_place_white));
        tlUserProfileTabs.addTab(tlUserProfileTabs.newTab().setIcon(R.drawable.ic_label_white));
    }

    private void setupUserProfileGrid() {
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        rvUserProfile.setLayoutManager(layoutManager);

        EndlessRecyclerViewScrollListener scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (!isLastPage && !isLoading) {
                    Log.i("Hologram", "get Feeds");
                    new UserProfileActivity.Worker("UserFeed").execute((String) null);
                }
            }
        };

        rvUserProfile.addOnScrollListener(scrollListener);


    }

    private void setupRevealBackground(Bundle savedInstanceState) {
        vRevealBackground.setOnStateChangeListener(this);
        userId = getIntent().getLongExtra(USER_ID, 0);
        if (savedInstanceState == null) {
            final int[] startingLocation = getIntent().getIntArrayExtra(ARG_REVEAL_START_LOCATION);
            vRevealBackground.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    vRevealBackground.getViewTreeObserver().removeOnPreDrawListener(this);
                    vRevealBackground.startFromLocation(startingLocation);
                    return true;
                }
            });
        } else {
            vRevealBackground.setToFinishedFrame();
            userPhotosAdapter.setLockedAnimations(true);
        }
    }

    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {
            rvUserProfile.setVisibility(View.VISIBLE);
            tlUserProfileTabs.setVisibility(View.VISIBLE);
            vUserProfileRoot.setVisibility(View.VISIBLE);
            userPhotosAdapter = new UserProfileAdapter(this);
            rvUserProfile.setAdapter(userPhotosAdapter);
            animateUserProfileOptions();
            animateUserProfileHeader();
            new Worker("UserFeed").execute((String) null);
        } else {
            tlUserProfileTabs.setVisibility(View.INVISIBLE);
            rvUserProfile.setVisibility(View.INVISIBLE);
            vUserProfileRoot.setVisibility(View.INVISIBLE);
        }
    }

    private void animateUserProfileOptions() {
        tlUserProfileTabs.setTranslationY(-tlUserProfileTabs.getHeight());
        tlUserProfileTabs.animate().translationY(0).setDuration(300).setStartDelay(USER_OPTIONS_ANIMATION_DELAY).setInterpolator(INTERPOLATOR);
    }

    private void animateUserProfileHeader() {
        vUserProfileRoot.setTranslationY(-vUserProfileRoot.getHeight());
        ivUserProfilePhoto.setTranslationY(-ivUserProfilePhoto.getHeight());
        vUserDetails.setTranslationY(-vUserDetails.getHeight());
        vUserStats.setAlpha(0);

        vUserProfileRoot.animate().translationY(0).setDuration(300).setInterpolator(INTERPOLATOR);
        ivUserProfilePhoto.animate().translationY(0).setDuration(300).setStartDelay(100).setInterpolator(INTERPOLATOR);
        vUserDetails.animate().translationY(0).setDuration(300).setStartDelay(200).setInterpolator(INTERPOLATOR);
        vUserStats.animate().alpha(1).setDuration(200).setStartDelay(400).setInterpolator(INTERPOLATOR).start();
    }

    public class Worker extends AsyncTask<String, Void, Boolean> {

        private String option;

        Worker(String option) {
            this.option = option;
        }

        // return current main instance
        public UserProfileActivity getMain() {
            return UserProfileActivity.this;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            switch (option) {
                case "Info":
                    getInfo();
                    break;
                case "UserFeed":
                    getPhotos();
                    break;
            }

            return true;
        }

        // get user info
        public void getInfo() {
            InstagramSearchUsernameResult result = null;
            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });

                Log.i("Hologram", "User Id" + userId);
                result = instagram.sendRequest(new InstagramGetUserInfoRequest(userId));
                final InstagramUser user = result.getUser();

                profilePhoto = user.getProfile_pic_url();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Picasso.with(UserProfileActivity.this)
                                .load(profilePhoto)
                                .placeholder(R.drawable.img_circle_placeholder)
                                .resize(avatarSize, avatarSize)
                                .centerCrop()
                                .transform(new CircleTransformation())
                                .into(ivUserProfilePhoto);

                        int follower = user.getFollower_count();
                        int following = user.getFollowing_count();
                        int post = user.getMedia_count();

                        String strFollower = follower > 1000 ? (follower / 1000) + "K" : follower + "";
                        String strFollowing = following > 1000 ? (following / 1000) + "K" : following + "";
                        String strPost = post > 1000 ? (post / 1000) + "K" : post + "";

                        tvProfileFullName.setText(user.getFull_name());
                        tvProfileUserName.setText("@" + user.getUsername());
                        tvProfileUserInfo.setText(user.getBiography());
                        tvProfileFollowers.setText(strFollower);
                        tvProfileFollowing.setText(strFollowing);
                        tvProfilePosts.setText(strPost);
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });

            } catch (IOException e) {

            }

        }

        // get post Photos
        public void getPhotos() {
            isLoading = true;
            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        userPhotosAdapter.setLockedAnimations(true);
                        //progressBar.setVisibility(View.VISIBLE);
                    }
                });

                Log.i("Hologram", "Read User Feeds");

                InstagramFeedResult result = instagram.sendRequest(new InstagramUserFeedRequest(userId, maxFeedId, 0L));
                List<InstagramFeedItem> items = result.getItems();

                if (result.getItems() == null) {
                    isLastPage = true;
                    isLoading = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //progressBar.setVisibility(View.INVISIBLE);
                            userPhotosAdapter.setLockedAnimations(false);
                        }
                    });
                    return;
                }

                for (InstagramFeedItem item : result.getItems()) {
                    Log.i("Hologram ->> user photo", userPhotosAdapter.getItemCount() + "");

                    // skip null photos
                    if (item.getImage_versions2() == null)
                        continue;

                    userPhotosAdapter.add(new UserProfileAdapter.FeedItem(item));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            userPhotosAdapter.setLockedAnimations(false);
                            userPhotosAdapter.notifyItemInserted(userPhotosAdapter.feedItems.size() - 1);
                        }
                    });

                    Thread.sleep(100);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //progressBar.setVisibility(View.INVISIBLE);
                        userPhotosAdapter.setLockedAnimations(false);
                    }
                });

                maxFeedId = result.getNext_max_id();
                if (maxFeedId == null) {
                    isLastPage = true;
                }
                Log.i("Hologram MaxId", maxFeedId + "");


            } catch (Exception e) {
                Log.e("Hologram", Log.getStackTraceString(e));
            }

            isLoading = false;
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
