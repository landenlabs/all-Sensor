/*
 * Unpublished Work © 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.all_sensor.ui.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom expandable list implemenation.
 */
@SuppressWarnings({"UnnecessaryLocalVariable", "unused"})
public class ExpandList<GG extends ExpandList.GroupItemsInfo> {

    public final LinkedHashMap<String, GG> groupGroupList = new LinkedHashMap<>();
    public final ArrayListEx<GG> grouplist = new ArrayListEx<>();
    public final CreateGroupItem<GG> creator;
    public ExpandableListView expListView;
    public ExpandListAdapter<GG> expandListAdapter;

    // ---------------------------------------------------------------------------------------------

    public ExpandList(CreateGroupItem<GG> creator) {
        this.creator = creator;
    }

    public ExpandableListView build(ExpandableListView expListView) {
        this.expListView = expListView;
        Context context = expListView.getContext();

        /*
        expListView.setOnGroupExpandListener(groupPosition -> {
            Toast.makeText(context, " Expanded", Toast.LENGTH_SHORT).show();
        });
        expListView.setOnGroupCollapseListener(groupPosition -> {
            Toast.makeText(context, " Collapsed", Toast.LENGTH_SHORT).show();
        });
        expListView.setOnGroupClickListener((parent, groupView, groupPosition, id) -> {
            Toast.makeText(context, " onGroupClick", Toast.LENGTH_SHORT).show();
            return false;
        });
        // I think this only works for ListView not ExpandableListView

        expListView.setOnItemClickListener((parent, view, position, id) -> {
            Toast.makeText(context, " onItemClick never called", Toast.LENGTH_SHORT).show();
        });

        expListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(context, " onItemSelected  called", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(context, " onNothingSelected  called", Toast.LENGTH_SHORT).show();
            }
        });
        */

        expListView.setOnChildClickListener((parent, childView, groupPosition, childPosition, id) -> {

            // int position = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
            // parent.setItemChecked(position, true);
            // parent.setSelectedChild(groupPosition, childPosition, true);

            expandListAdapter.setSelected(groupPosition, childPosition);
            childView.invalidate();

            if (false) {
                // int cp = (int) expandListAdapter.getChildId(groupPosition, childPosition);
                final GG gpInfo = expandListAdapter.groupList.get(groupPosition);
                Toast.makeText(context, gpInfo.name + " " + gpInfo.getChildren().get(childPosition).name
                        , Toast.LENGTH_SHORT).show();
            }

            return false;
        });

        expandListAdapter = new ExpandListAdapter<>(context, grouplist);
        expListView.setAdapter(expandListAdapter);
        return expListView;
    }

    // Add group and child to group.
    public GG addGroup(
            String groupName,
            @ColorInt int selectionColor,
            @ColorInt int backgroundColor,
            int showChildIdx) {

        // Check the hashmap if the group already exists
        GG groupItemsInfo = groupGroupList.get(groupName);

        if (groupItemsInfo == null) {
            // Add the group if doesn't exists
            // groupItemsInfo = new GroupItemsInfo(grouplist.size(), groupName, selectionColor, backgroundColor, showChildIdx);
            groupItemsInfo = creator.create(grouplist.size(), groupName, selectionColor, backgroundColor, showChildIdx);
            groupGroupList.put(groupName, groupItemsInfo);
            grouplist.add(groupItemsInfo);
        }
        return groupItemsInfo;
    }

    public void expandGroups(boolean expand, int... grpIdxList) {
        for (int grpIdx : grpIdxList) {
            if (expand)
                expListView.expandGroup(grpIdx);
            else
                expListView.collapseGroup(grpIdx);
        }
    }

    public void setSelectedChild(int groupIdx, int childIdx, boolean expandGroup) {
        if (expListView.isLaidOut()) {
            if (expandGroup) expListView.expandGroup(groupIdx);

        } else {
            expListView.post(() -> {
                if (expandGroup) expListView.expandGroup(groupIdx);
            });
        }
        expandListAdapter.setSelected(groupIdx, childIdx);
        expandListAdapter.notifyDataSetChanged();
    }

    @Nullable
    public ChildItemsInfo getSelected(String groupName) {
        GroupItemsInfo groupItemInfo = groupGroupList.get(groupName);
        ChildItemsInfo childItemsInfo = null;
        if (groupItemInfo != null) {
            int childIdx = expandListAdapter.getSelected(groupItemInfo.index);
            childItemsInfo = groupItemInfo.children.get(childIdx);
        }
        return childItemsInfo;
    }

    public ChildItemsInfo getSelected(int idx) {
        GroupItemsInfo groupItemInfo = grouplist.get(idx);
        int childIdx = expandListAdapter.getSelected(groupItemInfo.index);
        ChildItemsInfo childItemsInfo = groupItemInfo.children.get(childIdx);
        return childItemsInfo;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ExpandList.GroupItemsInfo add(Iterator<Map.Entry<String, Object>> itr, ExpandList.GroupItemsInfo group) {
        while (itr.hasNext()) {
            Map.Entry<String, Object> entry = itr.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                group = addGroup(key,
                        0x10ffffff,
                        0x10ffffff,
                        0);
                Map<String, Object> map2 = (Map) value;
                add(map2.entrySet().iterator(), group);
            } else {
                group.addChild(key + " " + value);
            }
        }
        return group;
    }

    public interface CreateGroupItem<GG extends ExpandList.GroupItemsInfo> {
        GG create(int grpIdx, String groupName, @ColorInt int selectionColor,
                  @ColorInt int backgroundColor, int showChildIdx);
    }

    // =============================================================================================
    public static class ChildItemsInfo {
        public final String name;
        public Object value = null;

        public ChildItemsInfo(String name) {
            this.name = name;
        }

        public ChildItemsInfo(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public View getView(
                ExpandListAdapter<? extends ExpandList.GroupItemsInfo> expandListAdapter,
                int groupPosition,
                int childPosition,
                boolean isLastChild,
                View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                Context context = parent.getContext();
                LayoutInflater infalInflater = LayoutInflater.from(context);
                convertView = infalInflater.inflate(R.layout.host_list_child, parent, false);
            }
            TextView childText = convertView.findViewById(R.id.host_child1);
            childText.setText(name);
            return convertView;
        }
    }

    // =============================================================================================
    public static class GroupItemsInfo {
        public static final int SHOW_SELECTED = -1;
        public static final int SHOW_NONE = -2;

        protected final String name;
        protected final int selectionColor;
        protected final int backgroundColor;
        protected final int showChildIdx;    // SHOW_SELECTED;
        protected final ArrayListEx<ChildItemsInfo> children = new ArrayListEx<>();
        protected String value = "";
        protected final int index;


        public GroupItemsInfo(int idx, String name, @ColorInt int selectionColor, @ColorInt int backgroundColor, int showChildIdx) {
            this.index = idx;
            this.name = name;
            this.selectionColor = selectionColor;
            this.backgroundColor = backgroundColor;
            this.showChildIdx = showChildIdx;
        }

        @NonNull
        public ArrayListEx<ChildItemsInfo> getChildren() {
            return children;
        }

        public ChildItemsInfo createChild(String name, Object values) {
            return new ChildItemsInfo(name, values);
        }

        public ChildItemsInfo addChild(String childName, Object... values) {
            ChildItemsInfo childInfo = createChild(childName, values);
            getChildren().add(childInfo);
            return childInfo;
        }

        /*
        public ChildItemsInfo addChild(String childName, @ColorInt int selectionColor) {
            return getChildren().add(createChild(childName));
        }
         */

        public void addChildIf(String childName, boolean add) {
            if (add) {
                getChildren().add(createChild(childName, null));
            }
        }

        public int getColor(boolean isSelected, int childPosition) {
            return isSelected ? selectionColor : backgroundColor;
        }

        public int showChild(int selectedIdx) {
            if (showChildIdx == GroupItemsInfo.SHOW_SELECTED) {
                return selectedIdx;
            }
            return showChildIdx;
        }

        public View getView(
                ExpandListAdapter<? extends ExpandList.GroupItemsInfo> expandListAdapter, int groupPosition,
                boolean isExpanded, View convertView, ViewGroup parent,
                int[] selected) {
            if (convertView == null) {
                Context context = parent.getContext();
                LayoutInflater inf = LayoutInflater.from(context);
                convertView = inf.inflate(R.layout.host_list_group, parent, false);
            }

            TextView groupText1 = convertView.findViewById(R.id.host_group1);
            groupText1.setText(name);
            TextView groupText2 = convertView.findViewById(R.id.host_group2);
            if (groupText2 != null) {
                int selectedIdx = showChild(selected[groupPosition]);
                ChildItemsInfo childItemsInfo = children.get(selectedIdx, null);
                if (childItemsInfo != null) {
                    groupText2.setText(childItemsInfo.name);
                }
            }
            convertView.setBackgroundColor(backgroundColor);
            return convertView;
        }
    }

    // =============================================================================================

    public static class ExpandListAdapter<GG extends ExpandList.GroupItemsInfo> extends BaseExpandableListAdapter {

        // private Context context;
        protected final ArrayListEx<GG> groupList;
        protected final int[] selected;

        ExpandListAdapter(Context context, ArrayListEx<GG> groupList) {
            // this.context = context;
            this.groupList = groupList;
            selected = new int[groupList.size()];
            Arrays.fill(selected, 0);
        }

        /*
        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }
        */

        @Override
        public int getGroupCount() {
            return groupList.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            ArrayListEx<ChildItemsInfo> productList = groupList.get(groupPosition).getChildren();
            return productList.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            ArrayListEx<ChildItemsInfo> productList = groupList.get(groupPosition).getChildren();
            return productList.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition; //  + 100 * groupPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(
                int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            GroupItemsInfo groupItemsInfo = (GroupItemsInfo) getGroup(groupPosition);

            return groupItemsInfo.getView(this, groupPosition, isExpanded, convertView, parent, selected);
        }

        @Override
        public View getChildView(
                int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            ChildItemsInfo childInfo = (ChildItemsInfo) getChild(groupPosition, childPosition);
            convertView = childInfo.getView(this, groupPosition, childPosition, isLastChild, convertView, parent);

            boolean isSelected = selected[groupPosition] == childPosition;
            int bgColor = ((GroupItemsInfo) getGroup(groupPosition)).getColor(isSelected, childPosition);
            convertView.setBackgroundColor(bgColor);

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public void setSelected(int groupPosition, int childPosition) {
            if (selected[groupPosition] != childPosition) {
                selected[groupPosition] = childPosition;
                notifyDataSetChanged();
            }
        }

        public int getSelected(int groupPosition) {
            return selected[groupPosition];
        }

        /*
        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void onGroupExpanded(int groupPosition) {
        }

        @Override
        public void onGroupCollapsed(int groupPosition) {
        }

        @Override
        public long getCombinedChildId(long groupId, long childId) {
            return 0;
        }

        @Override
        public long getCombinedGroupId(long groupId) {
            // return super.getCombinedGroupId(groupId);
            return 0;
        }
        */

    }
}
