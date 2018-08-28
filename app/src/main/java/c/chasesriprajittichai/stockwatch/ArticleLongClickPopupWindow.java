package c.chasesriprajittichai.stockwatch;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;


/**
 * This class is instantiated whenever the user long-clicks an Article in
 * IndividualStockActivity.
 *
 * @see PopupWindow
 * @see IndividualStockActivity#initNewsRecyclerView()
 */
final class ArticleLongClickPopupWindow extends PopupWindow {

    private final Context context;
    private final View layout;
    private final Article article;

    ArticleLongClickPopupWindow(final Context context, final Article article) {
        super(context);
        this.context = context;
        this.article = article;

        final LayoutInflater layoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layout = layoutInflater.inflate(R.layout.popup_window_article_long_click, null);

        setContentView(layout);
        setElevation(10);
        setAnimationStyle(R.style.Animation);

        // Make the window close if the user touches outside the window
        setFocusable(true);
        setOutsideTouchable(true);

        initViewsInLayout();
    }

    /**
     * Initialize the URL TextView, and buttons for the following actions:
     * <ol>
     * <li>open link in default browser
     * <li>copy link
     * <li>share link
     * </ol>
     */
    private void initViewsInLayout() {
        final TextView url =
                layout.findViewById(R.id.textView_url_articleLongClickPopupWindow);
        url.setText(article.getUrl());

        final Button browserBtn =
                layout.findViewById(R.id.button_openInBroswer_articleLongClickPopupWindow);
        browserBtn.setOnClickListener(view -> {
            final Intent browserIntent =
                    new Intent(Intent.ACTION_VIEW, Uri.parse(article.getUrl()));
            context.startActivity(browserIntent);

            dismiss(); // Close the popup if button pressed
        });

        final Button copyLinkBtn =
                layout.findViewById(R.id.button_copyLink_articleLongClickPopupWindow);
        copyLinkBtn.setOnClickListener(view -> {
            final ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(
                        ClipData.newPlainText("URL", article.getUrl()));
            } else {
                Toast.makeText(context, "Failed to copy URL", Toast.LENGTH_SHORT).show();
            }

            dismiss(); // Close the popup if button pressed
        });

        final Button shareLinkBtn =
                layout.findViewById(R.id.button_shareLink_articleLongClickPopupWindow);
        shareLinkBtn.setOnClickListener(view -> {
            // Dismiss() before showing intent. It looks better
            dismiss(); // Close the popup if button pressed

            final Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, article.getUrl());
            context.startActivity(shareIntent);
        });
    }

}
