package com.ph.bluetooth.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * Created by 86119 on 2017/3/20.
 */

public abstract class AutoAdapter<T> extends BaseAdapter {
    protected List<T> list;

    public AutoAdapter(List<T> list) {
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setList(List<T> list) {
        this.list = list;
        this.notifyDataSetChanged();
    }

}
