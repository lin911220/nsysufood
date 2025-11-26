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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private List<Review> reviewList;
    private OnReviewOptionClickListener optionClickListener;

    // 定義介面：當點擊「...」按鈕時通知 Activity
    public interface OnReviewOptionClickListener {
        void onOptionClick(View view, Review review);
    }

    public void setOnReviewOptionClickListener(OnReviewOptionClickListener listener) {
        this.optionClickListener = listener;
    }

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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Taipei"));
        holder.tvDate.setText(sdf.format(new Date(review.getTimestamp())));

        // --- 關鍵修改：檢查身分 ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // 如果有登入，且 ID 跟這則評論的作者 ID 一樣
        if (currentUser != null && currentUser.getUid().equals(review.getUserId())) {
            holder.btnMore.setVisibility(View.VISIBLE); // 顯示按鈕
            holder.btnMore.setOnClickListener(v -> {
                if (optionClickListener != null) {
                    optionClickListener.onOptionClick(v, review);
                }
            });
        } else {
            holder.btnMore.setVisibility(View.GONE); // 不是本人，隱藏按鈕
        }
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvComment, tvDate, btnMore; // 多了一個 btnMore
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvReviewName);
            tvComment = itemView.findViewById(R.id.tvReviewComment);
            tvDate = itemView.findViewById(R.id.tvReviewDate);
            ratingBar = itemView.findViewById(R.id.rbReviewRating);
            // 記得去 XML 加這個 id
            btnMore = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}