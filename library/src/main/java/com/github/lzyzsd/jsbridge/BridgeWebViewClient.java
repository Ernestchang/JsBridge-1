package com.github.lzyzsd.jsbridge;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.tencent.smtt.sdk.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends com.tencent.smtt.sdk.WebViewClient {
    private BridgeWebView webView;

    public BridgeWebViewClient(BridgeWebView webView) {
        this.webView = webView;
    }

    @Override
    public boolean shouldOverrideUrlLoading(com.tencent.smtt.sdk.WebView view, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
            webView.handlerReturnData(url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
            webView.flushMessageQueue();
            return true;
        } else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(final WebView view, String url) {
        super.onPageFinished(view, url);

        if (BridgeWebView.toLoadJs != null) {
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BridgeUtil.webViewLoadLocalJs(view, BridgeWebView.toLoadJs);
                }
            }, 100);
        }

        //
        if (webView.getStartupMessage() != null) {
            for (Message m : webView.getStartupMessage()) {
                webView.dispatchMessage(m);
            }
            webView.setStartupMessage(null);
        }
    }


    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        //注意：super句话一定要删除，或者注释掉，否则又走handler.cancel()默认的不支持https的了。
        //super.onReceivedSslError(view, handler, error);
        //handler.cancel(); // Android默认的处理方式
        //handler.handleMessage(Message msg); // 进行其他处理

        handler.proceed(); // 接受所有网站的证书
    }


    @TargetApi(android.os.Build.VERSION_CODES.M)
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
//        super.onReceivedHttpError(view, request, errorResponse);
        // 这个方法在6.0才出现
        int statusCode = errorResponse.getStatusCode();
        Log.e("ernest", "onReceivedHttpError code = " + statusCode);
//        if (404 == statusCode || 500 == statusCode) {
//            view.loadUrl("about:blank");// 避免出现默认的错误界面
//            view.loadUrl("file:///android_asset/error.html");
//        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        Log.e("ernest", "onReceivedError errorCode = " + errorCode);
        // 断网或者网络连接超时
        if (errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_CONNECT || errorCode == ERROR_TIMEOUT) {
            view.loadUrl("about:blank"); // 避免出现默认的错误界面
            view.loadUrl("file:///android_asset/error.html");
        }
    }
}