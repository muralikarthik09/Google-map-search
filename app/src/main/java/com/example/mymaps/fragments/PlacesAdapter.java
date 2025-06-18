package com.example.mymaps.fragments;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.mymaps.R;
import com.google.android.libraries.places.api.model.AutocompletePrediction;

import java.util.List;

public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.ViewHolder> {

    private List<AutocompletePrediction> places;
    private onItemClickListener listener;

    public void setData(List<AutocompletePrediction> places) {
        this.places = places;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.item_number);
        }
    }

    public PlacesAdapter(onItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.search_place_list_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        AutocompletePrediction place = places.get(position);
        String text = String.valueOf(place.getPrimaryText(null));
        if (!TextUtils.isEmpty(place.getSecondaryText(null))) {
            text = text + "\n" + place.getSecondaryText(null);
        }
        viewHolder.textView.setText(text);
        viewHolder.itemView.setOnClickListener(v -> listener.onItemClick(place));
    }

    @Override
    public int getItemCount() {
        if (places != null && !places.isEmpty())
            return places.size();
        else return 0;
    }
}