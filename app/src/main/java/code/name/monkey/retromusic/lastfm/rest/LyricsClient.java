package code.name.monkey.retromusic.lastfm.rest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import code.name.monkey.retromusic.lastfm.rest.service.WikiService;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.retro.musicplayer.backend.RetroConstants.BASE_API_URL_WIKI;

/**
 * @author Hemanth S (h4h13).
 */

public class LyricsClient {
    public static final String BASE_URL = BASE_API_URL_WIKI;

    private WikiService apiService;

    public LyricsClient(@NonNull Context context) {
        this(createDefaultOkHttpClientBuilder(context).build());
    }

    public LyricsClient(@NonNull Call.Factory client) {
        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .callFactory(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        apiService = restAdapter.create(WikiService.class);
    }

    @Nullable
    public static Cache createDefaultCache(Context context) {
        File cacheDir = new File(context.getCacheDir().getAbsolutePath(), "/okhttp-lastfm/");
        if (cacheDir.mkdirs() || cacheDir.isDirectory()) {
            return new Cache(cacheDir, 1024 * 1024 * 10);
        }
        return null;
    }

    public static Interceptor createCacheControlInterceptor() {
        return chain -> {
            Request modifiedRequest = chain.request().newBuilder()
                    .addHeader("Cache-Control", String.format("max-age=%d, max-stale=%d", 31536000, 31536000))
                    .build();
            return chain.proceed(modifiedRequest);
        };
    }

    public static OkHttpClient.Builder createDefaultOkHttpClientBuilder(Context context) {
        return new OkHttpClient.Builder()
                .cache(createDefaultCache(context))
                .addInterceptor(createCacheControlInterceptor());
    }

    public WikiService getApiService() {
        return apiService;
    }
}
