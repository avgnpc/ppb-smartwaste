package com.smartwaste.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.smartwaste.app.R;
import com.smartwaste.app.model.Capture;

import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class CaptureAdapter extends RecyclerView.Adapter<CaptureAdapter.CaptureViewHolder> {

    private final List<Capture> captureList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Capture capture);
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void submitList(List<Capture> newList) {
        captureList.clear();
        captureList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CaptureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout  .item_capture, parent, false);
        return new CaptureViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CaptureViewHolder holder, int position) {
        Capture capture = captureList.get(position);
        holder.bind(capture);
    }

    @Override
    public int getItemCount() {
        return captureList.size();
    }

    class CaptureViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTimestamp, tvDibersihkan;
        private final ImageView ivThumbnail;

        public CaptureViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvDibersihkan = itemView.findViewById(R.id.tvDibersihkan);
            ivThumbnail = itemView.findViewById(R.id.ivCaptureImage);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(captureList.get(pos));
                }
            });
        }

        public void bind(Capture capture) {
            String rawTimestamp = capture.getTimestamp();
            String formatted = rawTimestamp; // fallback default
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id", "ID"));
                formatted = outputFormat.format(inputFormat.parse(rawTimestamp));
            } catch (Exception e) {
                e.printStackTrace(); // debug kalau ada masalah parsing
            }
            tvTimestamp.setText(formatted);

            boolean sudah = capture.isDibersihkan();
            tvDibersihkan.setText(sudah ? "Dibersihkan" : "Belum Dibersihkan");
            tvDibersihkan.setBackgroundResource(sudah ? R.drawable.status_bg_green : R.drawable.status_bg_red);
            tvDibersihkan.setTextColor(0xFFFFFFFF); // putih

            Glide.with(itemView.getContext())
                    .load(capture.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(ivThumbnail);
        }

    }
}
