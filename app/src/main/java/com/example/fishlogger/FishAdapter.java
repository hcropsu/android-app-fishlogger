package com.example.fishlogger;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FishAdapter extends RecyclerView.Adapter<FishViewHolder> {
    private List<Fish> fishList;
    private OnClickListenerInterface onClickListenerInterface;

    // The Adapter's constructor gets the OnClickListenerInterface as a parameter and is assigned
    // to the member variable 'onClickListenerInterface'
    // Later on the onClickListenerInterface is passed on to the FishViewHolder's constructor
    // in onCreateViewHolder
    public FishAdapter(List<Fish> fishList, OnClickListenerInterface onClickListenerInterface) {
        this.fishList = fishList;
        this.onClickListenerInterface = onClickListenerInterface;
    }

    // Set the Adapter's list and use DiffUtil to compare difference between old and new list
    public void setFishList(List<Fish> newFishList){
        FishlistDiffCallback fishlistDiffCallback = new FishlistDiffCallback(this.fishList, newFishList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(fishlistDiffCallback);
        this.fishList.clear();
        this.fishList.addAll(newFishList);
        diffResult.dispatchUpdatesTo(this);
    }

    // Return the Fish by position
    public Fish getFishAtPosition(int position){
        return fishList.get(position);
    }

    // Return the list of Fish
    public List<Fish> getFishList(){
        return fishList;
    }

    @NonNull
    @Override
    public FishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.item_fish, parent, false);
        return new FishViewHolder(itemView, onClickListenerInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull FishViewHolder holder, int position) {
        Fish fish = fishList.get(position);
        holder.bind(fish);
    }

    @Override
    public int getItemCount() {
        return fishList.size();
    }

}
