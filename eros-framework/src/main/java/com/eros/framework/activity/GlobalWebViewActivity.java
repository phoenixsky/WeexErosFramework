package com.eros.framework.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.eros.framework.BMWXApplication;
import com.eros.framework.BMWXEnvironment;
import com.eros.framework.R;
import com.eros.framework.adapter.router.RouterTracker;
import com.eros.framework.constant.Constant;
import com.eros.framework.constant.WXEventCenter;
import com.eros.framework.event.mediator.EventCenter;
import com.eros.framework.manager.ManagerFactory;
import com.eros.framework.manager.impl.FileManager;
import com.eros.framework.manager.impl.ModalManager;
import com.eros.framework.manager.impl.dispatcher.DispatchEventManager;
import com.eros.framework.model.WebViewParamBean;
import com.eros.framework.utils.SharePreferenceUtil;
import com.eros.widget.utils.BaseCommonUtil;

import java.io.File;
import java.util.Map;

/**
 * Created by Carry on 2017/8/25.
 */

public class GlobalWebViewActivity extends AbstractWeexActivity {

    private final String LOCAL_SCHEME = "bmlocal";

    private View rl_refresh;
    private ProgressBar mProgressBar;
    private WebView mWeb;
    private String mFailUrl;
    public static String WEBVIEW_URL = "WEBVIEW_URL";
    private WebViewParamBean mWebViewParams;
    private RelativeLayout mContainer;
    private String mTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        init();
        statusBarHidden(BMWXApplication.getWXApplication().IS_FULL_SCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    private void init() {
        Intent data = getIntent();
        mWebViewParams = (WebViewParamBean) data.getSerializableExtra(Constant.WEBVIEW_PARAMS);
        String mUrl = mWebViewParams.getUrl();

        Uri imageUri = Uri.parse(mUrl);
        if (LOCAL_SCHEME.equalsIgnoreCase(imageUri.getScheme())) {
            mUrl = "file://" + localPath(imageUri);
        }

        rl_refresh = findViewById(R.id.rl_refresh);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_progress);
        mWeb = (WebView) findViewById(R.id.webView);
        mContainer = (RelativeLayout) findViewById(R.id.rl_container);
        WebSettings settings = mWeb.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        addWebJavascriptInterface();
        settings.setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        mWeb.setWebViewClient(new MyWebViewClient(this));
        mWeb.setWebChromeClient(new MyWebChromeClient());
        if (!TextUtils.isEmpty(mUrl)) {
            mWeb.loadUrl(mUrl);
        }
        mWeb.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
//                Toast.makeText(mWeb.getContext(),"请在通知栏里查看下载进度",Toast.LENGTH_LONG).show();
//                downloadBySystem(url,contentDisposition,mimetype);
                downloadByBrowser(url);
            }
        });
        ModalManager.BmLoading.showLoading(this, "", true);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void addWebJavascriptInterface() {
        WebSettings settings = mWeb.getSettings();
        settings.setJavaScriptEnabled(true);
        mWeb.addJavascriptInterface(new JSMethod(this), "bmnative");
    }

    private String localPath(Uri uri) {
        String path = uri.getHost() + File.separator +
                uri.getPath() + "?" + uri.getQuery();
        return FileManager.getPathBundleDir(this, "bundle/" + path)
                .getAbsolutePath();
    }


    //遇到ssl错误提示用户
    private void handleSSLError(final SslErrorHandler handler) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.str_error_ssl_cert_invalid);
        builder.setPositiveButton(getResources().getString(R.string.str_ensure), new
                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                });
        builder.setNegativeButton(getResources().getString(R.string.str_cancel), new
                DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }


    private class MyWebViewClient extends WebViewClient {
        GlobalWebViewActivity activity;

        public MyWebViewClient(GlobalWebViewActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handleSSLError(handler);
        }


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            ModalManager.BmLoading.dismissLoading(activity);
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String
                failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            //L.i("web failingUrl == " + failingUrl);
            activity.mFailUrl = failingUrl;
            activity.showRefreshView();
        }
    }


    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (!TextUtils.isEmpty(title) && mTitle == null) {
                getNavigationBar().setTitle(title);
            }
        }

        @Override

        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.e("onConsoleMessage", "onConsoleMessage>>>>>" + consoleMessage.message());
            return super.onConsoleMessage(consoleMessage);
        }
    }

    private void showRefreshView() {
        showWebCloseView();
    }


    @Override
    public void onBackPressed() {
        if (mWeb.canGoBack()) {
            mWeb.goBack();
        } else {
            BaseCommonUtil.clearAllCookies(this);
            super.onBackPressed();
        }

    }

    private void showWebCloseView() {

    }

    private void downloadByBrowser(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void downloadBySystem(String url, String contentDisposition, String mimeType) {
        // 指定下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner();
        // 设置通知的显示类型，下载进行时和完成后显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 设置通知栏的标题，如果不设置，默认使用文件名
//        request.setTitle("This is title");
        // 设置通知栏的描述
//        request.setDescription("This is description");
        // 允许在计费流量下下载
        request.setAllowedOverMetered(false);
        // 允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(false);
        // 允许漫游时下载
        request.setAllowedOverRoaming(true);
        // 允许下载的网路类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        // 设置下载文件保存的路径和文件名
        String fileName  = URLUtil.guessFileName(url, contentDisposition, mimeType);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//        另外可选一下方法，自定义下载路径
//        request.setDestinationUri()
//        request.setDestinationInExternalFilesDir()
        final DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        // 添加一个下载任务
        long downloadId = downloadManager.enqueue(request);
    }

    public static class JSMethod {
        private Context mContext;

        public JSMethod(Context mContext) {
            this.mContext = mContext;
        }

        @JavascriptInterface
        public void closePage() {
            //关闭当前页面
            RouterTracker.popActivity();
        }

        @JavascriptInterface
        public void fireEvent(String eventName, String param) {
            if (!TextUtils.isEmpty(eventName)) {
                Intent emit = new Intent(WXEventCenter.EVENT_JS_EMIT);
                emit.putExtra("data", new EventCenter.Emit(eventName, param));
                ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post(emit);
            }
        }
    }
}
