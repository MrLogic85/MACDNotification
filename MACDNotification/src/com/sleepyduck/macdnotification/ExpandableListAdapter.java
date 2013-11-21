package com.sleepyduck.macdnotification;

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

import java.util.List;
import java.util.Map;

/**
 * @author Fredrik Metcalf
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private final Activity mContext;
    private final Map<String, List<String>> mSymbols;
    private final List<String> mGroups;

    public ExpandableListAdapter(Activity context, List<String> groups, Map<String, List<String>> symbols) {
        mContext = context;
        mGroups = groups;
        mSymbols = symbols;
    }

    public Object getChild(int groupPosition, int childPosition) {
        return mSymbols.get(mGroups.get(groupPosition)).get(childPosition);
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        final String symbol = (String) getChild(groupPosition, childPosition);
        LayoutInflater inflater = mContext.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
        }

        if (convertView != null) {
            TextView item = (TextView) (convertView.findViewById(R.id.textViewSymbol));

            ImageView delete = (ImageView) convertView.findViewById(R.id.imageViewDelete);
            delete.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    new AlertDialog.Builder(mContext)
                            .setMessage(R.string.ask_delete_symbol)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.yes,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            removeChild(groupPosition, childPosition);
                                        }
                                    })
                            .setNegativeButton(android.R.string.no,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .create()
                            .show();
                }
            });

            item.setText(symbol);
            return convertView;
        }
        return null;
    }

    private void removeChild(int groupPosition, int childPosition) {
        String group = mGroups.get(groupPosition);
        List<String> children = mSymbols.get(group);
        String symbol = children.remove(childPosition);
        notifyDataSetChanged();
        Intent intent = new Intent(ActivityMACD.ACTION_BROADCAST_REMOVE);
        intent.putExtra(ActivityMACD.KEY_GROUP, group);
        intent.putExtra(ActivityMACD.KEY_NAME, symbol);
        mContext.sendBroadcast(intent);
    }

    public int getChildrenCount(int groupPosition) {
        return mSymbols.get(mGroups.get(groupPosition)).size();
    }

    public Object getGroup(int groupPosition) {
        return mGroups.get(groupPosition);
    }

    public int getGroupCount() {
        return mGroups.size();
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

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

    public boolean hasStableIds() {
        return true;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
