package com.example.khushbu.myapplication;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class Chat extends RecyclerView.Adapter<Chat.PlanetViewHolder> {

    String[] devices;

    public Chat(String[] devices, Context context) {
        this.devices = devices;
    }

    @Override
    public Chat.PlanetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_chat,parent,false);
        PlanetViewHolder viewHolder=new PlanetViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(Chat.PlanetViewHolder holder, int position) {
        holder.text.setText(devices[position].toString());
    }

    @Override
    public int getItemCount() {
        return devices.length;
    }

    public static class PlanetViewHolder extends RecyclerView.ViewHolder{

        protected TextView text;

        public PlanetViewHolder(View itemView) {
            super(itemView);
            text= (TextView) itemView.findViewById(R.id.device);
        }
    }
}
