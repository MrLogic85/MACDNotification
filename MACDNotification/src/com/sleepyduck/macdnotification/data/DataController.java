package com.sleepyduck.macdnotification.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class DataController {
	private static final String LOG_TAG = DataController.class.getSimpleName();
	private static final String PARCELABLE_DATA = "parcelable_data";

	private final List<Group> mGroups = Collections.synchronizedList(new ArrayList<Group>());

	public Symbol getSymbol(String groupName, String symbolName, String data) {
		int groupIndex = getGroupIndex(groupName);
		if (groupIndex > -1) {
			if (mGroups.get(groupIndex).getName().equals(groupName)) {
				return mGroups.get(groupIndex).getSymbol(symbolName);
			}
		}
		return null;
	}

	public Symbol getSymbol(String group, int symbol) {
		int groupIndex = getGroupIndex(group);
		if (groupIndex > -1) {
			return mGroups.get(groupIndex).getSymbol(symbol);
		}
		return null;
	}

	public Group getGroup(int i) {
		if (mGroups.size() > i) {
			return mGroups.get(i);
		}
		return null;
	}

	public int getGroupIndex(String group) {
		return mGroups.indexOf(group);
	}

	public List<Group> getGroups() {
		return mGroups;
	}

	public Symbol addSymbol(int groupIndex, String symbol) {
		if (mGroups.size() > groupIndex) {
			Group group = mGroups.get(groupIndex);
			return group.addSymbol(symbol);
		}
		return null;
	}

	public Group addGroup(String groupName) {
		Group group = new Group(groupName);
		mGroups.add(group);
		return group;
	}

	public List<Symbol> getAllSymbols() {
		List<Symbol> symbols = new LinkedList<Symbol>();
		for (Group group : mGroups) {
			symbols.addAll(group.getSymbols());
		}
		return symbols;
	}

	public void loadFromFile_1(Context context) {
		Editor prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).edit();
		prefs.clear();
		try {
			FileInputStream in = null;
			if (isExternalStorageReadable()) {
				try {
					File dir = context.getExternalFilesDir("data");
					if (!dir.exists() && !dir.mkdirs()) {
						Log.e(LOG_TAG, "Failed to create directory");
					}
					File file = new File(dir, "symbols.data");
					if (file != null && file.exists()) {
						in = new FileInputStream(file);
					}
				} catch (IOException e) {
					Log.e(LOG_TAG, "", e);
				}
			}
			if (in == null) {
				in = context.openFileInput("symbols.data");
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			String line;
			while ((line = reader.readLine()) != null) {
				String[] keyVal = line.split("<=>");
				if (keyVal.length > 1) {
					if (keyVal[0].contains("count")) {
						try {
							prefs.putInt(keyVal[0], Integer.parseInt(keyVal[1]));
						} catch (NumberFormatException e) {
							Log.e(LOG_TAG, "", e);
						}
					} else {
						prefs.putString(keyVal[0], keyVal[1]);
					}
				}
			}
			prefs.commit();
			in.close();
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "", e);
		} catch (IOException e) {
			Log.e(LOG_TAG, "", e);
		}
	}

	public void load_1(Context context) {
		mGroups.clear();
		final SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		final int groupCount = prefs.getInt("count", 0);
		for (int i = 0; i < groupCount; i++) {
			String groupName = prefs.getString("group:" + i + ":name", "Group");
			Group group = new Group(groupName);
			mGroups.add(group);
			int symbolCount = prefs.getInt("group:" + i + ":count", 0);
			for (int j = 0; j < symbolCount; j++) {
				String name = prefs.getString("group:" + i + ":" + j + "name", "");
				if (name.equals("")) {
					name = prefs.getString("group:" + i + ":" + j + ":name", "");
				}
				group.addSymbol(name);
			}
		}
	}

	public void loadFromFile(Context context) {
		loadFromFile_1(context);
	}

	public void load(Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.containsKey(PARCELABLE_DATA)) {
			Collections.addAll(mGroups, (Group[]) savedInstanceState.getParcelableArray(PARCELABLE_DATA));
		}
	}

	public void save(Bundle outState) {
		outState.putParcelableArray(PARCELABLE_DATA, mGroups.toArray(new Group[mGroups.size()]));
	}

	public void saveToFile(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		try {
			OutputStreamWriter out = null;
			if (isExternalStorageWritable()) {
				try {
					File file = getExternalStorageFile(context);
					if (file != null) {
						out = new OutputStreamWriter(new FileOutputStream(file));
					}
				} catch (IOException e) {
					Log.e(LOG_TAG, "", e);
				}
			}
			if (out == null) {
				out = new OutputStreamWriter(context.openFileOutput("symbols.data", Context.MODE_PRIVATE));
			}
			String line;
			for (String key : prefs.getAll().keySet()) {
				Object val = prefs.getAll().get(key);
				line = key + ":" + val + "\n";
				out.write(line);
			}
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "", e);
		} catch (IOException e) {
			Log.e(LOG_TAG, "", e);
		}
	}

	public File getExternalStorageFile(Context context) throws IOException {
		File dir = context.getExternalFilesDir("data");
		if (!dir.exists() && !dir.mkdirs()) {
			Log.e(LOG_TAG, "Failed to create directory");
		}
		return new File(dir, "symbols.data");
	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) ||
				Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}
}
