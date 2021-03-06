package ir.holugram.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import butterknife.BindView;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.requests.InstagramGetMediaCommentsRequest;
import dev.niekirk.com.instagram4android.requests.InstagramPostCommentRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramComment;
import dev.niekirk.com.instagram4android.requests.payload.InstagramGetMediaCommentsResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramPostCommentResult;
import ir.holugram.HolugramApplication;
import ir.holugram.R;
import ir.holugram.Utils;
import ir.holugram.ui.adapter.CommentsAdapter;
import ir.holugram.ui.utils.EndlessRecyclerViewScrollListener;
import ir.holugram.ui.view.SendCommentButton;

/**
 * Created by froger_mcs on 11.11.14.
 */
public class CommentsActivity extends BaseDrawerActivity implements SendCommentButton.OnSendClickListener, CommentsAdapter.OnCommentClickListener {
    public static final String ARG_DRAWING_START_LOCATION = "arg_drawing_start_location";
    public static final String MEDIA_ID = "arg_media_id";
    public Instagram4Android instagram;
    @BindView(R.id.contentRoot)
    LinearLayout contentRoot;
    @BindView(R.id.rvComments)
    RecyclerView rvComments;
    @BindView(R.id.llAddComment)
    LinearLayout llAddComment;
    @BindView(R.id.etComment)
    EditText etComment;
    @BindView(R.id.btnSendComment)
    SendCommentButton btnSendComment;
    @BindView(R.id.commentProgressBar)
    ProgressBar progressBar;
    boolean isLoading = false;
    boolean isLastPage = false;
    private CommentsAdapter commentsAdapter;
    private int drawingStartLocation;
    private long mediaId;
    private String maxCommentId = null;
    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        this.instagram = ((HolugramApplication) this.getApplication()).getInstagram();

        setupComments();
        setupSendCommentButton();

        drawingStartLocation = getIntent().getIntExtra(ARG_DRAWING_START_LOCATION, 0);
        mediaId = getIntent().getLongExtra(MEDIA_ID, 0);
        if (savedInstanceState == null) {
            new Worker("Comments").execute((String) null);
        }

    }

    private void setupComments() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(linearLayoutManager);
        rvComments.setHasFixedSize(true);

        commentsAdapter = new CommentsAdapter(this);
        rvComments.setAdapter(commentsAdapter);

        rvComments.setOverScrollMode(View.OVER_SCROLL_NEVER);

        EndlessRecyclerViewScrollListener scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (!isLastPage && !isLoading) {
                    new Worker("Comments").execute((String) null);
                }
            }
        };

        rvComments.addOnScrollListener(scrollListener);
    }

    private void setupSendCommentButton() {
        btnSendComment.setOnSendClickListener(this);
    }

    private void animateContent() {
        //commentsAdapter.updateItems();
        llAddComment.animate().translationY(0)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(200)
                .start();
    }

    @Override
    public void onBackPressed() {
        ViewCompat.setElevation(getToolbar(), 0);
        contentRoot.animate()
                .translationY(Utils.getScreenHeight(this))
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        CommentsActivity.super.onBackPressed();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    @Override
    public void onSendClickListener(View v) {
        if (validateComment()) {
            //commentsAdapter.addItem();
            String comment = etComment.getText().toString();
            new Worker("Send").execute(new String[]{comment});
            commentsAdapter.setAnimationsLocked(false);
            commentsAdapter.setDelayEnterAnimation(false);
            //rvComments.smoothScrollBy(0, rvComments.getChildAt(0).getHeight() * commentsAdapter.getItemCount());

            etComment.setText(null);
            btnSendComment.setCurrentState(SendCommentButton.STATE_DONE);
        }
    }

    @Override
    public void onProfileClick(View v, int position) {
        CommentsAdapter.CommentItem item = commentsAdapter.commentItems.get(position);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        startingLocation[0] += v.getWidth() / 2;
        long userId = item.userId;
        UserProfileActivity.startUserProfileFromLocation(startingLocation, this, userId);
        overridePendingTransition(0, 0);
    }

    private boolean validateComment() {
        if (TextUtils.isEmpty(etComment.getText())) {
            btnSendComment.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            return false;
        }

        return true;
    }

    public class Worker extends AsyncTask<String, Void, Boolean> {

        private String option;

        Worker(String option) {
            this.option = option;
        }

        // return current main instance
        public CommentsActivity getMain() {
            return CommentsActivity.this;
        }

        @Override
        protected Boolean doInBackground(String... params) {

            switch (option) {
                case "Comments":
                    getComments();
                    break;
                case "Send":
                    sendComment(params[0]);
            }

            // TODO: register the new account here.
            return true;
        }


        // get post comments
        public void getComments() {
            isLoading = true;
            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                        commentsAdapter.setAnimationsLocked(false);
                        commentsAdapter.setDelayEnterAnimation(false);
                    }
                });

                Log.i("Hologram", "Read User Comments");

                InstagramGetMediaCommentsResult commentsResult = instagram.sendRequest(new InstagramGetMediaCommentsRequest(mediaId, maxCommentId));

                if (commentsResult.getComments() == null) {
                    isLastPage = true;
                    isLoading = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    return;
                }

                Log.i("Hologram", "media id = " + mediaId);
                for (InstagramComment item : commentsResult.getComments()) {
                    Log.i("Hologram ->> comment", commentsAdapter.getItemCount() + "");
                    commentsAdapter.add(new CommentsAdapter.CommentItem(item));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            commentsAdapter.setAnimationsLocked(false);
                            commentsAdapter.setDelayEnterAnimation(false);
                            commentsAdapter.notifyItemInserted(commentsAdapter.commentItems.size() - 1);
                        }
                    });

                    Thread.sleep(100);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });

                maxCommentId = commentsResult.getNext_max_id();
                if (maxCommentId == null) {
                    isLastPage = true;
                }
                Log.i("Hologram MaxId", maxCommentId + "");


            } catch (Exception e) {
                Log.e("Hologram", Log.getStackTraceString(e));
            }

            isLoading = false;
        }

        // send user comment
        public void sendComment(String comment) {
            isLoading = true;

            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });

                Log.i("Hologram", "Send User Comment");

                InstagramPostCommentResult commentsResult = instagram.sendRequest(new InstagramPostCommentRequest(mediaId, comment));
                commentsAdapter.addFirstItem(new CommentsAdapter.CommentItem(commentsResult.getComment()));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        commentsAdapter.notifyItemInserted(0);
                        rvComments.smoothScrollToPosition(0);
                    }
                });

                //Thread.sleep(100);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });

            } catch (Exception e) {
                Log.e("Hologram", Log.getStackTraceString(e));
            }

            isLoading = false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

            if (success) {
                animateContent();
            } else {

            }
        }

        @Override
        protected void onCancelled() {

        }
    }

}
