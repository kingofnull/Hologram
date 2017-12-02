package dev.niekirk.com.instagram4android.requests;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.niekirk.com.instagram4android.requests.payload.InstagramPostCommentResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;

/**
 * Comment Post Request
 * 
 * @author Bruno Candido Volpato da Cunha
 *
 */
@AllArgsConstructor
public class InstagramPostCommentRequest extends InstagramPostRequest<InstagramPostCommentResult> {

    private long mediaId;
    private String commentText;
    
    @Override
    public String getUrl() {
        return "media/" + mediaId + "/comment/";
    }

    @Override
    @SneakyThrows
    public String getPayload() {
        
        Map<String, Object> likeMap = new LinkedHashMap<>();
        likeMap.put("_uuid", api.getUuid());
        likeMap.put("_uid", api.getUserId());
        likeMap.put("_csrftoken", api.getOrFetchCsrf(null));
        likeMap.put("comment_text", commentText);
        
        ObjectMapper mapper = new ObjectMapper();
        String payloadJson = mapper.writeValueAsString(likeMap);

        return payloadJson;
    }

    @Override
    @SneakyThrows
    public InstagramPostCommentResult parseResult(int statusCode, String content) {
        return parseJson(statusCode, content, InstagramPostCommentResult.class);
    }
}