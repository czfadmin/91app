package com.u91porn;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Severity;
import com.danikula.videocache.HttpProxyCacheServer;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.github.yuweiguocn.library.greendao.MigrationHelper;
import com.helper.loadviewhelper.load.LoadViewHelper;
import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.model.FileDownloadStatus;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;
import com.squareup.leakcanary.LeakCanary;
import com.u91porn.cookie.SetCookieCache;
import com.u91porn.cookie.SharedPrefsCookiePersistor;
import com.u91porn.data.NoLimit91PornServiceApi;
import com.u91porn.data.cache.CacheProviders;
import com.u91porn.data.dao.DaoMaster;
import com.u91porn.data.dao.DaoSession;
import com.u91porn.data.dao.GreenDaoHelper;
import com.u91porn.data.dao.MySQLiteOpenHelper;
import com.u91porn.data.model.UnLimit91PornItem;
import com.u91porn.data.model.User;
import com.u91porn.utils.AppCacheUtils;
import com.u91porn.utils.Constants;
import com.u91porn.utils.Keys;
import com.u91porn.utils.SPUtils;
import com.u91porn.utils.VideoCacheFileNameGenerator;

import org.greenrobot.greendao.database.Database;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.bingoogolapple.swipebacklayout.BGASwipeBackHelper;
import io.reactivex.annotations.Nullable;
import io.rx_cache2.internal.RxCache;
import io.victoralbertos.jolyglot.GsonSpeaker;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * 应用入口
 *
 * @author flymegoc
 * @date 2017/11/14
 */

public class MyApplication extends MultiDexApplication {

    private static final String TAG = MyApplication.class.getSimpleName();
    private NoLimit91PornServiceApi mNoLimit91PornServiceApi;
    private static MyApplication mMyApplication;
    private PersistentCookieJar cookieJar;
    private volatile String host;
    /**
     * 视频缓存
     */
    private HttpProxyCacheServer proxy;
    private CacheProviders cacheProviders;

    private SharedPrefsCookiePersistor sharedPrefsCookiePersistor;
    private SetCookieCache setCookieCache;
    private User user;
    private boolean isShowTips = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mMyApplication = this;
        host = (String) SPUtils.get(this, Keys.KEY_SP_NOW_ADDRESS, "");
        initLogger();
        initGreenDao3(this);
        initLeakCanry();
        initRetrofit("", 0);
        initRxCache();
        initLoadingHelper();
        initFileDownload();
        if (!BuildConfig.DEBUG) {
            //初始化bug收集
            Bugsnag.init(this);
        }
        BGASwipeBackHelper.init(this, null);
    }

    public boolean isShowTips() {
        return isShowTips;
    }

    public void setShowTips(boolean showTips) {
        isShowTips = showTips;
    }

    private void initFileDownload() {
        FileDownloader.setup(this);
        //纠正下载状态
        List<UnLimit91PornItem> list = GreenDaoHelper.getInstance().findByNotDownloadStatus(FileDownloadStatus.completed);
        for (UnLimit91PornItem unLimit91PornItem : list) {
            unLimit91PornItem.setStatus(FileDownloadStatus.paused);
        }
        GreenDaoHelper.getInstance().updateInTx(list);
    }

    /**
     * 初始化加载界面，空界面等
     */
    private void initLoadingHelper() {
        LoadViewHelper.getBuilder()
                .setLoadEmpty(R.layout.empty_view)
                .setLoadError(R.layout.error_view)
                .setLoadIng(R.layout.loading_view);
    }

    /**
     * 初始化greendDao3库
     */
    private void initGreenDao3(Context context) {
        //如果你想查看日志信息，请将DEBUG设置为true
        MigrationHelper.DEBUG = BuildConfig.DEBUG;
        MySQLiteOpenHelper helper = new MySQLiteOpenHelper(context, Constants.DB_NAME, null);
        Database db = helper.getWritableDb();
        DaoSession daoSession = new DaoMaster(db).newSession();
        GreenDaoHelper.init(daoSession);
    }

    /**
     * 设置地址
     *
     * @param host 当前可访问地址
     */
    public void setHost(@Nullable String host) {
        this.host = host;
        SPUtils.put(this, Keys.KEY_SP_NOW_ADDRESS, host);
    }

    public User getUser() {
        boolean isUserInfoNotComplete = user == null || user.getUserId() == 0 || TextUtils.isEmpty(user.getUserName());
        if (isUserInfoNotComplete) {
            return null;
        }
        if (user != null && (user.getUserId() == 0 || TextUtils.isEmpty(user.getUserName()))) {
            Bugsnag.notify(new Throwable("User info: " + user.toString()), Severity.WARNING);
        }
        return user;
    }

    public void setUser(User user) {
        if (user != null) {
            Logger.t(TAG).d(user.toString());
        }
        this.user = user;
    }

    /**
     * 获取当前可访问的地址
     *
     * @return 地址
     */
    public String getHost() {
        if (TextUtils.isEmpty(host)) {
            return Constants.BASE_URL;
        }
        return host;
    }

    /**
     * 获取视频缓存代理
     *
     * @return proxy
     */
    public HttpProxyCacheServer getProxy() {
        synchronized (MyApplication.class) {
            if (proxy == null) {
                proxy = newProxy();
            }
        }
        return proxy;
    }

    /**
     * 初始化视频缓存代理
     *
     * @return proxy
     */
    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                // 1 Gb for cache
                .maxCacheSize(AppCacheUtils.MAX_VIDEO_CACHE_SIZE)
                .cacheDirectory(AppCacheUtils.getVideoCacheDir(this))
                .fileNameGenerator(new VideoCacheFileNameGenerator())
                .build();
    }

    /**
     * 初始化内存分析工具
     */
    private void initLeakCanry() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        // Normal app init code...
    }

    /**
     * 初始化Retrifit网络请求
     */
    public void initRetrofit(String proxyHost, int port) {

        sharedPrefsCookiePersistor = new SharedPrefsCookiePersistor(this);
        setCookieCache = new SetCookieCache();
        cookieJar = new PersistentCookieJar(setCookieCache, sharedPrefsCookiePersistor);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
                //统一设置请求头
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder();
                requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.16299");
                requestBuilder.header("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5");
                requestBuilder.header("Proxy-Connection", "keep-alive");
                requestBuilder.header("Cache-Control", "max-age=0");
                // requestBuilder.header("X-Forwarded-For","114.114.114.117")
                requestBuilder.method(original.method(), original.body());
                String host = MyApplication.this.host;
                //切换服务器地址
                if (!TextUtils.isEmpty(host)) {
                    host = host.substring(host.indexOf("//") + 2, host.lastIndexOf("/"));
                    Logger.d("host:" + host);
                    if (!TextUtils.isEmpty(host)) {
                        HttpUrl newUrl = original.url().newBuilder()
                                .host(host)
                                .build();
                        requestBuilder.url(newUrl);
                        if (!"github.com".equals(original.url().host())) {
                            requestBuilder.header("Host", host);
                        }
                    }
                } else {
                    if (!"github.com".equals(original.url().host())) {
                        host = Constants.BASE_URL.substring(Constants.BASE_URL.indexOf("//") + 2, Constants.BASE_URL.lastIndexOf("/"));
                        requestBuilder.header("Host", host);
                    }
                }

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });
        builder.cookieJar(cookieJar);
        //如果代理地址不为空，且端口正确设置Http代理
        if (!TextUtils.isEmpty(proxyHost) && port < Constants.PROXY_MAX_PORT) {
            Logger.t(TAG).d("代理设置： host:" + proxyHost + "  端口：" + port);
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port)));
        }


        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(@NonNull String message) {
                Logger.t(TAG).d("HttpLog:" + message);
            }
        });
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        builder.addInterceptor(logging);
        builder.readTimeout(5, TimeUnit.SECONDS);
        builder.writeTimeout(5, TimeUnit.SECONDS);
        builder.connectTimeout(5, TimeUnit.SECONDS);

        Retrofit retrofit = new Retrofit.Builder()
                .client(builder.build())
                .baseUrl(Constants.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        mNoLimit91PornServiceApi = retrofit.create(NoLimit91PornServiceApi.class);
    }

    public SharedPrefsCookiePersistor getSharedPrefsCookiePersistor() {
        return sharedPrefsCookiePersistor;
    }

    /**
     * 初始化缓存
     */
    private void initRxCache() {
        File cacheDir = AppCacheUtils.getRxCacheDir(this);
        cacheProviders = new RxCache.Builder()
                .persistence(cacheDir, new GsonSpeaker())
                .using(CacheProviders.class);
    }

    public CacheProviders getCacheProviders() {
        return cacheProviders;
    }

    /**
     * 清除cookies
     */
    public void cleanCookies() {
        if (cookieJar == null) {
            return;
        }
        cookieJar.clear();
    }

    public SetCookieCache getSetCookieCache() {
        return setCookieCache;
    }

    public static MyApplication getInstace() {
        return mMyApplication;
    }

    public NoLimit91PornServiceApi getNoLimit91PornService() {
        return mNoLimit91PornServiceApi;
    }

    /**
     * 初始化日志工具
     */
    private void initLogger() {
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                // (Optional) Whether to show thread info or not. Default true
                .showThreadInfo(false)
                // (Optional) How many method line to show. Default 2
                .methodCount(0)
                // (Optional) Hides internal method calls up to offset. Default 5
                .methodOffset(5)
                // .logStrategy(customLog) // (Optional) Changes the log strategy to print out. Default LogCat
                // .tag("My custom tag")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return BuildConfig.DEBUG;
            }
        });
    }
}
