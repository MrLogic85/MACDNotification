package com.sleepyduck.macdnotification;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.sleepyduck.macdnotification.CalculateMACD.MACDListener;

public class ActivityMACD extends Activity {
	public static final String ACTION_BROADCAST_REMOVE = "ActivityMACD:action_broadcast_remove";

	private ExpandableListAdapter mListAdapter;
	private Spinner mGroupSpinner;
	private ArrayAdapter<String> mSpinnerAdapter;
	private View mAddLayout;
	private EditText mNameEditText;
	private DataController mDataController = new DataController();

	private MACDListener mMACDListener = new MACDListener() {

		@Override
		public void onMessage(String message) {
			Toast.makeText(ActivityMACD.this, message, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onCalculationComplete(Bundle bundle) {
			if (bundle.containsKey(CalculateMACD.DATA_MACD_LATEST)) {
				String group = bundle.getString(CalculateMACD.DATA_GROUP);
				String symbol = bundle.getString(CalculateMACD.DATA_SYMBOL);
				String data = String.format("Price %2.2f (%2.2f), MACD %2.2f (%2.2f)",
						bundle.getFloat(CalculateMACD.DATA_VALUE_LATEST),
						bundle.getFloat(CalculateMACD.DATA_VALUE_PREVIOUS),
						bundle.getFloat(CalculateMACD.DATA_MACD_LATEST),
						bundle.getFloat(CalculateMACD.DATA_MACD_PREVIOUS));
				mDataController.setSymbolData(group, symbol, data);
				mListAdapter.notifyDataSetChanged();
			}
		}
	};
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra(DataController.KEY_GROUP) && intent.hasExtra(DataController.KEY_NAME)) {
				String group = intent.getStringExtra(DataController.KEY_GROUP);
				String symbol = intent.getStringExtra(DataController.KEY_NAME);
				if (mGroupSpinner.getVisibility() == View.GONE || mAddLayout.getVisibility() == View.GONE) {
					onNewSymbolClicked(null);
				}
				if (mNameEditText != null) {
					mNameEditText.setText(symbol);
					mNameEditText.requestFocus();
				}
				mGroupSpinner.setSelection(mDataController.getGroupIndex(group));
			}
		}
	};

	private ExpandableListView.OnChildClickListener mChildClickListener =
			new ExpandableListView.OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView expandableListView, View view, int group, int symbol, long id) {
			String symbolName = mDataController.getSymbol(group, symbol);
			if (symbolName != null && symbolName.length() > 0) {
				Uri uri = Uri.parse("http://finance.yahoo.com/q/ta?s=" + symbolName + "&t=1y&l=on&z=l&q=l&p=e18%2Cb&a=m26-12-9%2Css&c=");
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(uri);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
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

		mSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mDataController.getGroups());
		mGroupSpinner.setAdapter(mSpinnerAdapter);

		mListAdapter = new ExpandableListAdapter(this, mDataController.getGroups(), mDataController.getSymbolsDataContainer());
		mListView.setAdapter(mListAdapter);
		mListView.setOnChildClickListener(mChildClickListener);

		if (savedInstanceState == null) {
			mDataController.loadFromFile(this);
			mDataController.load(this);
			for (String group : mDataController.getGroups()) {
				for (String[] symbolData : mDataController.getSymbols(group)) {
					new CalculateMACD(this, mMACDListener, group).execute(symbolData[0]);
				}
			}
			mDataController.clearSymbolData();
			mDataController.save(this);

			// TODO store symbol data in savedInstanceState, not on file
		}

		registerReceiver(mReceiver, new IntentFilter(ACTION_BROADCAST_REMOVE));
	}

	@Override
	public void onResume() {
		super.onResume();
		mDataController.load(this);
		mListAdapter.notifyDataSetChanged();
		mSpinnerAdapter.notifyDataSetChanged();

		registerReceiver(mReceiver, new IntentFilter(ACTION_BROADCAST_REMOVE));
	}

	@Override
	public void onPause() {
		super.onPause();
		mDataController.save(this);
		unregisterReceiver(mReceiver);
	}

	@Override
	public void onStop() {
		super.onStop();
		mDataController.saveToFile(this);
	}

	public void onAddSymbolClicked(final View view) {
		mAddLayout.setVisibility(View.GONE);
		if (mGroupSpinner.getVisibility() == View.VISIBLE) {
			// Add Symbol
			final String group = (String) mGroupSpinner.getSelectedItem();
			if (group != null
					&& mNameEditText != null
					&& mNameEditText.getText() != null
					&& !mNameEditText.getText().toString().equals("")) {
				String symbol = mNameEditText.getText().toString();
				mDataController.addSymbol(group, symbol);
				new CalculateMACD(this, mMACDListener, group).execute(symbol);
			}
		} else {
			// Add group
			if (mNameEditText != null && mNameEditText.getText() != null
					&& !mNameEditText.getText().toString().equals("")) {
				if (mDataController.containsGroup(mNameEditText.getText().toString())) {
					Toast.makeText(this, "That group alreay exists", Toast.LENGTH_LONG).show();
					mAddLayout.setVisibility(View.VISIBLE);
				} else {
					mDataController.addGroup(mNameEditText.getText().toString());
				}
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
			mAddLayout.setVisibility(View.GONE);
		}
	}
}
