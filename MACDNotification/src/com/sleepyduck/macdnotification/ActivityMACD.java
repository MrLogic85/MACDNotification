package com.sleepyduck.macdnotification;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class ActivityMACD extends Activity {
	public static final String KEY_SYMBOL = "sumbols";
	LinearLayout mList;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_macd);
		mList = (LinearLayout) findViewById(R.id.listView);
	}

	private Set<String> getSymbolSet() {
		View v;
		ListItem listItem;
		final Set<String> list = new HashSet<String>();
		for (int i = 0; i < mList.getChildCount(); i++) {
			v = mList.getChildAt(i);
			if (v instanceof ListItem) {
				listItem = (ListItem) v;
				list.add(listItem.getSymbol());
			}
		}
		return list;
	}

	private void loadSymbolList(final Set<String> symbolSet) {
		if (symbolSet.size() > 0) {
			mList.removeAllViews();
			for (final String symbol : symbolSet)
				mList.addView(new ListItem(this, symbol));
		}
	}

	private void saveSymbols() {
		final Editor edit = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
		edit.putStringSet(KEY_SYMBOL, getSymbolSet());
		edit.commit();
	}

	public void onNewSymbolClicked(final View view) {
		mList.addView(new ListItem(this));
	}

	@Override
	public void onPause() {
		super.onPause();
		saveSymbols();
	}

	@Override
	public void onResume() {
		super.onResume();
		loadSymbolList(getSharedPreferences(getPackageName(), MODE_PRIVATE).getStringSet(KEY_SYMBOL,
				new HashSet<String>()));
	}
}
