package ir.holugram.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dev.niekirk.com.instagram4android.requests.payload.InstagramFeedItem;
import ir.holugram.R;
import ir.holugram.ui.activity.MainActivity;
import ir.holugram.ui.utils.GlideApp;
import ir.holugram.ui.utils.GlideRequest;
import ir.holugram.ui.view.LoadingFeedItemView;


/**
 * Created by froger_mcs on 05.11.14.
 */
public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String ACTION_LIKE_BUTTON_CLICKED = "action_like_button_button";
    public static final String ACTION_LIKE_IMAGE_CLICKED = "action_like_image_button";
    public static final int VIEW_TYPE_DEFAULT = 1;
    public static final int VIEW_TYPE_LOADER = 2;

    protected boolean showLoader;

//    private static final int VIEWTYPE_ITEM = 1;
//    private static final int VIEWTYPE_LOADER = 2;

    public final List<FeedItem> feedItems = new ArrayList<>();

    private Context context;
    private OnFeedItemClickListener onFeedItemClickListener;

    private boolean showLoadingView = false;

//    protected LayoutInflater mInflater;

    public FeedAdapter(Context context) {
//        mInflater = LayoutInflater.from(context);
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADER) {

            // Your Loader XML view here
            View view = LayoutInflater.from(context).inflate(R.layout.loader_item_layout, parent, false);

            // Your LoaderViewHolder class
            return new LoaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_DEFAULT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
            CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
            setupClickableViews(view, cellFeedViewHolder);
            return cellFeedViewHolder;
        }/* else if (viewType == VIEW_TYPE_LOADER) {
            LoadingFeedItemView view = new LoadingFeedItemView(context);
            view.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            return new LoadingCellFeedViewHolder(view);
        }*/

        throw new IllegalArgumentException("Invalid ViewType: " + viewType);
    }

    private void setupClickableViews(final View view, final CellFeedViewHolder cellFeedViewHolder) {
        cellFeedViewHolder.btnComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onCommentsClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onMoreClick(v, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.ivFeedCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = cellFeedViewHolder.getAdapterPosition();
                FeedItem feedItem = feedItems.get(adapterPosition);
                feedItem.likesCount++;
                notifyItemChanged(adapterPosition, ACTION_LIKE_IMAGE_CLICKED);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).showLikedSnackbar(feedItem);
                }
            }
        });
        cellFeedViewHolder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = cellFeedViewHolder.getAdapterPosition();
                FeedItem feedItem = feedItems.get(adapterPosition);
                feedItem.likesCount++;
                notifyItemChanged(adapterPosition, ACTION_LIKE_BUTTON_CLICKED);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).showLikedSnackbar(feedItem);
                }
            }
        });
        cellFeedViewHolder.ivUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onProfileClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.ivFeedBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onFeedBottomClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });

        cellFeedViewHolder.playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedItemClickListener.onVideoClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });


    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        // Loader ViewHolder
        // Loader ViewHolder
        if (viewHolder instanceof LoaderViewHolder) {
            LoaderViewHolder loaderViewHolder = (LoaderViewHolder) viewHolder;
            if (showLoader) {
                loaderViewHolder.mProgressBar.setVisibility(View.VISIBLE);
            } else {
                loaderViewHolder.mProgressBar.setVisibility(View.GONE);
            }


            return;
        }

        ((CellFeedViewHolder) viewHolder).bindView(feedItems.get(position));

        /*if (getItemViewType(position) == VIEW_TYPE_LOADER) {
            bindLoadingFeedItem((LoadingCellFeedViewHolder) viewHolder);
        }*/
    }

    @Override
    public long getItemId(int position) {

        // loader can't be at position 0
        // loader can only be at the last position
        if (position != 0 && position == getItemCount() - 1) {

            // id of loader is considered as -1 here
            return -1;
        }
        return getItemId(position);
    }


    private void bindLoadingFeedItem(final LoadingCellFeedViewHolder holder) {
        holder.loadingFeedItemView.setOnLoadingFinishedListener(new LoadingFeedItemView.OnLoadingFinishedListener() {
            @Override
            public void onLoadingFinished() {
                showLoadingView = false;
                notifyItemChanged(0);
            }
        });
        holder.loadingFeedItemView.startLoading();
    }

    @Override
    public int getItemViewType(int position) {


        // loader can't be at position 0
        // loader can only be at the last position
        if (position != 0 && position == getItemCount() - 1) {
            return VIEW_TYPE_LOADER;
        }

        return VIEW_TYPE_DEFAULT;
    }

    public void showLoading(boolean status) {
        showLoader = status;
    }

    @Override
    public int getItemCount() {

        // If no items are present, there's no need for loader
        if (feedItems == null || feedItems.size() == 0) {
            return 0;
        }

        // +1 for loader
        return feedItems.size() + 1;
    }

    public void updateItems(boolean animated) {

    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public void showLoadingView() {
        showLoadingView = true;
        notifyItemChanged(0);
    }

    public class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ivFeedCenter)
        ImageView ivFeedCenter;
        @BindView(R.id.ivFeedBottom)
        TextView ivFeedBottom;
        @BindView(R.id.txtUserName)
        TextView txtUserName;
        @BindView(R.id.btnComments)
        ImageButton btnComments;
        @BindView(R.id.btnLike)
        ImageButton btnLike;
        @BindView(R.id.btnMore)
        ImageButton btnMore;
        @BindView(R.id.vBgLike)
        View vBgLike;
        @BindView(R.id.ivLike)
        ImageView ivLike;

        @BindView(R.id.playBtn)
        ImageView playBtn;


        @BindView(R.id.tsLikesCounter)
        TextSwitcher tsLikesCounter;
        @BindView(R.id.ivUserProfile)
        ImageView ivUserProfile;
        @BindView(R.id.vImageRoot)
        FrameLayout vImageRoot;

        FeedItem feedItem;

        public CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        public void bindView(FeedItem feedItem) {
            this.feedItem = feedItem;
            int adapterPosition = getAdapterPosition();

            GlideApp.with(context)
                    .load(feedItem.picProfile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivUserProfile);
            // show The Image in a ImageView
            //new DownloadImageTask(ivFeedCenter).execute(feedItem.imgUrl);

          /*  Picasso.with(context)
                    .load(feedItem.imgUrl)
                    .placeholder(R.drawable.loader_circle)
                    .noFade()
                    //.networkPolicy(NetworkPolicy.OFFLINE)
                    .into(ivFeedCenter);*/

            //To clean image view and prevent undesired repeat
            ivFeedCenter.setImageDrawable(null);
            playBtn.setVisibility(View.GONE);

            String imageUrl = feedItem.feedData.image_versions2.candidates.get(feedItem.feedData.image_versions2.candidates.size()-1).getUrl();

                /*Picasso.with(context)
                        .load(feedItem.imgUrl)
                        .placeholder(R.drawable.loader_circle)
                        .noFade()
                        //.networkPolicy(NetworkPolicy.OFFLINE)
                        .into(ivFeedCenter);*/
                /*Picasso.with(context)

                        .load(feedItem.picProfile)

                        //.networkPolicy(NetworkPolicy.OFFLINE)
                        .transform(new CircleTransformation())
                        .into(ivUserProfile);*/

            GlideRequest gR = GlideApp.with(context).load(imageUrl);
//                        .set(DiskCache<DiskCache>,);


            if (feedItem.feedData.media_type == 2) {
                playBtn.setVisibility(View.VISIBLE);
                gR = gR.placeholder(R.drawable.loader_circle);
            }

            gR
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .signature(new ObjectKey(feedItem.feedData.getPk()))
                    .into(ivFeedCenter);


            //ivFeedCenter.setImageResource(adapterPosition % 2 == 0 ? R.drawable.img_feed_center_1 : R.drawable.img_feed_center_2);
            ivFeedBottom.setText(makeShort(feedItem.caption));
            txtUserName.setText(feedItem.userName);
            btnLike.setImageResource(feedItem.isLiked ? R.drawable.ic_heart_red : R.drawable.ic_heart_outline_grey);
            tsLikesCounter.setCurrentText(NumberFormat.getInstance().format(feedItem.likesCount) + " " + vImageRoot.getResources().getQuantityString(
                    R.plurals.likes_count, feedItem.likesCount, feedItem.likesCount
            ));

//            tsLikesCounter.setCurrentText(feedItem.likesCount>1000?feedItem.likesCount/1000+"K":feedItem.likesCount+  "");
        }

        public FeedItem getFeedItem() {
            return feedItem;
        }

        public String makeShort(String text) {
            if (text.length() > 50) {
                text = text.substring(0, 50) + " بیشتر...";
            }
            return text;
        }
    }

    public class LoadingCellFeedViewHolder extends CellFeedViewHolder {

        LoadingFeedItemView loadingFeedItemView;

        public LoadingCellFeedViewHolder(LoadingFeedItemView view) {
            super(view);
            this.loadingFeedItemView = view;
        }

        @Override
        public void bindView(FeedItem feedItem) {
            super.bindView(feedItem);
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

        public FeedItem(InstagramFeedItem feedData) {
            this.likesCount = feedData.getLike_count();
            this.isLiked = feedData.isHas_liked();
            this.imgUrl = feedData.getImage_versions2().getCandidates().get(0).getUrl();
            this.caption = feedData.getCaption() != null ? (String) feedData.getCaption().get("text") : "";
            this.itemId = feedData.getPk();
            this.userName = feedData.getUser().getUsername();
            this.picProfile = feedData.getUser().getProfile_pic_url();
            this.userId = feedData.getUser().getPk();
            this.feedData = feedData;
        }
    }

    public interface OnFeedItemClickListener {
        void onCommentsClick(View v, int position);

        void onFeedBottomClick(View v, int position);

        void onMoreClick(View v, int position);

        void onProfileClick(View v, int position);

        void onVideoClick(View v, int position);
    }

    public boolean add(FeedItem r) {
        boolean s = feedItems.add(r);
        return s;
    }
}
