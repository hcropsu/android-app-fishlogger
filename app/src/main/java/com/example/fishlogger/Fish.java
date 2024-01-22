package com.example.fishlogger;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.util.UUID;

// These are the Fish objects. Fish implements Parcelable and the related methods so that
// Fish objects can be passed from one activity to another.
public class Fish implements Parcelable {
    private String fishId;
    private String userId;
    private String latitude;
    private String longitude;
    private String date;
    private String species;
    private float length;
    private float weight;
    private boolean hasCustomImage = false;

    // The constructor only needs to set the 'fishId' property automatically. The rest of the
    // properties are set using setters when needed.
    public Fish(){
        this.fishId = UUID.randomUUID().toString();
    };

    protected Fish(Parcel in) {
        fishId = in.readString();
        userId = in.readString();
        latitude = in.readString();
        longitude = in.readString();
        date = in.readString();
        species = in.readString();
        length = in.readFloat();
        weight = in.readFloat();
        hasCustomImage = in.readBoolean();
    }

    public static final Creator<Fish> CREATOR = new Creator<Fish>() {
        @Override
        public Fish createFromParcel(Parcel in) {
            return new Fish(in);
        }

        @Override
        public Fish[] newArray(int size) {
            return new Fish[size];
        }
    };

    public String getFishId(){ return fishId; }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLatitude() { return latitude; }

    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }

    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public float getLength() {
        return length;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
    public boolean isHasCustomImage() {
        return hasCustomImage;
    }

    public void setHasCustomImage(boolean hasCustomImage) {
        this.hasCustomImage = hasCustomImage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(fishId);
        parcel.writeString(userId);
        parcel.writeString(latitude);
        parcel.writeString(longitude);
        parcel.writeString(date);
        parcel.writeString(species);
        parcel.writeFloat(length);
        parcel.writeFloat(weight);
        parcel.writeBoolean(hasCustomImage);
    }
}
