package com.sleepyduck.macdnotification;

import java.util.LinkedList;
import java.util.List;

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
import com.sleepyduck.macdnotification.data.DataController;
import com.sleepyduck.macdnotification.data.Group;
import com.sleepyduck.macdnotification.data.Symbol;

public class ActivityMACD extends Activity {
	public static final String ACTION_BROADCAST_REMOVE = "ActivityMACD:action_broadcast_remove";
	public static final String DATA_REMOVED_SYMBOL = "removed_symbol";

	private ExpandableListAdapter mListAdapter;
	private Spinner mGroupSpinner;
	private ArrayAdapter<Group> mSpinnerAdapter;
	private View mAddLayout;
	private EditText mNameEditText;
	private DataController mDataController = new DataController();

	private MACDListener mMACDListener = new MACDListener() {

		@Override
		public void onMessage(String message) {
			Toast.makeText(ActivityMACD.this, message, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onCalculationComplete(Symbol symbol) {
			mListAdapter.notifyDataSetChanged();
		}
	};
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra(DATA_REMOVED_SYMBOL)) {
				Symbol symbol = intent.getParcelableExtra(DATA_REMOVED_SYMBOL);
				if (mGroupSpinner.getVisibility() == View.GONE || mAddLayout.getVisibility() == View.GONE) {
					onNewSymbolClicked(null);
				}
				if (mNameEditText != null) {
					mNameEditText.setText(symbol.getName());
					mNameEditText.requestFocus();
				}
			}
		}
	};

	private ExpandableListView.OnChildClickListener mChildClickListener =
			new ExpandableListView.OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView expandableListView, View view, int groupIndex, int symbolIndex, long id) {
			Symbol symbol = mDataController.getGroup(groupIndex).getSymbol(symbolIndex);
			Uri uri = Uri.parse("http://finance.yahoo.com/q/ta?s=" + symbol.getName() + "&t=1y&l=on&z=l&q=l&p=e18%2Cb&a=m26-12-9%2Css&c=");
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(uri);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
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

		mSpinnerAdapter = new ArrayAdapter<Group>(this, android.R.layout.simple_spinner_item, mDataController.getGroups());
		mGroupSpinner.setAdapter(mSpinnerAdapter);

		mListAdapter = new ExpandableListAdapter(this, mDataController.getGroups());
		mListView.setAdapter(mListAdapter);
		mListView.setOnChildClickListener(mChildClickListener);

		if (savedInstanceState == null) {
			mDataController.loadFromFile(this);
			List<Symbol> dataList = new LinkedList<Symbol>();
			for (Group group : mDataController.getGroups()) {
				dataList.addAll(group.getSymbols());
			}
			new CalculateMACD(this, mMACDListener).execute(dataList.toArray(new Symbol[dataList.size()]));
			mListAdapter.notifyDataSetChanged();
			mSpinnerAdapter.notifyDataSetChanged();
		}

		registerReceiver(mReceiver, new IntentFilter(ACTION_BROADCAST_REMOVE));
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mDataController.save(outState);
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
			if (mNameEditText != null
					&& mNameEditText.getText() != null
					&& !mNameEditText.getText().toString().equals("")) {
				String symbolName = mNameEditText.getText().toString();
				Symbol symbol = mDataController.addSymbol(mGroupSpinner.getSelectedItemPosition(), symbolName);
				new CalculateMACD(this, mMACDListener).execute(symbol);
			}
		} else {
			// Add group
			if (mNameEditText != null && mNameEditText.getText() != null
					&& !mNameEditText.getText().toString().equals("")) {
				if (mDataController.getGroupIndex(mNameEditText.getText().toString()) > -1) {
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
