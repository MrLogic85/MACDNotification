package com.sleepyduck.macdnotification;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Fredrik Metcalf
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
	private final Activity mContext;
	private final Map<String, List<String[]>> mSymbols;
	private final List<String> mGroups;

	public ExpandableListAdapter(Activity context, List<String> groups, Map<String, List<String[]>> symbols) {
		mContext = context;
		mGroups = groups;
		mSymbols = symbols;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mSymbols.get(mGroups.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		final String[] symbol = (String[]) getChild(groupPosition, childPosition);
		LayoutInflater inflater = mContext.getLayoutInflater();

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.list_item, null);
		}

		if (convertView != null) {
			TextView symbolText = (TextView) (convertView.findViewById(R.id.textViewSymbol));
			TextView dataText = (TextView) (convertView.findViewById(R.id.textViewSymbolData));

			ImageView delete = (ImageView) convertView.findViewById(R.id.imageViewDelete);
			delete.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(mContext)
					.setMessage(R.string.ask_delete_symbol)
					.setCancelable(false)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							removeChild(groupPosition, childPosition);
						}
					})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					})
					.create()
					.show();
				}
			});

			if (symbol.length > 0)
				symbolText.setText(symbol[0]);
			if (symbol.length > 1)
				dataText.setText(symbol[1]);
			return convertView;
		}
		return null;
	}

	private void removeChild(int groupPosition, int childPosition) {
		String group = mGroups.get(groupPosition);
		List<String[]> children = mSymbols.get(group);
		String[] symbol = children.remove(childPosition);
		notifyDataSetChanged();
		Intent intent = new Intent(ActivityMACD.ACTION_BROADCAST_REMOVE);
		intent.putExtra(ActivityMACD.KEY_GROUP, group);
		intent.putExtra(ActivityMACD.KEY_NAME, symbol);
		mContext.sendBroadcast(intent);
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mSymbols.get(mGroups.get(groupPosition)).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mGroups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mGroups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		String laptopName = (String) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.list_group_item,
					null);
		}
		TextView item;
		if (convertView != null) {
			item = (TextView) convertView.findViewById(R.id.textViewGroupName);
			if (item != null) {
				item.setTypeface(null, Typeface.BOLD);
				item.setText(laptopName);
			}
		}
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
