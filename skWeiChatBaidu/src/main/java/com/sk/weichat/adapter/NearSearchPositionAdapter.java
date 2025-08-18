package com.sk.weichat.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sk.weichat.R;
import com.sk.weichat.map.MapHelper;
import com.sk.weichat.util.SkinUtils;

import java.util.ArrayList;
import java.util.List;

public class NearSearchPositionAdapter extends RecyclerView.Adapter<NearSearchPositionAdapter.NearPositionViewHolder> {
    private List<MapHelper.Place> places = new ArrayList<>();
    private double latitudeSelected, longitudeSelected;

    private OnItemClickedListener onItemClickedListener;
    private Context mContext;

    public NearSearchPositionAdapter(Context context) {
        this.mContext = context;
    }

    public void setOnItemClickedListener(OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }

    public void setData(List<MapHelper.Place> data) {
        this.places = data;
        if (places == null) {
            places = new ArrayList<>();
        }
        latitudeSelected = 0.0;
        longitudeSelected = 0.0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NearPositionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.near_position_adapter, parent, false);
        return new NearPositionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NearPositionViewHolder holder, int position) {
        MapHelper.Place place = places.get(position);
        holder.check.setVisibility(View.GONE);
        if (place != null) {
            holder.map_name_tv.setText(place.getName());
            holder.map_detail_tv.setText(place.getAddress());
            if (place.getLatLng().getLatitude() == latitudeSelected
                    && place.getLatLng().getLongitude() == longitudeSelected) {
                holder.check.setVisibility(View.VISIBLE);
                ImageViewCompat.setImageTintList(holder.check, ColorStateList.valueOf(SkinUtils.getSkin(mContext).getAccentColor()));
            } else {
                holder.check.setVisibility(View.GONE);
            }

            holder.item_ll.setOnClickListener(v -> {
                if (onItemClickedListener != null) {
                    latitudeSelected = place.getLatLng().getLatitude();
                    longitudeSelected = place.getLatLng().getLongitude();
                    onItemClickedListener.onItemClick(place);
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public interface OnItemClickedListener {
        void onItemClick(MapHelper.Place place);
    }

    class NearPositionViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout item_ll;
        TextView map_name_tv;
        TextView map_detail_tv;
        ImageView check;

        NearPositionViewHolder(@NonNull View itemView) {
            super(itemView);
            item_ll = itemView.findViewById(R.id.item_ll);
            map_name_tv = itemView.findViewById(R.id.map_name_tv);
            map_detail_tv = itemView.findViewById(R.id.map_detail_tv);
            check = itemView.findViewById(R.id.check);
        }
    }
}
