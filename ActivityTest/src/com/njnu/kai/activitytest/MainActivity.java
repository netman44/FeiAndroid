package com.njnu.kai.activitytest;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.widget.Button;

public class MainActivity extends BaseActivity {

	@Override
	protected String getButtonText() {
		return "to Second";
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected Intent getStartupIntent() {
		Intent intent = new Intent(this, SecondActivity.class);
//		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.weibo.cn"));
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}


}
