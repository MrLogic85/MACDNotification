package com.sleepyduck.macdnotification;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;

public class DataController {
	private static final String LOG_TAG = DataController.class.getSimpleName();

	public static final String KEY_GROUP = "group";
	public static final String KEY_COUNT = "count";
	public static final String KEY_NAME = "name";
	public static final String KEY_DATA = "data";
	public static final String KEY_SEPPARATOR = ":";
	private static final String KEY_VALUE_SEPPARATOR = "<=>";

	private final List<String> mGroups = Collections.synchronizedList(new ArrayList<String>());
	private final Map<String, List<String[]>> mSymbols = Collections
			.synchronizedMap(new LinkedHashMap<String, List<String[]>>());

	public void setSymbolData(String group, String symbol, String data) {
		for (String[] values : mSymbols.get(group)) {
			if (values.length > 1 && values[0].equals(symbol)) {
				values[1] = data;
			}
		}
	}

	public int getGroupIndex(String group) {
		return mGroups.indexOf(group);
	}

	public String getSymbol(int group, int symbol) {
		if (mGroups.size() > group
				&& mSymbols.containsKey(mGroups.get(group))
				&& mSymbols.get(mGroups.get(group)).size() > symbol)
			return mSymbols.get(mGroups.get(group)).get(symbol)[0];
		else
			return null;
	}

	public List<String> getGroups() {
		return mGroups;
	}

	public Map<String, List<String[]>> getSymbolsDataContainer() {
		return mSymbols;
	}

	public void addSymbol(String group, String symbol) {
		List<String[]> groupSymbols = mSymbols.get(group);
		if (groupSymbols != null) {
			groupSymbols.add(createSymbolData(symbol, ""));
		}
	}

	private String[] createSymbolData(String symbol, String data) {
		return new String[] {symbol, data};
	}

	public boolean containsGroup(String string) {
		return mGroups.contains(string);
	}

	public void addGroup(String groupName) {
		mGroups.add(groupName);
		mSymbols.put(groupName, new ArrayList<String[]>());
	}

	public List<String[]> getSymbols(String group) {
		return mSymbols.get(group);
	}

	public void clearSymbolData() {
		for (List<String[]> groupSymbols : mSymbols.values()) {
			for (String[] symbolData : groupSymbols) {
				symbolData[1] = "";
			}
		}
	}

	public List<String> getAllSymbols() {
		List<String> symbols = new LinkedList<String>();
		for (List<String[]> groupSymbols : mSymbols.values()) {
			for (String[] symbolData : groupSymbols) {
				symbols.add(symbolData[0]);
			}
		}
		return symbols;
	}

	public void loadFromFile(Context context) {
		Editor prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).edit();
		prefs.clear();
		try {
			FileInputStream in = null;
			if (isExternalStorageReadable()) {
				try {
					File file = getExternalStorageFile(context);
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
				String[] keyVal = line.split(KEY_VALUE_SEPPARATOR);
				if (keyVal.length > 1) {
					if (keyVal[0].contains(KEY_COUNT)) {
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

	public void load(Context context) {
		mGroups.clear();
		mSymbols.clear();
		final SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
		int symbolCount;
		String groupName;
		List<String[]> symbols;
		final int groupCount = prefs.getInt(KEY_COUNT, 0);
		for (int i = 0; i < groupCount; i++) {
			groupName = prefs.getString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_NAME, "Group");
			mGroups.add(groupName);
			symbols = new ArrayList<String[]>();
			mSymbols.put(groupName, symbols);
			symbolCount = prefs.getInt(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_COUNT, 0);
			for (int j = 0; j < symbolCount; j++) {
				String name = prefs.getString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_NAME, "");
				if (name.equals("")) {
					name = prefs.getString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_SEPPARATOR + KEY_NAME, "");
				}
				String data = prefs.getString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_SEPPARATOR + KEY_DATA, "");
				symbols.add(createSymbolData(name, data));
			}
		}
	}

	public void save(Context context) {
		final SharedPreferences.Editor editor = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE).edit();
		editor.clear();
		String groupName;
		editor.putInt(KEY_COUNT, mGroups.size());
		List<String[]> symbols;
		for (int i = 0; i < mGroups.size(); i++) {
			groupName = mGroups.get(i);
			if (groupName.length() > 0) {
				editor.putString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_NAME, groupName);
				symbols = mSymbols.get(groupName);
				editor.putInt(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + KEY_COUNT, symbols.size());
				Collections.sort(symbols, new Comparator<String[]>() {
					@Override
					public int compare(String[] lhs, String[] rhs) {
						if (lhs.length > 0 && rhs.length > 0)
							return lhs[0].compareTo(rhs[0]);
						else
							return lhs.length - rhs.length;
					}});
				for (int j = 0; j < symbols.size(); j++) {
					if (symbols.get(j).length > 0 && symbols.get(j)[0].length() > 0) {
						editor.putString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_SEPPARATOR + KEY_NAME,
								symbols.get(j)[0]);
						if (symbols.get(j)[1] != null) {
							editor.putString(KEY_GROUP + KEY_SEPPARATOR + i + KEY_SEPPARATOR + j + KEY_SEPPARATOR + KEY_DATA,
									symbols.get(j)[1]);
						}
					}
				}
			}
		}
		editor.commit();
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
				line = key + KEY_VALUE_SEPPARATOR + val + "\n";
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
