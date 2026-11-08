package com.xboxcloud.tvlite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String TAG = "XboxCloudTVLite";
    private static final String XBOX_URL = "https://www.xbox.com/pt-br/play";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemBars();

        WebView.setWebContentsDebuggingEnabled(true);

        webView = new WebView(this);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        // O WebView do Android 7 acrescenta "wv" ao User-Agent.
        // Alguns sites tratam isso como navegador incorporado e bloqueiam recursos.
        // Mantemos a versão real do Chromium, retirando apenas os marcadores de WebView.
        String userAgent = settings.getUserAgentString();
        String browserUserAgent = userAgent
                .replace("; wv)", ")")
                .replace("Version/4.0 ", "");
        settings.setUserAgentString(browserUserAgent);
        Log.i(TAG, "User-Agent: " + browserUserAgent);

        // Permite conteúdo misto apenas no modo de compatibilidade, caso a página
        // carregue algum recurso legado HTTP dentro da sessão HTTPS.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            private boolean openCloudGamingInChrome(String url) {
                Uri uri = Uri.parse(url);
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
                boolean cloudGaming = host.equals("play.xbox.com")
                        || (host.equals("www.xbox.com") && path.contains("/play"));
                if (!cloudGaming) return false;

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.setPackage("com.android.chrome");
                    startActivity(intent);
                    Log.i(TAG, "Abrindo Xbox Cloud Gaming no Chrome: " + url);
                } catch (Exception chromeError) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                        Log.i(TAG, "Abrindo Xbox Cloud Gaming no navegador padrão: " + url);
                    } catch (Exception noBrowser) {
                        Log.e(TAG, "Não foi possível abrir o navegador", noBrowser);
                    }
                }
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openCloudGamingInChrome(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openCloudGamingInChrome(request.getUrl().toString());
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                Log.i(TAG, "Página iniciou: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "Página terminou: " + url);
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request.isForMainFrame()) {
                    Log.e(TAG, "Erro na página: " + error.getErrorCode() + " - " + error.getDescription());
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame()) {
                    Log.e(TAG, "Erro HTTP: " + response.getStatusCode() + " em " + request.getUrl());
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                Log.d(TAG, "Console " + message.messageLevel() + ": " + message.message()
                        + " (" + message.sourceId() + ":" + message.lineNumber() + ")");
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d(TAG, "Carregamento: " + newProgress + "%");
            }
        });

        setContentView(webView);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.loadUrl(XBOX_URL);
    }

    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP
                && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
