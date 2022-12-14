package com.hisu.zola.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.hisu.zola.R;
import com.hisu.zola.database.entity.Media;
import com.hisu.zola.database.entity.Message;
import com.hisu.zola.databinding.LayoutImageGroupItemBinding;
import com.hisu.zola.listeners.IOnItemTouchListener;
import com.hisu.zola.util.dialog.ViewImageDialog;

import java.util.List;

public class ImageGroupAdapter extends RecyclerView.Adapter<ImageGroupAdapter.ImageGroupViewHolder> {

    private Message message;
    private List<Media> mediaList;
    private final Context context;
    private int mode;
    public static final int SEND_MODE = 1;
    public static final int RECEIVE_MODE = 0;
    private IOnItemTouchListener onItemTouchListener;

    public ImageGroupAdapter(Context context) {
        this.context = context;
    }

    public ImageGroupAdapter(Message message, Context context, int mode) {
        this.message = message;
        this.mediaList = message.getMedia();
        this.context = context;
        this.mode = mode;
        notifyDataSetChanged();
    }

    public void setMediaList(List<Media> mediaList) {
        this.mediaList = mediaList;
        notifyDataSetChanged();
    }

    public void setOnItemTouchListener(IOnItemTouchListener onItemTouchListener) {
        this.onItemTouchListener = onItemTouchListener;
    }

    @NonNull
    @Override
    public ImageGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ImageGroupViewHolder(
                LayoutImageGroupItemBinding.inflate(LayoutInflater.from(parent.getContext()),
                        parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ImageGroupViewHolder holder, int position) {
        Media media = mediaList.get(position);

        if (mode == SEND_MODE)
            holder.binding.itemParent.setGravity(Gravity.END);
        else if (mode == RECEIVE_MODE)
            holder.binding.itemParent.setGravity(Gravity.START);

        if (mediaList.size() < 2) {
            holder.binding.rimvImageGroupItem.setMaxWidth(getDp(320));
            holder.binding.rimvImageGroupItem.setMaxHeight(getDp(200));
        } else {
            holder.binding.rimvImageGroupItem.setMaxWidth(getDp(160));
            holder.binding.rimvImageGroupItem.setMaxHeight(getDp(200));
        }

        Glide.with(context).asBitmap().load(media.getUrl())
                .placeholder(AppCompatResources.getDrawable(context, R.drawable.ic_img_place_holder))
                .diskCacheStrategy(DiskCacheStrategy.ALL).into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        holder.binding.rimvImageGroupItem.setImageBitmap(resource);
                        holder.binding.rimvImageGroupItem.setVisibility(View.VISIBLE);
                    }
                });

        holder.binding.rimvImageGroupItem.setOnLongClickListener(view -> {
            onItemTouchListener.longPress(message, holder.binding.rimvImageGroupItem);
            return true;
        });

        holder.binding.rimvImageGroupItem.setOnClickListener(view -> {
            ViewImageDialog dialog = new ViewImageDialog(context, media.getUrl(), Gravity.CENTER);
            dialog.showDialog();
        });
    }

    public int getDp(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public int getItemCount() {
        return mediaList != null ? mediaList.size() : 0;
    }

    public static class ImageGroupViewHolder extends RecyclerView.ViewHolder {

        private final LayoutImageGroupItemBinding binding;

        public ImageGroupViewHolder(@NonNull LayoutImageGroupItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}