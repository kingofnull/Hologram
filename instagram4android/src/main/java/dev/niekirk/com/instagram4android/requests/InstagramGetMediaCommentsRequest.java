package dev.niekirk.com.instagram4android.requests;

import dev.niekirk.com.instagram4android.InstagramConstants;
import dev.niekirk.com.instagram4android.requests.payload.InstagramGetMediaCommentsResult;

import lombok.NonNull;

/**
 * Created by root on 09/06/17.
 */

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
