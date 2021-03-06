package ir.holugram.ui.adapter;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedItem;
import dev.niekirk.com.instagram4android.requests.payload.InstagramVideoVersions;
import ir.holugram.R;
import ir.holugram.Utils;
import ir.holugram.ui.activity.VideoPlayerActivity;

/**
 * Created by Miroslaw Stanek on 20.01.15.
 */
public class UserProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int PHOTO_ANIMATION_DELAY = 600;
    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();


    private final Context context;
    private final int cellSize;

    public List<FeedItem> feedItems = new ArrayList<>();
    ;

    private boolean lockedAnimations = false;
    private int lastAnimatedItem = -1;

    public UserProfileAdapter(Context context) {
        this.context = context;
        this.cellSize = Utils.getScreenWidth(context) / 3;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        layoutParams.height = cellSize;
        layoutParams.width = cellSize;
        layoutParams.setFullSpan(false);
        view.setLayoutParams(layoutParams);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        bindPhoto((PhotoViewHolder) holder, position);
    }

    private void bindPhoto(final PhotoViewHolder holder, int position) {
        final FeedItem item = feedItems.get(position);
        Picasso.with(context)
                .load(item.imgUrl)
                .resize(cellSize, cellSize)
                .centerCrop()
                .into(holder.ivPhoto, new Callback() {
                    @Override
                    public void onSuccess() {
                        animatePhoto(holder);
                    }

                    @Override
                    public void onError() {

                    }
                });

        holder.playBtnProfile.setVisibility(View.INVISIBLE);

        if (item.feedData.media_type == 2) {
            holder.playBtnProfile.setVisibility(View.VISIBLE);
            holder.btnDownloadProfile.setVisibility(View.VISIBLE);

            holder.playBtnProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InstagramVideoVersions video = item.feedData.video_versions.get(item.feedData.video_versions.size() - 1);
                    String videoUrl = video.getUrl();
                    Log.i("VideoUrl", videoUrl);
                    showVideo(videoUrl);
                }

                public void showVideo(String Url) {
                    Intent videoIntent = new Intent(context, VideoPlayerActivity.class);
                    videoIntent.putExtra("url", Url);
                    context.startActivity(videoIntent);
                }
            });

        }

        holder.btnDownloadProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = null;
                String fileName = null;

                if (item.feedData.getMedia_type() == 1) {
                    url = item.feedData.getImage_versions2().getCandidates().get(0).getUrl();
                    fileName = item.itemId + ".jpg";
                    downloadFromUrl(url, fileName, "در حال دانلود تصویر . . .");
                } else if (item.feedData.getMedia_type() == 2) {
                    url = item.feedData.video_versions.get(0).getUrl();
                    fileName = item.itemId + ".mp4";
                    downloadFromUrl(url, fileName, "در حال دانلود ویدیو . . .");
                }
            }

            public void downloadFromUrl(String url, String fileName, String caption) {
                Log.e("DOWNLOAD-TRY", url);
                Log.e("DOWNLOAD-TRY", fileName);

                url = url.replace(" ", "%20");
                DownloadManager downloadManager = (DownloadManager) ((Activity) context).getSystemService(Context.DOWNLOAD_SERVICE);

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setDescription(caption)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                downloadManager.enqueue(request);


            }

        });

        if (lastAnimatedItem < position) lastAnimatedItem = position;


    }

    private void animatePhoto(PhotoViewHolder viewHolder) {
        if (!lockedAnimations) {
            if (lastAnimatedItem == viewHolder.getPosition()) {
                setLockedAnimations(true);
            }

            long animationDelay = PHOTO_ANIMATION_DELAY + viewHolder.getPosition() * 30;

            viewHolder.flRoot.setScaleY(0);
            viewHolder.flRoot.setScaleX(0);

            viewHolder.flRoot.animate()
                    .scaleY(1)
                    .scaleX(1)
                    .setDuration(200)
                    .setInterpolator(INTERPOLATOR)
                    .setStartDelay(animationDelay)
                    .start();
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    public void setLockedAnimations(boolean lockedAnimations) {
        this.lockedAnimations = lockedAnimations;
    }

    public boolean add(FeedItem r) {
        boolean s = feedItems.add(r);
        return s;
    }

    public interface OnFeedItemClickListener {
        void onCommentsClick(View v, int position);

        void onFeedBottomClick(View v, int position);

        void onMoreClick(View v, int position);

        void onProfileClick(View v, int position);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.flRoot)
        FrameLayout flRoot;
        @BindView(R.id.ivPhoto)
        ImageView ivPhoto;
        @BindView(R.id.playBtnProfile)
        ImageView playBtnProfile;
        @BindView(R.id.btnDownloadProfile)
        ImageView btnDownloadProfile;

        public PhotoViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public static class FeedItem {
        public int likesCount;
        public boolean isLiked;
        public String imgUrl;
        public long itemId;
        public String caption;
        public String userName;
        public long userId;
        public String picProfile;
        public InstagramFeedItem feedData;

        public FeedItem(InstagramFeedItem item) {
            this.likesCount = item.getLike_count();
            this.isLiked = item.isHas_liked();
            this.imgUrl = item.getImage_versions2().getCandidates().get(0).getUrl();
            this.caption = item.getCaption() != null ? (String) item.getCaption().get("text") : "";
            this.itemId = item.getPk();
            this.userName = item.getUser().getUsername();
            this.picProfile = item.getUser().getProfile_pic_url();
            this.userId = item.getUser().getPk();
            this.feedData = item;
        }
    }


}
