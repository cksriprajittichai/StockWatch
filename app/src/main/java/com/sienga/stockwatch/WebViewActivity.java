package com.sienga.stockwatch;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;


public final class WebViewActivity extends AppCompatActivity {

    @BindView(R.id.webView) WebView webView;
    @BindView(R.id.progressBar_webViewProgressBar) ProgressBar progressBar;

    /**
     * Initializes various components of this Activity. This method then starts
     * {@link #webView} by calling {@link WebView#loadUrl(String)}, using the
     * URL passed through Intent extras from IndividualStockActivity.
     *
     * @param savedInstanceState The savedInstanceState is not used
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        ButterKnife.bind(this);
        initWebView();
        progressBar.setMax(100);

        final String url = getIntent().getStringExtra("URL");
        webView.loadUrl(url);
    }

    /**
     * Initializes {@link #webView} to have certain features, including updating
     * {@link #progressBar} while loading pages.
     */
    private void initWebView() {
        /* By giving webView a WebViewClient, webView changes functionality to
         * open links that are clicked in the web view inside the web view,
         * rather than in an outside browser. */
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(final WebView view, final String url,
                                      final Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(final WebView view, final String url) {
                super.onPageFinished(view, url);
                progressBar.setProgress(100);
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });

        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true); // Shows zoom controls
        webView.getSettings().setDisplayZoomControls(false); // Hide zoom controls
    }

    /**
     * If the user is currently at the first website that was opened in {@link
     * #webView} return to the IndividualStockActivity that we were in before
     * this Activity. Otherwise, if the user is at the second or subsequent
     * website that was opened in webView, return to the previous website - like
     * the back button in a browser.
     */
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}
