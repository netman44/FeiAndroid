package com.njnu.kai.feiphoneinfo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TaskActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TextView tv = new TextView(this);
		tv.setText("Task");
		setContentView(tv);
	}


}