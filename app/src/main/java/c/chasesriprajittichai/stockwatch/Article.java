package c.chasesriprajittichai.stockwatch;

import android.support.v7.widget.RecyclerView;

import c.chasesriprajittichai.stockwatch.recyclerviews.NewsRecyclerAdapter;


/**
 * This class represents a news article for an AdvancedStock. A SparseArray of
 * these are filled in {@link IndividualStockActivity.DownloadNewsTask}, then
 * displayed in the news section of IndividualStockActivity.
 * <p>
 * This class' members are used to create both types of {@link
 * RecyclerView.ViewHolder} created in {@link NewsRecyclerAdapter}:
 * <li>{@link NewsRecyclerAdapter.ArticleViewHolder}</li>
 * <li>{@link NewsRecyclerAdapter.DateViewHolder}</li>
 */
public final class Article {

    private final String title;
    private final String source;
    private final String date;
    private final String url;

    public Article(final String date, final String title,
                   final String source, final String url) {
        this.date = date;
        this.title = title;
        this.source = source;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getDate() {
        return date;
    }

    public String getUrl() {
        return url;
    }

}

