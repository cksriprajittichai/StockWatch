package c.chasesriprajittichai.stockwatch.recyclerviews;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import c.chasesriprajittichai.stockwatch.Article;
import c.chasesriprajittichai.stockwatch.IndividualStockActivity;
import c.chasesriprajittichai.stockwatch.R;


public final class NewsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    /**
     * A {@link RecyclerView.ViewHolder} that will represent the date of
     * one or more {@link Article}.
     * <p>
     * The {@link IndividualStockActivity#newsRv} contains two different types
     * of View: {@link DateViewHolder} and {@link ArticleViewHolder}. Many of
     * the news articles that will be displayed, were published on the same
     * date. So, it can be redundant to display a repetitive date for many
     * adjacent articles. Instead, DateViewHolder occupies a cell with a
     * different layout than ArticleViewHolder, and only displays the date.
     * Below a DateViewHolder cell, one or more ArticleViewHolders display the
     * articles published on that day.
     */
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


    /**
     * A {@link RecyclerView.ViewHolder} that will represent a {@link Article}.
     */
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

    /**
     * @param position The position of the item within the adapter's data set
     * @return {@link ArticleViewHolder#TYPE_ID} if articleSparseArray has a
     * mapping for this position. Otherwise, return {@link
     * DateViewHolder#TYPE_ID}.
     */
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

    /**
     * Called when RecyclerView needs a new {@link RecyclerView.ViewHolder} of
     * the given type to represent an item.
     * <p>
     * This new StockViewHolder should be constructed with a new View that can
     * represent the items of the given type. You can either create a new View
     * manually or inflate it from an XML layout file.
     * <p>
     * The new StockViewHolder will be used to display items of the adapter using
     * {@link #onBindViewHolder(RecyclerView.ViewHolder, int, List)}.
     *
     * @param parent   The ViewGroup into which the new View will be added after
     *                 it is bound to an adapter position
     * @param viewType The view type of the new View
     * @return A new StockViewHolder that holds a View of the given view type
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
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

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method should update the contents of the {@link
     * RecyclerView.ViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder   The StockViewHolder which should be updated to represent the
     *                 contents of the item at the given position in the data set
     * @param position The position of the item within the adapter's data set
     */
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

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter
     */
    @Override
    public int getItemCount() {
        // Return the highest key (highest mapped index) + 1
        // size() returns the number of mappings, skipping missing mappings
        return articleSparseArray.keyAt(articleSparseArray.size() - 1) + 1;
    }

    /**
     * This method is used in {@link IndividualStockActivity} to pass {@link
     * #articleSparseArray} to the {@link
     * IndividualStockActivity.DownloadNewsTask}.
     *
     * @return articleSparseArray
     */
    public SparseArray<Article> getArticleSparseArray() {
        return articleSparseArray;
    }


    public interface OnItemClickListener {

        void onItemClick(final Article article);

    }
}
