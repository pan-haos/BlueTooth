package com.ph.bluetooth.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ph.bluetooth.MainActivity;
import com.ph.bluetooth.R;

import java.util.List;

/**
 * Created by 86119 on 2017/3/20.
 */

public class BlueAdapter extends AutoAdapter<BluetoothDevice> {


    public BlueAdapter(List<BluetoothDevice> list) {
        super(list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = View.inflate(MainActivity.context, R.layout.item, null);
            holder = new ViewHolder();
            holder.tvName = (TextView) convertView.findViewById(R.id.tvName);
            holder.tvAddress = (TextView) convertView.findViewById(R.id.tvAddress);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice device = list.get(position);
        if (null != device) {
            holder.tvName.setText(device.getName());
            holder.tvAddress.setText(device.getAddress());
        }
        return convertView;
    }

    class ViewHolder {
        TextView tvName;
        TextView tvAddress;
    }

}
