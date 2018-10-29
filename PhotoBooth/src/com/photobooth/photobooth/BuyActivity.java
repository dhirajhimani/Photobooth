package com.photobooth.photobooth;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class BuyActivity extends Activity {
	
	private WebView webview;
	private ProgressBar pb;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_buy);
		
		webview = (WebView) findViewById(R.id.webview);
		webview.setWebViewClient(new MyWebClient());
		WebSettings webSettings = webview.getSettings();
		webSettings.setJavaScriptEnabled(true);
		
		pb = (ProgressBar) findViewById(R.id.pb);
		
		webview.loadUrl("http://www.google.com");
	}
	
	
	private class MyWebClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			pb.setVisibility(View.VISIBLE);
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			pb.setVisibility(View.GONE);			
		}
		
		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			pb.setVisibility(View.GONE);
			super.onReceivedError(view, errorCode, description, failingUrl);
		}
		
	}

}
