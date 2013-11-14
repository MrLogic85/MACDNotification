package com.sleepyduck.macdnotification;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class ActivityMACD extends Activity {
    LinearLayout mList;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_macd);
        mList = (LinearLayout) findViewById(R.id.listView);
	}

    public void onNewSymbolClicked(View view) {
        mList.addView(new ListItem(this));
    }
}
