package c.chasesriprajittichai.stockwatch.recyclerviews;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import c.chasesriprajittichai.stockwatch.Article;
import c.chasesriprajittichai.stockwatch.R;


/**
 * The news {@link RecyclerView} contains two different types of {@link View}:
 * {@link DateViewHolder} and {@link ArticleViewHolder}. Many of the news
 * articles that will be displayed, were published on the same date. So, it can
 * be redundant to display a repetitive date for many adjacent articles.
 * Instead, DateViewHolder occupies a smaller cell than ArticleViewHolder, and
 * only displays the date. Below a DateViewHolder cell, one or more
 * ArticleViewHolders display the articles published on that day.
 */
public final class NewsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private final class DateViewHolder extends RecyclerView.ViewHolder {

        private final static int TYPE_ID = 1;
        private final TextView date;

        DateViewHolder(final View v) {
            super(v);

            date = v.findViewById(R.id.textView_date_newsRecyclerItem);
        }

        void bind(final String date) {
            this.date.setText(date);
        }

    }


    private final class ArticleViewHolder extends RecyclerView.ViewHolder {

        private final static int TYPE_ID = 2;
        private final TextView title;
        private final TextView source;

        ArticleViewHolder(final View v) {
            super(v);

            title = v.findViewById(R.id.textView_articleTitle_newsRecyclerItem);
            source = v.findViewById(R.id.textView_articleSource_newsRecyclerItem);
        }

        void bind(final Article article, final NewsRecyclerAdapter.OnItemClickListener listener) {
            title.setText(article.getTitle());
            source.setText(article.getSource());

            itemView.setOnClickListener(l -> listener.onItemClick(article));
        }

    }


    private final SparseArray<Article> articleSparseArray = new SparseArray<>();
    private final OnItemClickListener onItemClickListener;

    public NewsRecyclerAdapter(final OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public SparseArray<Article> getArticleSparseArray() {
        return articleSparseArray;
    }

    @Override
    public int getItemViewType(int position) {
        int type;
        if (articleSparseArray.get(position) != null) {
            type = ArticleViewHolder.TYPE_ID;
        } else {
            // If there is no mapping for this index (position)
            type = DateViewHolder.TYPE_ID;
        }

        return type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        final RecyclerView.ViewHolder holder;
        if (viewType == ArticleViewHolder.TYPE_ID) {
            holder = new ArticleViewHolder(
                    inflater.inflate(R.layout.recycler_item_article_news_recycler, parent, false));
        } else {
            holder = new DateViewHolder(
                    inflater.inflate(R.layout.recycler_item_date_news_recycler, parent, false));
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == ArticleViewHolder.TYPE_ID) {
            ((ArticleViewHolder) holder).bind(articleSparseArray.get(position), onItemClickListener);
        } else {
            /* articleSparseArray contains dates (no mapping), followed by the
             * Articles (mapping) that were published on that date. Once a new
             * date is found, that new date (no mapping) is "added" to the
             * sparse array, followed by the articles published on that date.
             * Dates in which no articles were published, are not "added".
             * Therefore, if there is a date (no mapping) at index n, there must
             * be a mapping at index n + 1, and the mapping at index n + 1 must
             * have the date that the non-mapping (date) at index n represents. */
            ((DateViewHolder) holder).bind(articleSparseArray.get(position + 1).getDate());
        }
    }

    @Override
    public int getItemCount() {
        // Return the highest key (highest mapped index) + 1
        // size() returns the number of mappings, skipping missing mappings
        return articleSparseArray.keyAt(articleSparseArray.size() - 1) + 1;
    }


    public interface OnItemClickListener {

        void onItemClick(final Article article);

    }
}
