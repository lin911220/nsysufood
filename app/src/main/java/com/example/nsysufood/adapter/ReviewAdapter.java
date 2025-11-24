package com.example.nsysufood.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nsysufood.R;
import com.example.nsysufood.model.Review;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<Review> reviewList;

    public ReviewAdapter(List<Review> list) {
        this.reviewList = list != null ? list : new ArrayList<>();
    }

    public void updateList(List<Review> newList) {
        this.reviewList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.tvName.setText(review.getUserName());
        holder.tvComment.setText(review.getComment());
        holder.ratingBar.setRating(review.getRating());

        // 轉換時間戳記為日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Taipei"));
        holder.tvDate.setText(sdf.format(new Date(review.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvComment, tvDate;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReviewName);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            ratingBar = itemView.findViewById(R.id.rbReviewRating);
        }
    }
}