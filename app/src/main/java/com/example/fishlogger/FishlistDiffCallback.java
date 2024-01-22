package com.example.fishlogger;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

// This class was suggested by ChatGPT when I asked it to review my implementation of FishViewModel,
// FishViewHolder and FishAdapter. After implementing this I was able to get the default animation
// for deleting an item from the RecyclerView to work
public class FishlistDiffCallback extends DiffUtil.Callback {
    private List<Fish> oldFishlist;
    private List<Fish> newFishList;

    public FishlistDiffCallback(List<Fish> oldFishlist, List<Fish> newFishList){
        this.oldFishlist = oldFishlist;
        this.newFishList = newFishList;
    }
    @Override
    public int getOldListSize() {
        return oldFishlist.size();
    }

    @Override
    public int getNewListSize() {
        return newFishList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Fish oldFish = oldFishlist.get(oldItemPosition);
        Fish newFish = newFishList.get(newItemPosition);
        return oldFish.getFishId().equals(newFish.getFishId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Fish oldFish = oldFishlist.get(oldItemPosition);
        Fish newFish = newFishList.get(newItemPosition);
        return oldFish.equals(newFish);
    }
}
