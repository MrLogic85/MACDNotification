package com.sleepyduck.macdnotification;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sleepyduck.macdnotification.data.Group;
import com.sleepyduck.macdnotification.data.Symbol;

/**
 * @author Fredrik Metcalf
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
	private final Activity mContext;
	private final List<Group> mGroups;

	public ExpandableListAdapter(Activity context, List<Group> groups) {
		mContext = context;
		mGroups = groups;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mGroups.get(groupPosition).getSymbol(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		final Symbol symbol = (Symbol) getChild(groupPosition, childPosition);
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

			symbolText.setText(symbol.getName());
			if (symbol.getMACD() > -1f) {
				String text = String.format("Price %2.2f (%2.2f), MACD %2.2f (%2.2f)",
						symbol.getValue(),
						symbol.getValueOld(),
						symbol.getMACD(),
						symbol.getMACDOld());
				dataText.setText(text);

				if (symbol.getMACD() >= 0f) {
					symbolText.setTextColor(Color.GREEN);
				} else {
					symbolText.setTextColor(Color.RED);
				}
			}
			return convertView;
		}
		return null;
	}

	private void removeChild(int groupPosition, int childPosition) {
		Group group = mGroups.get(groupPosition);
		Symbol symbol = group.removeSymbol(childPosition);
		notifyDataSetChanged();
		Intent intent = new Intent(ActivityMACD.ACTION_BROADCAST_REMOVE);
		intent.putExtra(ActivityMACD.DATA_REMOVED_SYMBOL, symbol);
		mContext.sendBroadcast(intent);
	}

	private void removeGroup(int location) {
		if (mGroups.size() > location) {
			mGroups.remove(location);
			notifyDataSetChanged();
		}
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mGroups.get(groupPosition).getSymbols().size();
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
	public View getGroupView(final int groupPosition, boolean isExpanded,
			View groupView, ViewGroup parent) {
		String groupName = (String) getGroup(groupPosition);

		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		groupView = groupView != null ? groupView : inflater.inflate(R.layout.list_group_item, null);

		TextView item;
		if (groupView != null) {
			item = (TextView) groupView.findViewById(R.id.textViewGroupName);
			if (item != null) {
				item.setTypeface(null, Typeface.BOLD);
				item.setText(groupName);
			}
		}

		ImageView deleteGroup = (ImageView) groupView.findViewById(R.id.imageViewDeleteGroup);
		deleteGroup.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(mContext)
				.setMessage(R.string.ask_delete_group)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						removeGroup(groupPosition);
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
		return groupView;
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
