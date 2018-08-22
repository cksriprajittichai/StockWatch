package c.chasesriprajittichai.stockwatch;

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

