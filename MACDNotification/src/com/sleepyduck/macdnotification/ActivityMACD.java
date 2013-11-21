package com.sleepyduck.macdnotification;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;

public class ActivityMACD extends Activity {
	public static final String KEY_GROUP = "group";
	public static final String KEY_COUNT = "count";
	public static final String KEY_NAME = "name";
	public static final String KEY_SEPPARATOR = ":";
    public static final String ACTION_BROADCAST_REMOVE = "ActivityMACD:action_broadcast_remove";
    private static final String KEY_VALUE_SEPPARATOR = "<=>";

	private final List<String> mGroups = Collections.synchronizedList(new ArrayList<String>());
	private final Map<String, List<String>> mSymbols = Collections
			.synchronizedMap(new LinkedHashMap<String, List<String>>());
	private ExpandableListAdapter mListAdapter;
	private Spinner mGroupSpinner;
	private ArrayAdapter<String> mSpinnerAdapter;
    private View mAddLayout;
    private EditText mNameEditText;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(KEY_GROUP) && intent.hasExtra(KEY_NAME)) {
                String group = intent.getStringExtra(KEY_GROUP);
                String symbol = intent.getStringExtra(KEY_NAME);
                if (mGroupSpinner.getVisibility() == View.GONE || mAddLayout.getVisibility() == View.GONE) {
                    onNewSymbolClicked(null);
                }
                if (mNameEditText != null) {
                    mNameEditText.setText(symbol);
                    mNameEditText.requestFocus();
                }
                mGroupSpinner.setSelection(mGroups.indexOf(group));
            }
        }
    };

    private ExpandableListView.OnChildClickListener mChildClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id) {
            final String symbol = (String) mListAdapter.getChild(groupPosition, childPosition);
            new CalculateMACD(getApplicationContext()).execute(symbol, CalculateMACD.TOAST);
            return true;
        }
    };

    @Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_macd);

		mGroupSpinner = (Spinner) findViewById(R.id.spinnerGroup);
        mAddLayout = findViewById(R.id.addLayout);
        mNameEditText = (EditText) findViewById(R.id.editTextNewSymbol);

		final ExpandableListView mListView = (ExpandableListView) findViewById(R.id.listView);

		mSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mGroups);
		mGroupSpinner.setAdapter(mSpinnerAdapter);

		mListAdapter = new ExpandableListAdapter(this, mGroups, mSymbols);
		mListView.setAdapter(mListAdapter);

		mListView.setOnChildClickListener(mChildClickListener);
	}

	public void onAddSymbolClicked(final View view) {
		mAddLayout.setVisibility(View.GONE);
		if (mGroupSpinner.getVisibility() == View.VISIBLE) {
			final String group = (String) mGroupSpinner.getSelectedItem();
			if (group != null) {
				final List<String> symbols = mSymbols.get(group);
				if (mNameEditText != null && mNameEditText.getText() != null
						&& !mNameEditText.getText().toString().equals(""))
					symbols.add(mNameEditText.getText().toString());
			}
		} else {
			if (mNameEditText != null && mNameEditText.getText() != null
					&& !mNameEditText.getText().toString().equals("")) {
				mGroups.add(mNameEditText.getText().toString());
				mSymbols.put(mNameEditText.getText().toString(), new ArrayList<String>());
			}
		}
		mListAdapter.notifyDataSetChanged();
		mSpinnerAdapter.notifyDataSetChanged();
	}

	public void onNewGroupClicked(final View view) {
		if (mGroupSpinner.getVisibility() == View.VISIBLE || mAddLayout.getVisibility() == View.GONE) {
			mGroupSpinner.setVisibility(View.GONE);
			if (mNameEditText != null) {
                mNameEditText.setText("");
                mNameEditText.setHint(R.string.group_name);
			}
            mAddLayout.setVisibility(View.VISIBLE);
		} else {
            mAddLayout.setVisibility(View.GONE);
		}
	}

	public void onNewSymbolClicked(final View view) {
		if (mGroupSpinner.getVisibility() == View.GONE || mAddLayout.getVisibility() == View.GONE) {
			mGroupSpinner.setVisibility(View.VISIBLE);
			if (mNameEditText != null) {
                mNameEditText.setText("");
                mNameEditText.setHint(R.string.symbol_name);
			}
            mAddLayout.setVisibility(View.VISIBLE);
		} else {
            mNameEditText.setVisibility(View.GONE);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		final SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
		editor.clear();
		String groupName;
		editor.putInt(KEY_COUNT, mGroups.size());
		List<String> symbols;
		for (int i = 0; i < mGroups.size(); i++) {
			groupName = mGroups.get(i);
			if (groupName.length() > 0) {
				editor.putString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_NAME, groupName);
				symbols = mSymbols.get(groupName);
				editor.putInt(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_COUNT, symbols.size());
				Collections.sort(symbols);
				for (int j = 0; j < symbols.size(); j++) {
					if (symbols.get(j).length() > 0) {
						editor.putString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_NAME,
								symbols.get(j));
					}
				}
			}
		}
		editor.commit();

        registerReceiver(mReceiver, new IntentFilter(ACTION_BROADCAST_REMOVE));
	}

	@Override
	public void onResume() {
		super.onResume();
		mGroups.clear();
		mSymbols.clear();
		final SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		int symbolCount;
		String groupName;
		List<String> symbols;
		final int groupCount = prefs.getInt(KEY_COUNT, 0);
		for (int i = 0; i < groupCount; i++) {
			groupName = prefs.getString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_NAME, "Group");
			mGroups.add(groupName);
			symbols = new ArrayList<String>();
			mSymbols.put(groupName, symbols);
			symbolCount = prefs.getInt(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_COUNT, 0);
			for (int j = 0; j < symbolCount; j++) {
				symbols.add(prefs.getString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_NAME,
						"-SYM-"));
			}
		}
		mListAdapter.notifyDataSetChanged();
		mSpinnerAdapter.notifyDataSetChanged();

        unregisterReceiver(mReceiver);
	}

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        prefs.getAll();

        File f = new File("symbols.data");
        if (f != null && f.exists()) {
            if (f.canWrite()) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(f));
                    String line;
                    for (String key : prefs.getAll().keySet()) {
                        Object val = prefs.getAll().get(key);
                        line = key + KEY_VALUE_SEPPARATOR + val + "\n";
                        out.write(line);
                    }
                    out.flush();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
