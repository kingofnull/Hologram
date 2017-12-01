package dev.niekirk.com.instagram4android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.niekirk.com.instagram4android.requests.InstagramLoginRequest;
import dev.niekirk.com.instagram4android.requests.InstagramRequest;
import dev.niekirk.com.instagram4android.requests.internal.InstagramFetchHeadersRequest;
import dev.niekirk.com.instagram4android.requests.payload.InstagramLoginPayload;
import dev.niekirk.com.instagram4android.requests.payload.InstagramLoginResult;
import dev.niekirk.com.instagram4android.requests.payload.InstagramSyncFeaturesPayload;
import dev.niekirk.com.instagram4android.requests.payload.StatusResult;
import dev.niekirk.com.instagram4android.util.InstagramGenericUtil;
import dev.niekirk.com.instagram4android.util.InstagramHashUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by root on 08/06/17.
 */

public class Instagram4Android {

    Context context;

    @Getter
    protected String deviceId;

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private String password;

    @Getter
    protected boolean isLoggedIn;

    @Getter
    private String uuid;

    @Getter
    @Setter
    protected String rankToken;

    @Getter
    @Setter
    private long userId;

    @Getter
    @Setter
    protected Response lastResponse;

    @Getter
    protected OkHttpClient client;

    private final Set<Cookie> cookieStore = new HashSet<>();

    public CookieJar cookieJar;

    private SharedPreferences preferences;
    private final String STORAGE_NAME = "AuthData";

    @Builder
    public Instagram4Android(final Context context) {
        super();
        this.context = context;
        preferences = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE);
        loadPreferences();

        cookieJar = new CookieJar() {

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                //Log.d("I4A", "Added cookies!");
                cookieStore.addAll(cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> validCookies = new ArrayList<>();
                for (Cookie cookie : cookieStore) {

                    //Log.d("I4A", "Cookie: " + cookie.name());

                    if (cookie.expiresAt() < System.currentTimeMillis()) {

                    } else {
                        validCookies.add(cookie);
                    }

                }

                return validCookies;
            }
        };
//        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));


        client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
    }

    public void setup() {

        Log.i("Instagram", "setup");
        if (this.username.length() < 1) {
            throw new IllegalArgumentException("Username is mandatory.");
        }

        if (this.password.length() < 1) {
            throw new IllegalArgumentException("Password is mandatory.");
        }

        this.deviceId = InstagramHashUtil.generateDeviceId(this.username, this.password);
        this.uuid = InstagramGenericUtil.generateUuid(true);


//        cookieJar = new SerializableCookieJar(context);


    }

    public InstagramLoginResult login() throws IOException {

        Log.d("LOGIN", "Logging with user " + username + " and password " + password.replaceAll("[a-zA-Z0-9]", "*"));
        InstagramLoginPayload loginRequest = InstagramLoginPayload.builder().username(username)
                .password(password)
                .guid(uuid)
                .device_id(deviceId)
                .phone_id(InstagramGenericUtil.generateUuid(true))
                .login_attempt_account(0)
                ._csrftoken(getOrFetchCsrf(null))
                .build();

        InstagramLoginResult loginResult = this.sendRequest(new InstagramLoginRequest(loginRequest));
        if (loginResult.getStatus().equalsIgnoreCase("ok")) {
            this.userId = loginResult.getLogged_in_user().getPk();
            this.rankToken = this.userId + "_" + this.uuid;
            this.isLoggedIn = true;


            InstagramSyncFeaturesPayload syncFeatures = InstagramSyncFeaturesPayload.builder()
                    ._uuid(uuid)
                    ._csrftoken(getOrFetchCsrf(null))
                    ._uid(userId)
                    .id(userId)
                    .experiments(InstagramConstants.DEVICE_EXPERIMENTS)
                    .build();


//            this.sendRequest(new InstagramSyncFeaturesRequest(syncFeatures));
//            this.sendRequest(new InstagramAutoCompleteUserListRequest());
            //this.sendRequest(new InstagramTimelineFeedRequest());
//            this.sendRequest(new InstagramGetInboxRequest());
//            this.sendRequest(new InstagramGetRecentActivityRequest());

            savePreferences();
        }


        return loginResult;
    }

    public String getOrFetchCsrf(HttpUrl url) throws IOException {

        Cookie cookie = getCsrfCookie(url);
        if (cookie == null) {
            sendRequest(new InstagramFetchHeadersRequest());
            cookie = getCsrfCookie(url);
        }

        return cookie.value();

    }

    public Cookie getCsrfCookie(HttpUrl url) {

        for (Cookie cookie : client.cookieJar().loadForRequest(url)) {

//            Log.d("GETCOOKIE", "Name: " + cookie.name());
            if (cookie.name().equalsIgnoreCase("csrftoken")) {
                return cookie;
            }

        }

        return null;

    }

    public <T> T sendRequest(InstagramRequest<T> request) throws IOException {

        if (!this.isLoggedIn
                && request.requiresLogin()) {
            throw new IllegalStateException("Need to login first!");
        }

        request.setApi(this);
        T response = request.execute();

        if (((StatusResult) response).getMessage() == "login_required") {
            throw new IllegalStateException("Need to login first!");
        }

        return response;
    }


    public void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("uuid", uuid);
        editor.putString("rankToken", rankToken);
        editor.putLong("userId", userId);
        editor.putString("deviceId", deviceId);
        editor.putBoolean("isLoggedIn", isLoggedIn);

        JSONArray cookies = new JSONArray();
        for (Cookie cookie : client.cookieJar().loadForRequest(null)) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", cookie.name());
                obj.put("value", cookie.value());
                obj.put("expire", cookie.expiresAt());
                obj.put("domain", cookie.domain());
                cookies.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        String cookiesStr = cookies.toString();
        editor.putString("cookies", cookiesStr);
        Log.i("Cookies", cookiesStr);
        Log.i("savePreferences", editor.toString());
        editor.apply();
    }

    public void loadPreferences() {
        uuid = preferences.getString("uuid", null);
        rankToken = preferences.getString("rankToken", null);
        userId = preferences.getLong("userId", 0);
        uuid = preferences.getString("uuid", null);
        isLoggedIn = preferences.getBoolean("isLoggedIn", false);
        try {
            JSONArray cookies = new JSONArray(preferences.getString("cookies", ""));
            for (int i = 0; i < cookies.length(); i++) {
                JSONObject c = (JSONObject) cookies.get(i);
                Cookie cookie = (new Cookie.Builder())
                        .domain(c.getString("domain"))
                        .name(c.getString("name"))
                        .value(c.getString("value"))
                        .expiresAt(c.getLong("expire"))
                        .build();
                cookieStore.add(cookie);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.i("loadPreferences", preferences.getAll().toString());

    }
}
