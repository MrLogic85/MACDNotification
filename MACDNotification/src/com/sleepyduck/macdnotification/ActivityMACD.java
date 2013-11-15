package com.sleepyduck.macdnotification;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityMACD extends Activity {
    public static final String KEY_INDEX_SYMBOLS = "Indexes";
    public static final String KEY_STOCK_SYMBOLS = "Stocks";
    public static final String KEY_ETF_SYMBOLS = "ETFs";

    private List<String> mGroups;
    private Map<String, List<String>> mSymbols;
    private ExpandableListAdapter mListAdapter;
    private Spinner mGroupSpinner;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_macd);

        mGroupSpinner = (Spinner) findViewById(R.id.spinnerGroup);
        ExpandableListView mListView = (ExpandableListView) findViewById(R.id.listView);

        setupSpiner();
        createGroupList();
        loadSymbols();

        mListAdapter = new ExpandableListAdapter(this, mGroups, mSymbols);
        mListView.setAdapter(mListAdapter);

        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {
                final String symbol = (String) mListAdapter.getChild(
                        groupPosition, childPosition);
                new CalculateMACD(getApplicationContext()).execute(symbol, CalculateMACD.TOAST);
                return true;
            }
        });
    }

    private void setupSpiner() {
        SpinnerAdapter spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                new String[] {KEY_INDEX_SYMBOLS, KEY_STOCK_SYMBOLS, KEY_ETF_SYMBOLS});
        mGroupSpinner.setAdapter(spinnerAdapter);
    }

    private void loadSymbols() {
        SharedPreferences prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        if (mSymbols == null)
            mSymbols = new LinkedHashMap<String, List<String>>();
        Set<String> symbols = prefs.getStringSet(KEY_INDEX_SYMBOLS, new HashSet<String>());
        mSymbols.put(mGroups.get(0), new ArrayList<String>(symbols));
        symbols = prefs.getStringSet(KEY_STOCK_SYMBOLS, new HashSet<String>());
        mSymbols.put(mGroups.get(1), new ArrayList<String>(symbols));
        symbols = prefs.getStringSet(KEY_ETF_SYMBOLS, new HashSet<String>());
        mSymbols.put(mGroups.get(2), new ArrayList<String>(symbols));
    }

    private void createGroupList() {
        mGroups = new ArrayList<String>();
        mGroups.add(KEY_INDEX_SYMBOLS);
        mGroups.add(KEY_STOCK_SYMBOLS);
        mGroups.add(KEY_ETF_SYMBOLS);
    }

    public void onResume() {
        super.onResume();
        loadSymbols();
        mListAdapter.notifyDataSetChanged();
    }

    public void onPause(){
        super.onPause();
        SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
        editor.putStringSet(KEY_INDEX_SYMBOLS, new HashSet<String>(mSymbols.get(mGroups.get(0))));
        editor.putStringSet(KEY_STOCK_SYMBOLS, new HashSet<String>(mSymbols.get(mGroups.get(1))));
        editor.putStringSet(KEY_ETF_SYMBOLS, new HashSet<String>(mSymbols.get(mGroups.get(2))));
        editor.commit();
    }

    public void onNewSymbolClicked(View view) {
        View addLayout = findViewById(R.id.addLayout);
        addLayout.setVisibility(View.VISIBLE);
    }

    public void onAddSymbolClicked(View view) {
        View addLayout = findViewById(R.id.addLayout);
        addLayout.setVisibility(View.GONE);
        String group = (String) mGroupSpinner.getSelectedItem();
        List<String> symbols = mSymbols.get(group);
        EditText symbolText = (EditText) findViewById(R.id.editTextNewSymbol);
        if (symbolText != null && symbolText.getText() != null)
            symbols.add(symbolText.getText().toString());
        mListAdapter.notifyDataSetChanged();
    }
}
