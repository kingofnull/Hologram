package ir.holugram.ui.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import dev.niekirk.com.instagram4android.requests.payload.InstagramComment;
import ir.holugram.R;
import ir.holugram.ui.activity.CommentsActivity;
import ir.holugram.ui.activity.MainActivity;
import ir.holugram.ui.activity.UserProfileActivity;
import ir.holugram.ui.utils.RoundedTransformation;
import ir.holugram.ui.view.LoadingFeedItemView;


/**
 * Created by froger_mcs on 11.11.14.
 */
public class CommentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private int lastAnimatedPosition = -1;
    private int avatarSize;

    private static long curTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
    public static final int VIEW_TYPE_DEFAULT = 1;
    public static final int VIEW_TYPE_LOADER = 2;

    public final List<CommentItem> commentItems = new ArrayList<>();

    private boolean animationsLocked = false;
    private boolean delayEnterAnimation = true;

    private OnCommentClickListener onCommentClickListener;

    public CommentsAdapter(Context context) {
        this.context = context;
        avatarSize = context.getResources().getDimensionPixelSize(R.dimen.comment_avatar_size);

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        CommentViewHolder cellFeedViewHolder = new CommentViewHolder(view);
        setupClickableViews(view, cellFeedViewHolder);
        return cellFeedViewHolder;

    }

    private void setupClickableViews(final View view, final CommentViewHolder cellCommentViewHolder) {
        cellCommentViewHolder.ivUserAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position =  cellCommentViewHolder.getAdapterPosition();
                CommentsAdapter.CommentItem item = commentItems.get(position);
                int[] startingLocation = new int[2];
                v.getLocationOnScreen(startingLocation);
                startingLocation[0] += v.getWidth() / 2;
                long userId = item.userId;
                UserProfileActivity.startUserProfileFromLocation(startingLocation, (CommentsActivity)context, userId);


            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_LOADER;
        } else {
            return VIEW_TYPE_DEFAULT;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        runEnterAnimation(viewHolder.itemView, position);
        CommentViewHolder holder = (CommentViewHolder) viewHolder;

        if (commentItems.get(position) == null)
            return;

        CommentItem item = commentItems.get(position);
        String txt = item.userName + ":\n\n" + item.text + "\n\n" + item.agoTime;
        holder.tvComment.setText(txt);

        Picasso.with(context)
                .load(item.picProfile)
                .centerCrop()
                .resize(avatarSize, avatarSize)
                .transform(new RoundedTransformation())
                .into(holder.ivUserAvatar);
    }

    private void runEnterAnimation(View view, int position) {
        if (animationsLocked) return;

        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            //view.setTranslationY(100);
            view.setAlpha(0.f);
            view.animate()
                    .translationY(0).alpha(1.f)
                    .setStartDelay(delayEnterAnimation ? 20 * (position) : 0)
                    .setInterpolator(new DecelerateInterpolator(2.f))
                    .setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            animationsLocked = true;
                        }
                    })
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return commentItems.size();
    }

    public void updateItems() {
        notifyDataSetChanged();
    }

    public static class CommentItem {

        public String text;
        public String picProfile;
        public String userName;
        public long userId;
        public String agoTime;

        public CommentItem(InstagramComment item) {
            text = item.getText();
            picProfile = item.getUser().getProfile_pic_url();
            userName = item.getUser().getUsername();
            userId = item.getUser_id();
            agoTime = calcTime(item.getCreated_at_utc());

        }

        private String calcTime(long time) {

            long seconds = (curTime / 1000 - time);
            long minute = seconds / 60;
            long hour = minute / 60;
            long date = hour / 24;
            long month = date / 30;
            long year = month / 12;

            String result = "چند لحظه پیش";

            if (year > 0)
                result = year + " سال پیش";
            else if (month > 0)
                result = month + " ماه پیش";
            else if (date > 0)
                result = date + " روز پیش";
            else if (hour > 0)
                result = hour + " ساعت پیش";
            else if (minute > 0)
                result = minute + " دقیقه پیش";

            return result;
        }
    }

    public boolean add(CommentItem r) {
        boolean s = commentItems.add(r);
        if (s) {
        }
        return s;
    }

    public void addFirstItem(CommentItem r) {
        commentItems.add(0, r);
    }


    public void setAnimationsLocked(boolean animationsLocked) {
        this.animationsLocked = animationsLocked;
    }

    public void setDelayEnterAnimation(boolean delayEnterAnimation) {
        this.delayEnterAnimation = delayEnterAnimation;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ivUserAvatar)
        ImageView ivUserAvatar;
        @BindView(R.id.tvComment)
        TextView tvComment;

        public CommentViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public interface OnCommentClickListener {

        void onProfileClick(View v, int itemPosition);
    }

}
