package ir.holugram.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;

import butterknife.BindView;
import dev.niekirk.com.instagram4android.Instagram4Android;
import dev.niekirk.com.instagram4android.InstagramConstants;
import dev.niekirk.com.instagram4android.requests.InstagramGetRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramComment;
import dev.niekirk.com.instagram4android.requests.payload.InstagramGetMediaCommentsResult;
import ir.holugram.HolugramApplication;
import ir.holugram.R;
import ir.holugram.Utils;
import ir.holugram.ui.adapter.CommentsAdapter;
import ir.holugram.ui.utils.EndlessRecyclerViewScrollListener;
import ir.holugram.ui.view.SendCommentButton;

/**
 * Created by froger_mcs on 11.11.14.
 */
public class CommentsActivity extends BaseDrawerActivity implements SendCommentButton.OnSendClickListener {
    public static final String ARG_DRAWING_START_LOCATION = "arg_drawing_start_location";
    public static final String MEDIA_ID = "arg_media_id";

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

    private CommentsAdapter commentsAdapter;
    private int drawingStartLocation;
    private long mediaId;

    boolean isLoading = false;
    boolean isLastPage = false;

    private String maxCommentId = null;

    public Instagram4Android instagram;

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
            contentRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    contentRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    startIntroAnimation();
                    return true;
                }
            });
        }


    }

    private void setupComments() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(linearLayoutManager);
        rvComments.setHasFixedSize(true);

        commentsAdapter = new CommentsAdapter(this);
        rvComments.setAdapter(commentsAdapter);

        rvComments.setOverScrollMode(View.OVER_SCROLL_NEVER);
       /* rvComments.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    commentsAdapter.setAnimationsLocked(true);
                }
            }
        });*/

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

    private void startIntroAnimation() {
        ViewCompat.setElevation(getToolbar(), 0);
        contentRoot.setScaleY(0.1f);
        contentRoot.setPivotY(drawingStartLocation);
        llAddComment.setTranslationY(200);

        contentRoot.animate()
                .scaleY(1)
                .setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewCompat.setElevation(getToolbar(), Utils.dpToPx(8));
                        new Worker("Comments").execute((String) null);

                    }
                })
                .start();
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
            commentsAdapter.addItem();
            commentsAdapter.setAnimationsLocked(false);
            commentsAdapter.setDelayEnterAnimation(false);
            rvComments.smoothScrollBy(0, rvComments.getChildAt(0).getHeight() * commentsAdapter.getItemCount());

            etComment.setText(null);
            btnSendComment.setCurrentState(SendCommentButton.STATE_DONE);
        }
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
            }

            // TODO: register the new account here.
            return true;
        }


        // fetch user feed
        public void getComments() {
            isLoading = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //progressBar.setVisibility(View.VISIBLE);
                }
            });
            try {

                Log.i("Hologram", "Read User Comments");

                InstagramGetMediaCommentsResult commentsResult = instagram.sendRequest(new InstagramGetMediaCommentsRequest(mediaId, maxCommentId));
                Log.i("Hologram", "media id = " + mediaId);
                for (InstagramComment item : commentsResult.getComments()) {

                    Log.i("Hologram ->> comment", item.getText());
                    commentsAdapter.add(new CommentsAdapter.CommentItem(item));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            commentsAdapter.notifyItemInserted(commentsAdapter.commentItems.size() - 1);
                        }
                    });

                    Thread.sleep(100);
                }

                maxCommentId = commentsResult.getNext_max_id();
                if (maxCommentId == null) {
                    isLastPage = true;
                }
                Log.i("Hologram MaxId", maxCommentId + "");

            } catch (Exception e) {
                Log.e("Hologram", Log.getStackTraceString(e));
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //progressBar.setVisibility(View.GONE);
                }
            });
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

    public class InstagramGetMediaCommentsRequest extends InstagramGetRequest<InstagramGetMediaCommentsResult> {

        @NonNull
        private String mediaId;
        private String maxId;

        public InstagramGetMediaCommentsRequest(long mediaId, String maxId) {
            this.maxId = maxId;
            this.mediaId = Long.toString(mediaId);
        }

        @Override
        public String getUrl() {
            String url = "media/" + mediaId + "/comments/?ig_sig_key_version=" + InstagramConstants.API_KEY_VERSION;
            if (maxId != null && !maxId.isEmpty()) {
                url += "&max_id=" + maxId;
            }
            return url;
        }

        @Override
        public InstagramGetMediaCommentsResult parseResult(int resultCode, String content) {
            return parseJson(resultCode, content, InstagramGetMediaCommentsResult.class);
        }
    }
}
