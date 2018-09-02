package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.wefika.horizontalpicker.HorizontalPicker;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.ChronoUnit;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import c.chasesriprajittichai.stockwatch.listeners.DownloadChartsTaskListener;
import c.chasesriprajittichai.stockwatch.listeners.DownloadNewsTaskListener;
import c.chasesriprajittichai.stockwatch.listeners.DownloadStatsTaskListener;
import c.chasesriprajittichai.stockwatch.recyclerviews.NewsRecyclerAdapter;
import c.chasesriprajittichai.stockwatch.recyclerviews.NewsRecyclerDivider;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock.ChartPeriod;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteAdvancedStock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteAdvancedStockWithAhVals;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteStockWithAhVals;
import c.chasesriprajittichai.stockwatch.stocks.Stock;
import c.chasesriprajittichai.stockwatch.stocks.StockInHomeActivity;
import c.chasesriprajittichai.stockwatch.stocks.StockWithAhVals;

import static c.chasesriprajittichai.stockwatch.stocks.AdvancedStock.Stat;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.ERROR;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.PREMARKET;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public final class IndividualStockActivity
        extends AppCompatActivity
        implements HorizontalPicker.OnItemSelected,
        CustomScrubGestureDetector.ScrubIndexListener,
        DownloadChartsTaskListener,
        DownloadStatsTaskListener,
        DownloadNewsTaskListener {

    @BindView(R.id.viewFlipper_overviewOrNewsFlipper) ViewFlipper viewFlipper;
    @BindView(R.id.button_overview) Button overviewBtn;
    @BindView(R.id.button_news) Button newsBtn;

    @BindView(R.id.textView_topPrice_individual) TextView top_price;
    @BindView(R.id.textView_topChangePoint_individual) TextView top_changePoint;
    @BindView(R.id.textView_topChangePercent_individual) TextView top_changePercent;
    @BindView(R.id.textView_topTime_individual) TextView top_time;
    @BindView(R.id.linearLayout_topAfterHoursData_individual) LinearLayout ah_linearLayout;
    @BindView(R.id.textView_topAfterHoursChangePoint_individual) TextView top_ah_changePoint;
    @BindView(R.id.textView_topAfterHoursChangePercent_individual) TextView top_ah_changePercent;
    @BindView(R.id.textView_topAfterHoursTime_individual) TextView top_ah_time;
    @BindView(R.id.progressBar_loadingCharts) ProgressBar loadingChartsProgressBar;
    @BindView(R.id.textView_chartsStatus) TextView chartsStatus;
    @BindView(R.id.sparkView) CustomSparkView sparkView;
    @BindView(R.id.horizontalPicker_chartPeriod) HorizontalPicker chartPeriodPicker;
    @BindView(R.id.view_chartPeriodPickerUnderline) View chartPeriodPickerUnderline;
    @BindView(R.id.textView_keyStatisticsHeader) TextView keyStatisticsHeader;
    @BindView(R.id.textSwitcher_prevClose) TextSwitcher prevClose;
    @BindView(R.id.textSwitcher_open) TextSwitcher open;
    @BindView(R.id.textSwitcher_volume) TextSwitcher volume;
    @BindView(R.id.textSwitcher_averageVolume) TextSwitcher avgVolume;
    @BindView(R.id.textSwitcher_todaysLow) TextSwitcher todaysLow;
    @BindView(R.id.textSwitcher_todaysHigh) TextSwitcher todaysHigh;
    @BindView(R.id.textSwitcher_fiftyTwoWeekLow) TextSwitcher fiftyTwoWeekLow;
    @BindView(R.id.textSwitcher_fiftyTwoWeekHigh) TextSwitcher fiftyTwoWeekHigh;
    @BindView(R.id.textSwitcher_marketCap) TextSwitcher marketCap;
    @BindView(R.id.textSwitcher_peRatio) TextSwitcher peRatio;
    @BindView(R.id.textSwitcher_eps) TextSwitcher eps;
    @BindView(R.id.textSwitcher_yield) TextSwitcher yield;
    @BindView(R.id.textSwitcher_description) TextSwitcher description;

    @BindView(R.id.recyclerView_newsRecycler) RecyclerView newsRv;
    @BindView(R.id.progressBar_loadingNews) ProgressBar loadingNewsProgressBar;
    @BindView(R.id.textView_newsStatus) TextView newsStatus;

    /**
     * A container of the big ChartPeriods (all ChartPeriods excluding {@link
     * ChartPeriod#ONE_DAY}). The big ChartPeriods are often treated similarly,
     * and unlike ONE_DAY.
     */
    private static final ChartPeriod[] BIG_CHART_PERIODS = {
            ChartPeriod.FIVE_YEARS, ChartPeriod.ONE_YEAR,
            ChartPeriod.THREE_MONTHS, ChartPeriod.ONE_MONTH,
            ChartPeriod.TWO_WEEKS
    };

    /**
     * Maps every {@link Stat} to the {@link TextSwitcher} that displays its
     * value.
     * <p>
     * This cannot be initialized here because the TextSwitchers that will be in
     * the map are not initialized yet.
     */
    private final Map<Stat, TextSwitcher> statToViewMap = new HashMap<>();

    private AdvancedStock stock;
    private boolean wasStarredInitially;
    private boolean isStarred;
    private boolean isInPrefs;
    private SparkViewAdapter sparkViewAdapter;
    private NewsRecyclerAdapter newsRecyclerAdapter;
    private SharedPreferences prefs;
    private Timer timer;

    /**
     * This is set to true once a {@link DownloadChartsTask} completes with a
     * status code of {@link DownloadChartsTask.Status#GOOD}. This variable's
     * value is checked and possibly set to true in {@link
     * #onDownloadChartsTaskCompleted(int, Set)}. This is used to determine the
     * state of the Views displaying charts. More specifically, this is a flag
     * for whether or not a Download has completed with a {@link
     * DownloadChartsTask.Status} equal to GOOD.
     *
     * @see #onDownloadChartsTaskCompleted(int, Set)
     */
    private boolean showsRealValues_charts = false;

    /**
     * This is set to true once a {@link DownloadStatsTask} completes with a
     * status code of {@link DownloadStatsTask.Status#GOOD}. This variable's
     * value is checked and possibly set to true in {@link
     * #onDownloadStatsTaskCompleted(int, Set)}. This is used to determine the
     * state of the Views displaying Stats. More specifically, this is a flag
     * for whether or not a DownloadStatsTask has completed with a {@link
     * DownloadStatsTask.Status} equal to GOOD.
     *
     * @see #updateDisplayStat(Stat)
     * @see #onDownloadStatsTaskCompleted(int, Set)
     */
    private boolean showsRealValues_stats = false;

    /**
     * This is the number of consecutive times that a specific AsyncTask can
     * fail (IOException) and be restarted (new instance created and executed).
     * If a specific AsyncTask consecutively fails more times than this value,
     * the AsyncTask will not be restarted.
     *
     * @see #onDownloadStatsTaskCompleted(int, Set)
     */
    private final int NUM_CONSEC_TASK_FAILS_ALLOWED = 3; // Per each task

    /**
     * Counter for the number of consecutive {@link DownloadChartsTask} that
     * complete with a {@link DownloadChartsTask.Status} not equal to {@link
     * DownloadChartsTask.Status#GOOD}.
     *
     * @see #onDownloadChartsTaskCompleted(int, Set)
     */
    private int consecFails_chartTask = 0;

    /**
     * Counter for the number of consecutive {@link DownloadStatsTask} that
     * complete with a {@link DownloadStatsTask.Status} not equal to {@link
     * DownloadStatsTask.Status#GOOD}.
     *
     * @see #onDownloadStatsTaskCompleted(int, Set)
     */
    private int consecFails_statsTask = 0;

    /**
     * Counter for the number of consecutive {@link DownloadNewsTask} that
     * complete with a {@link DownloadNewsTask.Status} not equal to {@link
     * DownloadNewsTask.Status#GOOD}.
     *
     * @see #onDownloadNewsTaskCompleted(int)
     */
    private int consecFails_newsTask = 0;

    /**
     * Called from {@link DownloadChartsTask#onPostExecute(Integer)}.
     * <p>
     * The following {@link DownloadChartsTask.Status} represent at least one
     * {@link IOException} being thrown in the DownloadChartsTask:
     * <ul>
     * <li>{@link DownloadChartsTask.Status#IO_EXCEPTION_FOR_WSJ_ONLY}
     * <li>{@link DownloadChartsTask.Status#IO_EXCEPTION_FOR_MW_AND_WSJ}
     * </ul>
     * If status represents at least one IOException being thrown in the
     * DownloadChartsTask and {@link #NUM_CONSEC_TASK_FAILS_ALLOWED} has been
     * exceeded by this task's fail count, this method shows a no-connection
     * message where {@link #sparkView} would be if there were filled charts.
     * If {@link #consecFails_chartTask} does not exceed
     * NUM_CONSEC_TASK_FAILS_ALLOWED, a new instance of DownloadChartsTask is
     * restarted (created and executed).
     * <p>
     * If status equals {@link DownloadChartsTask.Status#GOOD}, this method
     * uses missingChartPeriods to determine which charts are filled. It then
     * displays the names of the filled ChartPeriod(s) in {@link
     * #chartPeriodPicker}, sets up {@link #sparkViewAdapter} with the initially
     * selected chart, updates {@link #sparkView}, then shows all the Views
     * related to the charts. In the extremely rare case that status equals
     * GOOD, and no charts are filled, this method does the same thing that it
     * would do if status represented an thrown IOException in
     * DownloadChartsTask, except in this case, a no-charts message is shown,
     * rather than a no-connection message.
     * <p>
     * In the first call to this function where status equals GOOD, {@link
     * #showsRealValues_charts} is set to true.
     * <p>
     * Once real values have been shown (showsRealValues_charts == true) or
     * consecFails_chartTask exceeds NUM_CONSEC_TASK_FAILS_ALLOWED, regardless
     * of the value of status, {@link #loadingChartsProgressBar}'s visibility is
     * set to {@link View#GONE}, and a response (charts or message) to the
     * DownloadChartsTask is shown.
     *
     * @param status              The {@link DownloadChartsTask.Status} of the
     *                            task
     * @param missingChartPeriods The set of missing {@link ChartPeriod}
     * @see #consecFails_chartTask
     * @see #showsRealValues_charts
     */
    @Override
    public synchronized void onDownloadChartsTaskCompleted(final int status,
                                                           final Set<ChartPeriod> missingChartPeriods) {
        switch (status) {
            case DownloadChartsTask.Status.GOOD:
                consecFails_chartTask = 0;

                if (showsRealValues_charts) {
                    final ChartPeriod selectedPeriod = sparkViewAdapter.getChartPeriod();
                    boolean needToUpdate = false;
                    if (selectedPeriod == ChartPeriod.ONE_DAY) {
                        /* The 1D chart adds a data point every 5 minutes (while
                         * OPEN). Check if a new data point has been added to
                         * the prices list that is currently displayed. The 1D
                         * chart does not have dates, so it must be treated
                         * differently than the big ChartPeriods. */
                        if (sparkViewAdapter.getPrices().size() !=
                                stock.getPrices(selectedPeriod).size()) {
                            needToUpdate = true;
                        }
                    } else if (selectedPeriod != null) {
                        /* sparkViewAdapter's ChartPeriod is null if stock does
                         * not have charts (task executed with status == GOOD,
                         * but WSJ and MarketWatch did not have a chart for
                         * stock). This is very rare. If sparkViewAdapter's
                         * ChartPeriod is null, do nothing. */

                        /* Check if the most recent date in the dates list that
                         * is being displayed is the same as the most recent
                         * date in stock's dates list for the selected
                         * ChartPeriod. Note that the charts for the big
                         * ChartPeriods will rarely need to be updated. Their
                         * data changes only once per trading day. */
                        final String displayDate =
                                sparkViewAdapter.getDate(
                                        sparkViewAdapter.getDates().size() - 1);
                        final String updatedDate =
                                stock.getDates(selectedPeriod).get(
                                        stock.getDates(selectedPeriod).size() - 1);
                        if (!displayDate.equals(updatedDate)) {
                            needToUpdate = true;
                        }
                    }

                    if (needToUpdate) {
                        sparkViewAdapter.setPrices(stock.getPrices(selectedPeriod));
                        sparkViewAdapter.setDates(stock.getDates(selectedPeriod));
                        sparkViewAdapter.notifyDataSetChanged();
                    }
                } else {
                    /* This else block is unaffected by sparkViewAdapter's
                     * ChartPeriod being null. */

                    showsRealValues_charts = true;


                    // ChartPeriods from string-array resource are in increasing order (1D -> 5Y)
                    final List<CharSequence> displayChartPeriods = Arrays.stream(
                            getResources().getStringArray(R.array.chartPeriods))
                            .collect(Collectors.toList());

                    if (!missingChartPeriods.contains(ChartPeriod.ONE_DAY)) {
                        sparkViewAdapter.setChartPeriod(ChartPeriod.ONE_DAY);
                        sparkViewAdapter.setPrices(stock.getPrices_1day());
                        // Don't set dates for sparkViewAdapter for 1D
                        sparkViewAdapter.notifyDataSetChanged();
                    } else {
                        displayChartPeriods.remove(0); // 1D is at index 0 of chartPeriods
                    }

                    for (final ChartPeriod p : BIG_CHART_PERIODS) {
                        if (missingChartPeriods.contains(p)) {
                            /* Always remove last node because
                             * displayChartPeriods is in increasing order
                             * (1D -> 5Y), unlike BIG_CHART_PERIODS which is in
                             * decreasing order. */
                            displayChartPeriods.remove(displayChartPeriods.size() - 1);
                        } else {
                            if (missingChartPeriods.contains(ChartPeriod.ONE_DAY)) {
                                /* If the 1D chart is filled, the current
                                 * ChartPeriod will be set to ONE_DAY.
                                 * Otherwise, if the 1D chart is not filled, the
                                 * initially selected ChartPeriod should be the
                                 * smallest big ChartPeriod (TWO_WEEKS). By the
                                 * way that we're filling the charts, if at
                                 * least one big ChartPeriod is filled, that
                                 * guarantees that the 2W chart is filled. */
                                sparkViewAdapter.setChartPeriod(ChartPeriod.TWO_WEEKS);
                                sparkViewAdapter.setPrices(stock.getPrices_2weeks());
                                sparkViewAdapter.setDates(stock.getDates_2weeks());
                                sparkViewAdapter.notifyDataSetChanged();
                            }

                            /* Smaller charts (shorter ChartPeriods) take their
                             * data from the largest chart that is filled. So
                             * once a filled chart is found, there is no need to
                             * check if the smaller charts are filled. */
                            break;
                        }
                    }

                    if (missingChartPeriods.size() == ChartPeriod.values().length) {
                        // If there are no filled charts
                        chartsStatus.setText(getString(R.string.chartsUnavailable));
                        chartsStatus.setVisibility(View.VISIBLE);
                    } else {
                        // If there is at least 1 filled chart

                        final CharSequence[] displayChartPeriodsArr =
                                new CharSequence[displayChartPeriods.size()];
                        displayChartPeriods.toArray(displayChartPeriodsArr);
                        chartPeriodPicker.setValues(displayChartPeriodsArr);

                        sparkView.setVisibility(View.VISIBLE);
                        chartPeriodPicker.setVisibility(View.VISIBLE);
                        chartPeriodPickerUnderline.setVisibility(View.VISIBLE);
                    }

                    loadingChartsProgressBar.setVisibility(View.GONE);
                }
                break;
            case DownloadChartsTask.Status.IO_EXCEPTION_FOR_MW_AND_WSJ:
            case DownloadChartsTask.Status.IO_EXCEPTION_FOR_MW_ONLY:
            case DownloadChartsTask.Status.IO_EXCEPTION_FOR_WSJ_ONLY:
                consecFails_chartTask++;

                if (consecFails_chartTask > NUM_CONSEC_TASK_FAILS_ALLOWED) {
                    chartsStatus.setText(getString(R.string.ioException_loadingCharts));
                    chartsStatus.setVisibility(View.VISIBLE);

                    loadingChartsProgressBar.setVisibility(View.GONE);
                } else {
                    new DownloadChartsTask(stock, this).execute();
                }
                break;
        }
    }

    /**
     * Called from {@link DownloadStatsTask#onPostExecute(Integer)}.
     * <p>
     * If status equals {@link DownloadStatsTask.Status#IO_EXCEPTION} and {@link
     * #NUM_CONSEC_TASK_FAILS_ALLOWED} has been exceeded by {@link
     * #consecFails_statsTask} this method sets each display in the "Key
     * Statistics" section to show a {@literal x}, and makes the description
     * display show a no-connection message. If status equals IO_EXCEPTION and
     * consecFails_statsTask does not exceed NUM_CONSEC_TASK_FAILS_ALLOWED, a
     * new instance of this task is created and executed. If status equals
     * IO_EXCEPTION, the "top views" are not changed by this method.
     * <p>
     * If status equals {@link DownloadStatsTask.Status#GOOD}, the "Key
     * Statistics" and description displays display the values of {@link #stock}
     * that were updated in DownloadStatsTask. The top views are updated as well
     * to show the values of stock. If a Stat is contained in missingStats, its
     * display text is set to {@literal N/A}. Displays are only updated (setText
     * method) if necessary.
     * <p>
     * In the first call to this function where status equals GOOD, {@link
     * #showsRealValues_stats} is set to true.
     *
     * @param status       The {@link DownloadStatsTask.Status} of the task
     * @param missingStats The set of missing {@link Stat}
     * @see #consecFails_statsTask
     * @see #showsRealValues_stats
     * @see #updateDisplayStat(Stat)
     */
    @Override
    public synchronized void onDownloadStatsTaskCompleted(final int status,
                                                          final Set<Stat> missingStats) {
        switch (status) {
            case DownloadStatsTask.Status.GOOD:
                consecFails_statsTask = 0;

                initTopViews(); // Update top views

                for (final Stat s : Stat.values()) {
                    if (!missingStats.contains(s)) {
                        updateDisplayStat(s);
                    } else if (!showsRealValues_stats) {
                        if (s != Stat.DESCRIPTION) {
                            /* If a stat (excluding DESCRIPTION) is "missing",
                             * "N/A" should be displayed as its value. If this
                             * is the first time real values are being shown,
                             * animate the text change. By using the condition
                             * that showsRealValues_stats is false (this is the
                             * first time real values are being shown), we are
                             * assuming that values that are not "missing" the
                             * first time real values are shown, will not be
                             * "missing" after the first time real values are
                             * shown (updated). */
                            statToViewMap.get(s).setText(getString(R.string.na));
                        } else {
                            /* Description is the only Stat that shouldn't
                             * display "N/A" if missing. */
                            description.setText(getString(R.string.descriptionNotFound));
                        }
                    }
                }

                /* Real values are first shown in updateDisplayStat. Don't
                 * change this value before updateDisplayStat is called. */
                showsRealValues_stats = true;
                break;
            case DownloadStatsTask.Status.IO_EXCEPTION:
                consecFails_statsTask++;

                /* Don't change any of the top views. They show the values
                 * that were passed on from HomeActivity. */

                if (consecFails_statsTask > NUM_CONSEC_TASK_FAILS_ALLOWED) {
                    todaysLow.setText(getString(R.string.x));
                    todaysHigh.setText(getString(R.string.x));
                    fiftyTwoWeekLow.setText(getString(R.string.x));
                    fiftyTwoWeekHigh.setText(getString(R.string.x));
                    marketCap.setText(getString(R.string.x));
                    prevClose.setText(getString(R.string.x));
                    peRatio.setText(getString(R.string.x));
                    eps.setText(getString(R.string.x));
                    yield.setText(getString(R.string.x));
                    volume.setText(getString(R.string.x));
                    open.setText(getString(R.string.x));
                    avgVolume.setText(getString(R.string.x));

                    description.setText(getString(R.string.ioException_loadingDescription));
                } else {
                    new DownloadStatsTask(stock, this).execute();
                }
                break;
        }
    }

    /**
     * Called from {@link DownloadNewsTask#onPostExecute(Integer)}.
     * <p>
     * {@link DownloadNewsTask} updates {@link #newsRecyclerAdapter} within
     * {@link DownloadNewsTask#doInBackground(Void...)}, so this method does not
     * need to do much analysis on the completed DownloadNewsTask. The {@link
     * DownloadNewsTask.Status} codes are very clear, and determine what this
     * method does. If status equals {@link
     * DownloadNewsTask.Status#IO_EXCEPTION}, and {@link #consecFails_newsTask}
     * does not exceed {@link #NUM_CONSEC_TASK_FAILS_ALLOWED}, a
     * DownloadNewsTask is restarted (created and executed). If status equals
     * IOException and consecFails_newsTask does exceed
     * NUM_CONSEC_TASK_FAILS_ALLOWED, a no connection message is shown. If
     * status equals {@link DownloadNewsTask.Status#NO_NEWS_ARTICLES}, a
     * corresponding message is shown. If status equals {@link
     * DownloadNewsTask.Status#GOOD}, {@link #newsRv} is shown displaying the
     * Articles that {@link #newsRecyclerAdapter} has.
     * <p>
     * Once DownloadNewsTasks are no longer being created, regardless of the
     * value of status, this method sets {@link #loadingNewsProgressBar}'s
     * visibility to {@link View#GONE}, and a response (newsRv or message) to
     * the DownloadNewsTask is shown.
     *
     * @param status The {@link DownloadNewsTask.Status} of the task
     * @see #consecFails_newsTask
     */
    @Override
    public synchronized void onDownloadNewsTaskCompleted(final int status) {
        switch (status) {
            case DownloadNewsTask.Status.GOOD:
                loadingNewsProgressBar.setVisibility(View.GONE);
                newsRv.setVisibility(View.VISIBLE);

                newsRecyclerAdapter.notifyDataSetChanged();
                break;
            case DownloadNewsTask.Status.NO_NEWS_ARTICLES:
                loadingNewsProgressBar.setVisibility(View.GONE);
                newsRv.setVisibility(View.GONE);

                newsStatus.setText(getString(R.string.noNewsArticlesFound));
                break;
            case DownloadNewsTask.Status.IO_EXCEPTION:
                consecFails_newsTask++;

                if (consecFails_newsTask > NUM_CONSEC_TASK_FAILS_ALLOWED) {
                    loadingNewsProgressBar.setVisibility(View.GONE);
                    newsRv.setVisibility(View.GONE);

                    newsStatus.setText(getString(R.string.ioException_loadingNews));
                } else {
                    new DownloadNewsTask(stock.getTicker(), newsRecyclerAdapter.getArticleSparseArray(), this)
                            .execute();
                }
                break;
        }
    }

    /**
     * Initialize the top TextViews that are changed during scrubbing. This
     * includes the live price (large), live change point, live change percent,
     * change point at close, change percent at close, and the two views that
     * show values related to time. Values are taken from {@link #stock}.
     * <p>
     * This method is used in {@link #onCreate(Bundle)} to initialize the top
     * TextViews to show the top values that have been passed from HomeActivity.
     * This method should also be called any time that the selected chart
     * changes, so that the top views can update to display the values for the
     * selected ChartPeriod.
     */
    private void initTopViews() {
        top_price.setText(getString(R.string.double2dec, stock.getLivePrice()));

        /* sparkViewAdapter's ChartPeriod is null if there are no charts, or if
         * charts have not been initialized yet. */
        setScrubTime(sparkViewAdapter.getChartPeriod() != null ?
                sparkViewAdapter.getChartPeriod() :
                ChartPeriod.ONE_DAY);

        final double changePoint;
        final double changePercent;

        if (sparkViewAdapter.getChartPeriod() == ChartPeriod.ONE_DAY ||
                sparkViewAdapter.getChartPeriod() == null) {
            /* sparkViewAdapter's ChartPeriod is null if there are no charts, or
             * if charts have not been initialized yet. */

            changePoint = stock.getChangePoint();
            changePercent = stock.getChangePercent();

            /* Init TextViews for after hours data in ah_linearLayout and init
             * their visibility. */
            if (stock instanceof StockWithAhVals) {
                top_ah_time.setText(getString(R.string.afterHours));

                if (stock.getLiveChangePoint() < 0) {
                    // '-' is already part of the number
                    top_ah_changePoint.setText(getString(
                            R.string.double2dec,
                            stock.getLiveChangePoint()));
                    top_ah_changePercent.setText(getString(
                            R.string.openParen_double2dec_percent_closeParen,
                            stock.getLiveChangePercent()));
                } else {
                    top_ah_changePoint.setText(getString(
                            R.string.plus_double2dec,
                            stock.getLiveChangePoint()));
                    top_ah_changePercent.setText(getString(
                            R.string.openParen_plus_double2dec_percent_closeParen,
                            stock.getLiveChangePercent()));
                }
                ah_linearLayout.setVisibility(View.VISIBLE);
            } else {
                ah_linearLayout.setVisibility(View.INVISIBLE);
            }
        } else {
            ah_linearLayout.setVisibility(View.INVISIBLE);

            final double firstPriceOfPeriod = sparkViewAdapter.getPrice(0);

            /* If stock instanceof StockWithAhVals, change values are
             * comparisons between live values and the first price of the
             * current ChartPeriod. */
            changePoint = stock.getLivePrice() - firstPriceOfPeriod;
            changePercent = (changePoint / firstPriceOfPeriod) * 100;
        }

        if (changePoint < 0) {
            // '-' is already part of the number
            top_changePoint.setText(getString(R.string.double2dec, changePoint));
            top_changePercent.setText(getString
                    (R.string.openParen_double2dec_percent_closeParen,
                            changePercent));
        } else {
            top_changePoint.setText(getString(R.string.plus_double2dec, changePoint));
            top_changePercent.setText(getString(
                    R.string.openParen_plus_double2dec_percent_closeParen,
                    changePercent));
        }
    }

    /**
     * Called from {@link CustomScrubGestureDetector#onTouch(View, MotionEvent)}.
     * <p>
     * This method changes the top Views depending on which {@link ChartPeriod}
     * is selected, and the index of the scrubbing line.
     * <p>
     * If the selected ChartPeriod is {@link ChartPeriod#ONE_DAY}, this function
     * calculates the time of day that corresponds to the scrubbing index, and
     * displays it in {@link #top_time}. If the top time value is 4:00pm or
     * later, {@link #ah_linearLayout} is set to {@link View#VISIBLE}, and the
     * top after hours Views display {@link #stock}'s after hours values.
     * Additionally, if the scrubbing index represents a time value of 4:00pm or
     * later, the shown change values reset to be relative to stock's price at
     * close. This differs from if the scrubbing index does not represent an
     * after hours time, and the change values are relative to the stock's price
     * at open. This functionality is unique for when the selected ChartPeriod
     * is ONE_DAY. This is the only ChartPeriod where ah_linearLayout is used
     * and VISIBLE, and the only ChartPeriod where the change values can be
     * relative to different prices depending on the scrubbing index.
     * <p>
     * If the selected ChartPeriod is not ONE_DAY, this method sets top_time to
     * show the date at this scrubbing index of the selected ChartPeriod. Unlike
     * the time of day if the selected ChartPeriod were ONE_DAY, the date is not
     * calculated of the scrubbing index of the selected ChartPeriod is not
     * calculated in this function. The date at this scrubbing index is gotten
     * from {@link SparkViewAdapter#getDate(int)}. For ChartPeriods not equal to
     * ONE_DAY, the change values are always relative to the first price of the
     * list of prices of the selected ChartPeriod.
     *
     * @param index The scrubbing index; index of the scrubbing line
     */
    @Override
    public void onScrubbed(final int index) {
        // Get scrubbing price from the chart data for the selected ChartPeriod
        final double scrubPrice = sparkViewAdapter.getPrice(index);
        top_price.setText(getString(R.string.double2dec, scrubPrice));

        final double firstPriceOfSection;
        final double changePoint;
        final double changePercent;

        if (sparkViewAdapter.getChartPeriod() == ChartPeriod.ONE_DAY) {
            /* After hours begins as soon as the open market closes. Therefore,
             * the after hours section of the chart should begin at 4:00pm,
             * which is at index 78. If the user is scrubbing in the after hours
             * range of the chart, the change values should change so that they
             * are relative to the price at close. */
            if (index >= 78) {
                /* Because stock instanceof StockWithAhVals,
                 * stock.getPrice() returns the price at close. */
                firstPriceOfSection = stock.getPrice();

                // "Hide" after hours change data, only show "After-Hours"
                top_ah_changePoint.setText("");
                top_ah_changePercent.setText("");
                top_ah_time.setText(getString(R.string.afterHours));
                ah_linearLayout.setVisibility(View.VISIBLE);
            } else {
                firstPriceOfSection = sparkViewAdapter.getPrice(0);

                ah_linearLayout.setVisibility(View.INVISIBLE);
            }

            /* There are 78 data points representing the open hours data
             * (9:30am - 4:00pm ET). This means that 78 data points represent
             * 6.5 hours. Therefore, there is one data point for every 5
             * minutes. This 5 minute step size is constant throughout all
             * states. The number of data points is dependent on the time of
             * day. */
            int minute = 30 + (index * 5);
            int hour = 9;
            if (minute >= 60) {
                hour += minute / 60;
                minute %= 60;
            }
            final boolean isPm = hour >= 12;
            if (hour > 12) {
                hour %= 12;
            }
            if (isPm) {
                top_time.setText(getString(R.string.int_colon_int2dig_pm_ET, hour, minute));
            } else {
                top_time.setText(getString(R.string.int_colon_int2dig_am_ET, hour, minute));
            }
        } else {
            firstPriceOfSection = sparkViewAdapter.getPrice(0);

            // Get scrubbing date from the chart data for the selected ChartPeriod
            top_time.setText(getString(R.string.string, sparkViewAdapter.getDate(index)));
        }

        changePoint = scrubPrice - firstPriceOfSection;
        changePercent = (changePoint / firstPriceOfSection) * 100;

        if (changePoint < 0) {
            // '-' is already part of the number
            top_changePoint.setText(getString(R.string.double2dec, changePoint));
            top_changePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, changePercent));
        } else {
            top_changePoint.setText(getString(R.string.plus_double2dec, changePoint));
            top_changePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, changePercent));
        }
    }

    /**
     * Called from {@link CustomScrubGestureDetector#onTouch(View, MotionEvent)}.
     * <p>
     * When scrubbing, the top Views are changed. Once the scrubbing ends, the
     * top Views need to be restored to their original, non-scrubbing states.
     * This method calls {@link #initTopViews()} to do this.
     */
    @Override
    public void onScrubEnded() {
        initTopViews();
    }

    /**
     * Initializes various components of this Activity. Some components of this
     * Activity are not "completely initialized" until a specific AsyncTask is
     * completed.
     * <p>
     * This Activity's AsyncTasks are started in {@link #onResume()}.
     *
     * @param savedInstanceState The savedInstanceState is not used
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_stock);
        ButterKnife.bind(this);
        initStockFromHomeActivity();
        setTitle(stock.getName());
        initOverviewAndNewsButtons();
        initSparkView();
        initNewsRecyclerView();
        initTopViews();
        initStatToViewMap();
        AndroidThreeTen.init(this); // Used in DownloadChartsTask
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isStarred = getIntent().getBooleanExtra("Is in favorites", false);
        wasStarredInitially = isStarred;
        isInPrefs = isStarred;
    }

    /**
     * Through intent extras, {@link HomeActivity} passes information about the
     * Stock that will be displayed in this activity. This method initializes
     * {@link #stock} using the information about stock that have been passed
     * from HomeActivity. This method does not set any of stock's fields that
     * are declared in {@link AdvancedStock} - this only sets stock's fields
     * that are declared in {@link Stock}.
     * <p>
     * The information from HomeActivity can represent one of
     * two types of {@link StockInHomeActivity}:
     * <ul>
     * <li>{@link ConcreteStock} is represented:
     * ticker, name, state, price, change point, and change percent are passed
     * <li>{@link ConcreteStockWithAhVals} is represented:
     * ticker, name, state, price, change point, change percent, after hours
     * price, after hours change point, and after hours change percent are
     * passed
     * </ul>
     */
    private void initStockFromHomeActivity() {
        final Intent intent = getIntent();
        final String ticker, name;
        final Stock.State state;
        final double price, changePoint, changePercent;

        ticker = intent.getStringExtra("Ticker");
        name = intent.getStringExtra("Name");

        final String[] data = intent.getStringArrayExtra("Data");
        state = Util.stringToStateMap.get(data[0]);

        price = parseDouble(data[1]);
        changePoint = parseDouble(data[2]);
        changePercent = parseDouble(data[3]);

        if (state == AFTER_HOURS || state == PREMARKET) {
            final double ahPrice, ahChangePoint, ahChangePercent;
            ahPrice = parseDouble(data[4]);
            ahChangePoint = parseDouble(data[5]);
            ahChangePercent = parseDouble(data[6]);

            stock = new ConcreteAdvancedStockWithAhVals(state, ticker, name,
                    price, changePoint, changePercent,
                    ahPrice, ahChangePoint, ahChangePercent);
        } else {
            stock = new ConcreteAdvancedStock(state, ticker, name,
                    price, changePoint, changePercent);
        }
    }

    /**
     * Initializes the {@link #overviewBtn} and {@link #newsBtn} Buttons that
     * are defined in this Activity's XML.
     */
    private void initOverviewAndNewsButtons() {
        overviewBtn.setTextColor(Color.WHITE); // Selected initially
        overviewBtn.setOnClickListener(view -> {
            overviewBtn.setTextColor(Color.WHITE);
            newsBtn.setTextColor(Color.DKGRAY);

            viewFlipper.setDisplayedChild(0);
        });
        newsBtn.setOnClickListener(view -> {
            overviewBtn.setTextColor(Color.DKGRAY);
            newsBtn.setTextColor(Color.WHITE);

            viewFlipper.setDisplayedChild(1);
        });
    }

    /**
     * Initializes {@link #sparkView}, {@link #sparkViewAdapter}, and {@link
     * #chartPeriodPicker}.
     */
    private void initSparkView() {
        sparkViewAdapter = new SparkViewAdapter(); // Init without prices or dates
        sparkView.setAdapter(sparkViewAdapter);
        /* SparkView needs its OnScrubListener member variable to be non-null in
         * order for the scrubbing line to work properly. The purpose of this
         * line is to make the scrubbing line work. All the scrub listening is
         * handled in onScrubbed(). */
        sparkView.setScrubListener(value -> {
        });
        final float touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        sparkView.setOnTouchListener(
                new CustomScrubGestureDetector(sparkView, this, touchSlop));
        chartPeriodPicker.setOnItemSelectedListener(this);
    }

    /**
     * Initializes {@link #newsRv} and {@link #newsRecyclerAdapter}.
     * Initializing newsRecyclerAdapter includes setting newsRv's {@link
     * NewsRecyclerAdapter.OnItemClickListener} and {@link
     * NewsRecyclerAdapter.OnItemLongClickListener}.
     *
     * @see ArticleLongClickPopupWindow
     */
    private void initNewsRecyclerView() {
        newsRv.setLayoutManager(new LinearLayoutManager(this));
        newsRv.addItemDecoration(new NewsRecyclerDivider(this));
        newsRecyclerAdapter = new NewsRecyclerAdapter(
                article -> {
                    final Intent webViewIntent =
                            new Intent(this, WebViewActivity.class);
                    webViewIntent.putExtra("URL", article.getUrl());
                    startActivity(webViewIntent);
                },
                article -> {
                    final ArticleLongClickPopupWindow popupWindow =
                            new ArticleLongClickPopupWindow(this, article);
                    popupWindow.showAtLocation(newsRv, Gravity.CENTER, 0, 0);
                }
        );
        newsRv.setAdapter(newsRecyclerAdapter);
    }

    /**
     * Initialize {@link #statToViewMap} with all {@link Stat}s.
     *
     * @see #statToViewMap
     */
    private void initStatToViewMap() {
        statToViewMap.put(Stat.PREV_CLOSE, prevClose);
        statToViewMap.put(Stat.OPEN, open);
        statToViewMap.put(Stat.VOLUME, volume);
        statToViewMap.put(Stat.AVG_VOLUME, avgVolume);
        statToViewMap.put(Stat.TODAYS_LOW, todaysLow);
        statToViewMap.put(Stat.TODAYS_HIGH, todaysHigh);
        statToViewMap.put(Stat.FIFTY_TWO_WEEK_LOW, fiftyTwoWeekLow);
        statToViewMap.put(Stat.FIFTY_TWO_WEEK_HIGH, fiftyTwoWeekHigh);
        statToViewMap.put(Stat.MARKET_CAP, marketCap);
        statToViewMap.put(Stat.PE_RATIO, peRatio);
        statToViewMap.put(Stat.EPS, eps);
        statToViewMap.put(Stat.YIELD, yield);
        statToViewMap.put(Stat.DESCRIPTION, description);
    }

    /**
     * Re-initializes {@link #timer} because it is invalidated when {@link
     * #onPause()} is called. This method then uses timer to create and execute
     * {@link DownloadChartsTask}s and {@link DownloadStatsTask}s on different
     * constant intervals. This method also creates and executes a single
     * instance of {@link DownloadNewsTask}.
     */
    @Override
    protected void onResume() {
        super.onResume();

        final TimerTask chartsTask = new TimerTask() {
            @Override
            public void run() {
                if (consecFails_chartTask < NUM_CONSEC_TASK_FAILS_ALLOWED) {
                    new DownloadChartsTask(stock, IndividualStockActivity.this)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        };
        final TimerTask statsTask = new TimerTask() {
            @Override
            public void run() {
                if (consecFails_statsTask < NUM_CONSEC_TASK_FAILS_ALLOWED) {
                    new DownloadStatsTask(stock, IndividualStockActivity.this)
                            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        };

        timer = new Timer();

        // Run charts task every 1 minute, starting immediately
        timer.schedule(chartsTask, 0, 60000);

        // Run stats task every 15 seconds, starting immediately
        timer.schedule(statsTask, 0, 15000);


        // Start tasks that don't update
        new DownloadNewsTask(stock.getTicker(), newsRecyclerAdapter.getArticleSparseArray(), this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * This method stops scheduled AsyncTasks by cancelling {@link #timer} and
     * adds or removes {@link #stock}'s presence from {@link #prefs} to reflect
     * the current star status of stock.
     * <p>
     * If stock's information is not in prefs but is starred, add its
     * information to Tickers, Names, and Data TSV, set the "Stock Added"
     * boolean in prefs to true, and add stock's information to the following
     * prefs strings:
     * <ul>
     * <li>"Stock Added Ticker"
     * <li>"Stock Added Name"
     * <li>"Stock Added Data" (7 element TSV)
     * </ul>.
     * If stock's information is in prefs already but it is not starred, remove
     * its information from Tickers, Names, and Data TSV, set the "Stock
     * Removed" boolean in prefs to true, and add stock's information to the
     * following prefs strings:
     * <ul>
     * <li>"Stock Removed Ticker"
     * </ul>
     *
     * @see #addStockToPreferences()
     * @see #removeStockFromPreferences()
     * @see IndividualStockActivity#onResume()
     */
    @Override
    protected void onPause() {
        super.onPause();

        // cancel() invalidates timer - it must be re-initialized to use again
        timer.cancel();

        if (isStarred != wasStarredInitially &&
                isStarred != isInPrefs) {
            // If the star status (favorites status) has changed

            if (isInPrefs) {
                removeStockFromPreferences();
                isInPrefs = false;

                prefs.edit().putBoolean("Stock Added", false).apply();
                prefs.edit().putBoolean("Stock Removed", true).apply();

                prefs.edit().putString("Stock Removed Ticker", stock.getTicker()).apply();
            } else {
                addStockToPreferences();
                isInPrefs = true;

                prefs.edit().putBoolean("Stock Added", true).apply();
                prefs.edit().putBoolean("Stock Removed", false).apply();

                prefs.edit().putString("Stock Added Ticker", stock.getTicker()).apply();
                prefs.edit().putString("Stock Added Name", stock.getName()).apply();
                final String dataStr;
                if (stock instanceof StockWithAhVals) {
                    dataStr = TextUtils.join("\t", new ConcreteStockWithAhVals(stock)
                            .getDataAsArray());
                } else {
                    dataStr = TextUtils.join("\t", new ConcreteStock(stock)
                            .getDataAsArray());
                }
                prefs.edit().putString("Stock Added Data", dataStr).apply();
            }
        }
    }

    /**
     * This is called whenever an item of {@link #chartPeriodPicker} is
     * selected. This method updates {@link #sparkViewAdapter} by making it
     * reference the correct selected ChartPeriod, making it reference the
     * correct chart's prices and dates, and calling {@link
     * SparkViewAdapter#notifyDataSetChanged()}. Lastly, this function updates
     * this Activity's top Views by calling {@link #initTopViews()}.
     *
     * @param index The index of the selected item
     */
    @Override
    public void onItemSelected(final int index) {
        // chartPeriodPicker's values come from chartPeriods string-array resource
        // chartPeriodStr must be a value in CHART_PERIOD_STRS
        final CharSequence chartPeriodStr = chartPeriodPicker.getValues()[index];
        final CharSequence[] CHART_PERIOD_STRS = getResources().getStringArray(R.array.chartPeriods);

        final ChartPeriod selected;

        for (int i = 0; i < CHART_PERIOD_STRS.length; i++) {
            if (CHART_PERIOD_STRS[i].equals(chartPeriodStr)) {
                selected = ChartPeriod.values()[i];

                if (selected != sparkViewAdapter.getChartPeriod()) {
                    // If the selected ChartPeriod has changed
                    sparkViewAdapter.setChartPeriod(selected);
                    sparkViewAdapter.setPrices(stock.getPrices(selected));
                    sparkViewAdapter.setDates(stock.getDates(selected));
                    sparkViewAdapter.notifyDataSetChanged();

                    initTopViews();
                }
                break;
            }
        }
    }

    /**
     * Initializes the contents of this Activity's standard options menu.
     *
     * @param menu The Menu containing the star MenuItem
     * @return True because we want the menu to be shown
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_activity, menu);
        final MenuItem starItem = menu.findItem(R.id.starMenuItem);
        starItem.setIcon(isStarred ? R.drawable.star_on : R.drawable.star_off);
        return true;
    }

    /**
     * This method handles the selection of a {@link MenuItem} from the options
     * menu of this Activity. If the star is pressed, toggle {@link
     * #isStarred}.
     *
     * @param item The selected MenuItem
     * @return True if a known item was selected, otherwise call the super
     * method
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.starMenuItem:
                isStarred = !isStarred; // Toggle
                item.setIcon(isStarred ? R.drawable.star_on : R.drawable.star_off);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds values from {@link #stock} to preferences: adds stock's ticker to
     * Tickers TSV, stock's name to Names TSV, and stock's data to Data TSV.
     * There are seven data values added in this order: state, price, change
     * point, change percent, after hours price, after hours change point, and
     * after hours change percent. These seven data values are added to Data TSV
     * regardless of whether or not stock has after hours values. If stock does
     * not have after hours values (is not instanceof {@link StockWithAhVals}),
     * then 0s are added in place of the three after hours values. stock's
     * ticker, name, and data values are added to the front of each preference
     * string, meaning that the stock is inserted at the top of the list of
     * stocks that are represented in prefs. This function does not check if
     * stock is already in prefs before adding stock.
     * <p>
     * If stock's state is {@link Stock.State#ERROR}, this function does
     * nothing.
     */
    private void addStockToPreferences() {
        if (stock.getState() == ERROR) {
            return;
        }

        final String tickersTSV = prefs.getString("Tickers TSV", "");
        final String namesTSV = prefs.getString("Names TSV", "");
        final String dataTSV = prefs.getString("Data TSV", "");

        final String dataStr;
        if (stock instanceof StockWithAhVals) {
            final StockWithAhVals ahStock = (StockWithAhVals) stock;
            dataStr = String.format(Locale.US, "%s\t%f\t%f\t%f\t%f\t%f\t%f",
                    ahStock.getState().toString(),
                    ahStock.getPrice(),
                    ahStock.getChangePoint(),
                    ahStock.getChangePercent(),
                    ahStock.getAfterHoursPrice(),
                    ahStock.getAfterHoursChangePoint(),
                    ahStock.getAfterHoursChangePercent());
        } else {
            dataStr = String.format(Locale.US, "%s\t%f\t%f\t%f\t%d\t%d\t%d",
                    stock.getState().toString(),
                    stock.getPrice(),
                    stock.getChangePoint(),
                    stock.getChangePercent(),
                    0, 0, 0);
        }

        if (!tickersTSV.isEmpty()) {
            // There are other stocks in favorites
            prefs.edit().putString("Tickers TSV", stock.getTicker() + '\t' + tickersTSV).apply();
            prefs.edit().putString("Names TSV", stock.getName() + '\t' + namesTSV).apply();
            prefs.edit().putString("Data TSV", dataStr + '\t' + dataTSV).apply();
        } else {
            // There are no stocks in favorites, this will be the first
            prefs.edit().putString("Tickers TSV", stock.getTicker()).apply();
            prefs.edit().putString("Names TSV", stock.getName()).apply();
            prefs.edit().putString("Data TSV", dataStr).apply();
        }
    }

    /**
     * Removes {@link #stock} from preferences: removes stock's ticker from
     * Tickers TSV, stock's name from Names TSV, and stock's data from Data TSV.
     */
    private void removeStockFromPreferences() {
        final String tickersTSV = prefs.getString("Tickers TSV", "");
        final String namesTSV = prefs.getString("Names TSV", "");
        final String[] tickerArr = tickersTSV.split("\t");
        final String[] nameArr = namesTSV.split("\t");

        if (!tickerArr[0].isEmpty()) {
            final List<String> tickerList = new ArrayList<>(Arrays.asList(tickerArr));
            final List<String> nameList = new ArrayList<>(Arrays.asList(nameArr));
            final List<String> dataList = new ArrayList<>(Arrays.asList(
                    prefs.getString("Data TSV", "").split("\t")));

            final int tickerNdx = tickerList.indexOf(stock.getTicker());
            tickerList.remove(tickerNdx);
            nameList.remove(tickerNdx);
            prefs.edit().putString("Tickers TSV", TextUtils.join("\t", tickerList)).apply();
            prefs.edit().putString("Names TSV", TextUtils.join("\t", nameList)).apply();

            // 7 data elements per 1 ticker. DataNdx is the index of the first element to delete.
            final int dataNdx = tickerNdx * 7;
            for (int deleteCount = 1; deleteCount <= 7; deleteCount++) {
                dataList.remove(dataNdx);
            }
            prefs.edit().putString("Data TSV", TextUtils.join("\t", dataList)).apply();
        }
    }

    /**
     * This method is called from {@link
     * #onDownloadStatsTaskCompleted(int, Set)} to update display Stat values.
     * <p>
     * This method checks if the {@link TextSwitcher} that is displaying stat,
     * is not displaying the value of stat that {@link #stock} has. If the
     * display value and stock's value are the same, this method does nothing.
     * Otherwise, this method updates the display to show the updated value of
     * stat.
     * <p>
     * For views in the "Key Statistics" or description sections, the value of
     * {@link #showsRealValues_stats} determines whether or not the TextSwitcher
     * update (text change) is animated or not.
     *
     * @param stat The {@link Stat} to update
     * @see #onDownloadStatsTaskCompleted(int, Set)
     */
    private void updateDisplayStat(final Stat stat) {
        final TextSwitcher displayView = statToViewMap.get(stat);
        final String displayed =
                ((TextView) displayView.getCurrentView()).getText().toString();
        String updated = "";

        switch (stat) {
            case PREV_CLOSE:
                updated = getString(R.string.double2dec, stock.getPrevClose());
                break;
            case OPEN:
                updated = getString(R.string.double2dec, stock.getOpen());
                break;
            case VOLUME:
                updated = stock.getVolume();
                break;
            case AVG_VOLUME:
                updated = stock.getAverageVolume();
                break;
            case TODAYS_LOW:
                updated = getString(R.string.double2dec, stock.getTodaysLow());
                break;
            case TODAYS_HIGH:
                updated = getString(R.string.double2dec, stock.getTodaysHigh());
                break;
            case FIFTY_TWO_WEEK_LOW:
                updated = getString(R.string.double2dec, stock.getFiftyTwoWeekLow());
                break;
            case FIFTY_TWO_WEEK_HIGH:
                updated = getString(R.string.double2dec, stock.getFiftyTwoWeekHigh());
                break;
            case MARKET_CAP:
                updated = stock.getMarketCap();
                break;
            case PE_RATIO:
                updated = getString(R.string.double2dec, stock.getPeRatio());
                break;
            case EPS:
                updated = getString(R.string.double2dec, stock.getEps());
                break;
            case YIELD:
                updated = getString(R.string.double2dec, stock.getYield());
                break;
            case DESCRIPTION:
                updated = stock.getDescription();
                break;
        }

        // Key Statistics and description are TextSwitchers
        if (!displayed.equals(updated)) {

            /* If real values are already shown, don't animate the text change.
             * Only animate the text change the first time the text is changed
             * to show real values. */
            if (showsRealValues_stats) {
                displayView.setCurrentText(updated);
            } else {
                displayView.setText(updated);
            }
        }
    }

    /**
     * This method sets {@link #top_time} to show the display name of the passed
     * in {@link ChartPeriod}.
     *
     * @param chartPeriod The ChartPeriod whose display name should be shown
     */
    private void setScrubTime(@NonNull final ChartPeriod chartPeriod) {
        switch (chartPeriod) {
            case ONE_DAY:
                top_time.setText(getString(R.string.today));
                break;
            case TWO_WEEKS:
                top_time.setText(getString(R.string.pastTwoWeeks));
                break;
            case ONE_MONTH:
                top_time.setText(getString(R.string.pastMonth));
                break;
            case THREE_MONTHS:
                top_time.setText(getString(R.string.pastThreeMonths));
                break;
            case ONE_YEAR:
                top_time.setText(getString(R.string.pastYear));
                break;
            case FIVE_YEARS:
                top_time.setText(getString(R.string.pastFiveYears));
                break;
        }
    }


    /**
     * An AsyncTask that updates {@link AdvancedStock} through setter methods
     * defined in AdvancedStock. Updates the AdvancedStock's chart fields
     * (prices and dates) for each {@link ChartPeriod}.
     */
    private static final class DownloadChartsTask extends AsyncTask<Void, Integer, Integer> {

        private final AdvancedStock stock;
        private final Set<ChartPeriod> missingChartPeriods = new HashSet<>();
        private final WeakReference<DownloadChartsTaskListener> completionListener;

        private DownloadChartsTask(final AdvancedStock stock,
                                   final DownloadChartsTaskListener completionListener) {
            this.stock = stock;
            this.completionListener = new WeakReference<>(completionListener);
        }

        /**
         * Gets the prices and dates needed for all the charts of {@link #stock}.
         * The one day chart is taken from the MarketWatch multiple-stock
         * website, and all other "big" charts are taken from two WSJ websites.
         * This function connects to three websites:
         * <ul>
         * <li>to the MarketWatch multiple-stock website
         * <li>to stock's WSJ overview website
         * <li>to stock's WSJ historical quotes AJAX website
         * </ul>
         * <p>
         * If an {@link IOException} is thrown during any of the three
         * connections, the returned {@link DownloadChartsTask.Status} can
         * represent which websites failed to connect. Because of the
         * co-dependence of the information taken from the two WSJ websites,
         * these two connections are treated as pair by the returned Status. For
         * example, if this method throws and IOException when connecting to the
         * WSJ historical quotes AJAX website, the returned Status will state
         * that there was an IOException from WSJ, without specifying which WSJ
         * website threw the IOException, and regardless of the status of the
         * connection to the WSJ overview website.
         * <p>
         * The loading of the one day chart and the loading of the big charts
         * are treated separately. Meaning that the loading of the big charts is
         * not effected by the status of the one day chart, and vice versa.
         * <p>
         * If a Stock isn't old enough to contain all the data for a {@link
         * ChartPeriod}'s chart, then that ChartPeriod's prices
         * and dates are set to empty lists. The "missing" ChartPeriod is also
         * added to {@link #missingChartPeriods}.
         *
         * @param voids Take no parameters
         * @return The {@link DownloadChartsTask.Status} of the function
         */
        @Override
        protected Integer doInBackground(final Void... voids) {
            int status = Status.GOOD;

            Document multiDoc;
            try {
                multiDoc = Jsoup.connect(
                        "https://www.marketwatch.com/investing/multi?tickers=" + stock.getTicker())
                        .timeout(20000)
                        .get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                multiDoc = null;
                missingChartPeriods.add(ChartPeriod.ONE_DAY);
                status = Status.IO_EXCEPTION_FOR_MW_ONLY;
            }

            if (multiDoc != null) {
                /* Some stocks have no chart data. If this is the case, chart_prices will be an
                 * empty array list. */
                final ArrayList<Double> chartPrices_1day = new ArrayList<>();

                final Element multiQuoteValueRoot = multiDoc.selectFirst(
                        "html > body > div#blanket > div[class*=multi] > div#maincontent > " +
                                "div[class^=block multiquote] > div[class^=quotedisplay] > " +
                                "div[class^=section activeQuote bgQuote]");
                final Element javascriptElmnt = multiQuoteValueRoot.selectFirst(
                        ":root > div.intradaychart > script[type=text/javascript]");
                final String jsonString = substringBetween(
                        javascriptElmnt.toString(),
                        "var chartData = [", "];");

                /* If there is no chart data, javascriptElmnt element still exists
                 * in the HTML and there is still some javascript code in
                 * javascriptElmnt.toString(). There is just no chart data embedded
                 * in the javascript. This means that the call to substringBetween()
                 * on javascriptElmnt.toString() will return null, because no
                 * substring between the open and close parameters
                 * (substringBetween() parameters) exists. */
                final String javascriptStr = substringBetween(
                        javascriptElmnt.toString(),
                        "Trades\":[", "]");
                if (javascriptStr != null) {
                    try {
                        final JSONObject topObj = new JSONObject(jsonString);
                        final JSONObject valueOuterObj = topObj.getJSONObject("Value");
                        final JSONArray dataArr = valueOuterObj.getJSONArray("Data");
                        final JSONObject valueInnerObj = dataArr.getJSONObject(0).getJSONObject("Value");

                        final JSONArray sessionsArr = valueInnerObj.getJSONArray("Sessions");
                        final JSONObject sessionsNdxZero = sessionsArr.getJSONObject(0);
                        final JSONArray tradesArr = sessionsNdxZero.getJSONArray("Trades");
                        final int numPrices = tradesArr.length();

                        /* Fill chartPrices_1day. If null values are found, replace
                         * them with the last non-null value. If the first value is
                         * null, replace it with the first non-null value. */
                        if (tradesArr.length() > 0) {
                            chartPrices_1day.ensureCapacity(numPrices);

                            /* Init as out of bounds. If firstNonNullNdx is never
                             * changed to an index in bounds, then all values in
                             * tradesArr are null. */
                            int firstNonNullNdx = numPrices;

                            // Find firstNonNullNdx and fill chartPrices up through firstNonNullNdx
                            /* After this if/else statement, chartPrices is filled
                             * with non-null values up through firstNonNullNdx. */
                            if (tradesArr.get(0).toString().equals("null")) {
                                for (int i = 1; i < numPrices; i++) { // Redundant to check index 0
                                    if (!tradesArr.get(i).toString().equals("null")) {
                                        firstNonNullNdx = i;

                                        /* The first non-null value has been found.
                                         * The indexes < firstNonNullNdx have null
                                         * values and therefore should be replaced
                                         * with the first non-null value
                                         * (firstNonNullValue) which is at
                                         * firstNonNullNdx. */
                                        final double firstNonNullValue =
                                                parseDouble(tradesArr.get(firstNonNullNdx).toString());
                                        while (i >= 0) {
                                            chartPrices_1day.add(firstNonNullValue);
                                            i--;
                                        }
                                        break;
                                    }
                                }
                            } else {
                                firstNonNullNdx = 0;
                                chartPrices_1day.add(parseDouble(tradesArr.get(0).toString()));
                            }

                            // Fill chartPrices for the indexes after firstNonNullNdx
                            for (int i = firstNonNullNdx + 1; i < numPrices; i++) {
                                if (!tradesArr.get(i).toString().equals("null")) {
                                    chartPrices_1day.add(parseDouble(tradesArr.get(i).toString()));
                                } else {
                                    chartPrices_1day.add(chartPrices_1day.get(i - 1));
                                }
                            }


                            /* The values given on the Market Watch multi stock page can be slightly
                             * off by minutes. For example, the Market Watch multi stock page may
                             * correctly say that AAPL closed at $171.00, but the value used for the
                             * Market Watch graph (where we're getting the 1 day graph data) may
                             * show that the AAPL price at 4:00pm (time of close) is $171.21; for
                             * this example, it is likely that around the time of 4:00pm, AAPL's
                             * price moved toward $171.21 (or at least increased from its closing
                             * price). The largest problem that this issue causes is that a user
                             * could be scrubbing through the 1 day graph, and notice that the price
                             * at 4:00pm is different from the closing price - this also causes the
                             * change values at 4:00pm to be nonzero, which is incorrect. Fix this
                             * issue by manually setting the price at 4:00pm to the price at close.
                             * Recall that in the 1 day chart, the prices are taken every 5 minutes,
                             * starting at 9:30am - the 4:00pm price is at index 78. */
                            if (chartPrices_1day.size() >= 79) {
                                chartPrices_1day.set(78, stock.getPrice());
                            }

                            // Update stock's one day chart
                            stock.setPrices_1day(chartPrices_1day);
                        }
                    } catch (final JSONException jsone) {
                        Log.e("JSONException", jsone.getLocalizedMessage());
                        missingChartPeriods.add(ChartPeriod.ONE_DAY);
                    }
                } else {
                    missingChartPeriods.add(ChartPeriod.ONE_DAY);
                }
            }
            // Done with one day chart
            // Code below is for the big charts


            Document individualDoc;
            try {
                individualDoc = Jsoup.connect(
                        "https://quotes.wsj.com/" + stock.getTicker())
                        .timeout(20000)
                        .get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                individualDoc = null;
                missingChartPeriods.addAll(Arrays.asList(BIG_CHART_PERIODS));
                status = status == Status.GOOD ?
                        Status.IO_EXCEPTION_FOR_WSJ_ONLY :
                        Status.IO_EXCEPTION_FOR_MW_AND_WSJ;
            }

            if (individualDoc != null) {
                /* Get chart data for periods greater than one day from Wall Street
                 * Journal. Certain values from WSJ page are needed for the URL of
                 * the WSJ database of historical prices. */
                final Element contentFrame = individualDoc.selectFirst(
                        ":root > body > div.pageFrame > div.contentFrame");
                final Element module2 = contentFrame.selectFirst(
                        ":root > section[class$=section_1] > div.zonedModule[data-module-id=2]");

                final String countryCode = module2.selectFirst(
                        ":root > input#quote_country_code").ownText();
                final String exchangeCode = module2.selectFirst(
                        ":root > input#quote_exchange_code").ownText();
                final String quoteType = module2.selectFirst(
                        ":root > input#quote_type").ownText();

                final LocalDate today = LocalDate.now();
                /* Deduct extra two weeks because it doesn't hurt and ensures that the URL we create
                 * doesn't incorrectly believe that there isn't enough data for the five year chart. */
                final LocalDate fiveYearsAgo = today.minusYears(5).minusWeeks(2);
                final int period_5years = (int) ChronoUnit.DAYS.between(fiveYearsAgo, today);

                // Pad zeros on the left if necessary
                final String wsj_todayDateStr = String.format(Locale.US, "%d/%d/%d",
                        today.getMonthValue(), today.getDayOfMonth(), today.getYear());
                final String wsj_5yearsAgoDateStr = String.format(Locale.US, "%d/%d/%d",
                        fiveYearsAgo.getMonthValue(), fiveYearsAgo.getDayOfMonth(), fiveYearsAgo.getYear());

                /* If the WSJ URL parameters (ie. start date) request data from dates that
                 * are prior to a stock's existence, then the WSJ response delivers all
                 * historical data available for the stock. Because five years is the largest
                 * chart period we are using, data for the smaller periods can be grabbed
                 * from the five year WSJ response page. The actual number of data points
                 * that exists for a stock can be determined by parsing the WSJ response and
                 * counting the number of certain elements (ie. table rows). */
                final String wsj_url_5years = String.format(Locale.US,
                        "https://quotes.wsj.com/ajax/historicalprices/4/%s?MOD_VIEW=page" +
                                "&ticker=%s&country=%s&exchange=%s&instrumentType=%s&num_rows=%d" +
                                "&range_days=%d&startDate=%s&endDate=%s",
                        stock.getTicker(), stock.getTicker(), countryCode, exchangeCode, quoteType,
                        period_5years, period_5years, wsj_5yearsAgoDateStr, wsj_todayDateStr);


                Document fiveYearDoc;
                try {
                    fiveYearDoc = Jsoup.connect(wsj_url_5years)
                            .timeout(20000)
                            .get();
                } catch (final IOException ioe) {
                    Log.e("IOException", ioe.getLocalizedMessage());
                    fiveYearDoc = null;
                    missingChartPeriods.addAll(Arrays.asList(BIG_CHART_PERIODS));
                    status = status == Status.GOOD ?
                            Status.IO_EXCEPTION_FOR_WSJ_ONLY :
                            Status.IO_EXCEPTION_FOR_MW_AND_WSJ;
                }

                if (fiveYearDoc != null) {
                    final List<Double> chartPrices_5years = new ArrayList<>();
                    final List<Double> chartPrices_1year = new ArrayList<>();
                    final List<Double> chartPrices_3months = new ArrayList<>();
                    final List<Double> chartPrices_1month = new ArrayList<>();
                    final List<Double> chartPrices_2weeks = new ArrayList<>();
                    final List<List<Double>> chartPricesList = new ArrayList<>(Arrays.asList(
                            chartPrices_5years, chartPrices_1year, chartPrices_3months,
                            chartPrices_1month, chartPrices_2weeks));
                    final List<String> chartDates_5years = new ArrayList<>();
                    final List<String> chartDates_1year = new ArrayList<>();
                    final List<String> chartDates_3months = new ArrayList<>();
                    final List<String> chartDates_1month = new ArrayList<>();
                    final List<String> chartDates_2weeks = new ArrayList<>();
                    final List<List<String>> chartDatesList = new ArrayList<>(Arrays.asList(
                            chartDates_5years, chartDates_1year, chartDates_3months,
                            chartDates_1month, chartDates_2weeks));

                    int reverseNdx, periodNdx, i;

                    /* This is the number of data points needed for each period. The
                     * stock market is only open on weekdays, and there are 9
                     * holidays that the stock market closes for. So the stock
                     * market is open for ~252 days a year. Use this value to
                     * approximate the number of data points that should be in each
                     * period. */
                    final int[] SIZES = {1260, 252, 63, 21, 10};
                    /* Don't get too many data points for long chart periods because
                     * it takes too long and is unnecessary. These increments mean
                     * that for chartPrices for periods greater than 1 day, the list
                     * will either be empty (not enough data), or have a constant
                     * size. The non-empty sizes are: [5 year]=140, [1 year]=126,
                     * [3 month]=63, [1 month]=21, [2 weeks]=10. */
                    final int[] INCREMENTS = {9, 2, 1, 1, 1};

                    final Elements rowElmnts = fiveYearDoc.select(
                            ":root > body > div > div#historical_data_table > " +
                                    "div > table > tbody > tr");
                    final int NUM_DATA_POINTS = rowElmnts.size() <= 1260 ? rowElmnts.size() : 1260;
                    final double[] allChartPrices = new double[NUM_DATA_POINTS];
                    final String[] allChartDates = new String[NUM_DATA_POINTS];

                    /* The most recent prices are at the top of the WSJ page (top of
                     * the HTML table), and the oldest prices are at the bottom.
                     * Fill allChartPrices starting with the last price elements so
                     * that the oldest prices are the front of allPrices and the
                     * recent prices are at the end. Do the same for allChartTimes. */
                    for (i = 0, reverseNdx = NUM_DATA_POINTS - 1; reverseNdx >= 0; i++, reverseNdx--) {
                        /* Charts use the closing price of each day. The closing
                         * price is the 5th column in each row. The date is the 1st
                         * column in each row. */
                        allChartPrices[i] = parseDouble(rowElmnts.get(reverseNdx).selectFirst(
                                ":root > :eq(4)").ownText());
                        allChartDates[i] = rowElmnts.get(reverseNdx).selectFirst(
                                ":root > :eq(0)").ownText();
                    }

                    /* Fill chartPrices and chartDates for each period. If there is
                     * not enough data to represent a full period, then leave that
                     * period's chartPrices and chartDates empty. */
                    List<Double> curChartPrices;
                    List<String> curChartDates;
                    for (periodNdx = 0; periodNdx < chartPricesList.size(); periodNdx++) {
                        if (SIZES[periodNdx] <= NUM_DATA_POINTS) {
                            // If there are enough data points to fill this period

                            curChartPrices = chartPricesList.get(periodNdx);
                            curChartDates = chartDatesList.get(periodNdx);
                            for (reverseNdx = NUM_DATA_POINTS - SIZES[periodNdx];
                                 reverseNdx < NUM_DATA_POINTS;
                                 reverseNdx += INCREMENTS[periodNdx]) {
                                curChartPrices.add(allChartPrices[reverseNdx]);
                                curChartDates.add(allChartDates[reverseNdx]);
                            }
                        } else {
                            missingChartPeriods.add(BIG_CHART_PERIODS[periodNdx]);
                        }
                    }

                    // Update stock's charts for the "big" ChartPeriods
                    stock.setPrices_2weeks(chartPrices_2weeks);
                    stock.setPrices_1month(chartPrices_1month);
                    stock.setPrices_3months(chartPrices_3months);
                    stock.setPrices_1year(chartPrices_1year);
                    stock.setPrices_5years(chartPrices_5years);
                    stock.setDates_2weeks(chartDates_2weeks);
                    stock.setDates_1month(chartDates_1month);
                    stock.setDates_3months(chartDates_3months);
                    stock.setDates_1year(chartDates_1year);
                    stock.setDates_5years(chartDates_5years);
                }
            }

            return status;
        }

        /**
         * Notifies {@link #completionListener} that the task is complete.
         * <p>
         * {@link #missingChartPeriods} is passed to completionListener so that
         * completionListener can be aware of which ChartPeriods were missing.
         *
         * @param status The Status of the task
         */
        @Override
        protected void onPostExecute(final Integer status) {
            if (completionListener.get() != null) {
                completionListener.get().onDownloadChartsTaskCompleted(status, missingChartPeriods);
            }
        }


        interface Status {

            int GOOD = 0;
            int IO_EXCEPTION_FOR_MW_ONLY = 1;
            int IO_EXCEPTION_FOR_WSJ_ONLY = 2;
            int IO_EXCEPTION_FOR_MW_AND_WSJ = 3;

        }

    }


    /**
     * An AsyncTask that updates a {@link AdvancedStock} through setter methods
     * defined in AdvancedStock. Updates the AdvancedStock with values that are
     * parsed from the AdvancedStock's WSJ website.
     */
    private static final class DownloadStatsTask extends AsyncTask<Void, Integer, Integer> {

        private AdvancedStock stock;
        private final Set<Stat> missingStats = new HashSet<>();
        private final WeakReference<DownloadStatsTaskListener> completionListener;

        private DownloadStatsTask(final AdvancedStock stock,
                                  final DownloadStatsTaskListener completionListener) {
            this.stock = stock;
            this.completionListener = new WeakReference<>(completionListener);
        }

        /**
         * Connects to the {@link #stock}'s WSJ website and parses it for values
         * that are fields of {@link AdvancedStock}, excluding values related
         * the AdvancedStock's chart data (historical prices and dates). The
         * values updated in this method are represented by {@link Stat}, or are
         * "top values". On the WSJ website, many values are irregular values,
         * or "missing" values. {@link #missingStats} keeps track of these
         * values, and is passed as a parameter to {@link #completionListener}.
         * <p>
         * If an {@link IOException} is thrown while connecting to the WSJ
         * website, this returns {@link Status#IO_EXCEPTION}. Otherwise, {@link
         * Status#GOOD} is returned.
         *
         * @param voids Take no parameters
         * @return The Status of the task
         */
        @Override
        protected Integer doInBackground(final Void... voids) {
            int status = Status.GOOD;

            Document individualDoc;
            try {
                individualDoc = Jsoup.connect(
                        "https://quotes.wsj.com/" + stock.getTicker())
                        .timeout(8000)
                        .get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                individualDoc = null;
                status = Status.IO_EXCEPTION;
            }

            if (individualDoc != null) {
                /* Get chart data for periods greater than one day from Wall Street
                 * Journal. Certain values from WSJ page are needed for the URL of
                 * the WSJ database of historical prices. */
                final Element contentFrame = individualDoc.selectFirst(
                        ":root > body > div.pageFrame > div.contentFrame");
                final Element module2 = contentFrame.selectFirst(
                        ":root > section[class$=section_1] > div.zonedModule[data-module-id=2]");


                /* Get non-chart data. */
                final Element mainData = module2.selectFirst(
                        "ul[class$=info_main]");
                final double price, changePoint, changePercent, open, prevClose;
                // Remove ',' or '%' that could be in strings
                price = parseDouble(mainData.selectFirst(
                        ":root > li[class$=quote] > span.curr_price > " +
                                "span > span#quote_val").ownText().replaceAll("[^0-9.]+", ""));
                final Elements diffs = mainData.select(
                        ":root > li[class$=diff] > span > span");
                changePoint = parseDouble(
                        diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
                changePercent = parseDouble(
                        diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));
                // Assume that price, change point, and change percent cannot be missing
                stock.setPrice(price);
                stock.setChangePoint(changePoint);
                stock.setChangePercent(changePercent);

                final Elements openAndPrevClose = module2.select(
                        ":root > div > div[id=chart_divId] > div[class$=compare] > " +
                                "div[class$=compare_data] > ul > li > span.data_data");
                open = parseDouble(openAndPrevClose.get(0).ownText().replaceAll("[^0-9.]+", ""));
                prevClose = parseDouble(openAndPrevClose.get(1).ownText().replaceAll("[^0-9.]+", ""));
                /* If previous close isn't applicable (stock just had IPO),
                 * element exists and has value 0. Same applies for open. */
                if (open == 0) {
                    missingStats.add(Stat.OPEN);
                } else {
                    stock.setOpen(open);
                }
                if (prevClose == 0) {
                    missingStats.add(Stat.PREV_CLOSE);
                } else {
                    stock.setPrevClose(prevClose);
                }


                final Element subData = mainData.nextElementSibling();
                boolean stockHasAhVals = subData.className().endsWith("info_sub");

                final Stock.State state;
                final String stateStr;
                if (stockHasAhVals) {
                    // Ensure stock is the correct type
                    if (!(stock instanceof StockWithAhVals)) {
                        stock = new ConcreteAdvancedStockWithAhVals(stock);
                    }

                    final double ahPrice, ahChangePoint, ahChangePercent;
                    // Remove ',' or '%' that could be in strings
                    ahPrice = parseDouble(subData.selectFirst(
                            "span#ms_quote_val").ownText().replaceAll("[^0-9.]+", ""));
                    final Elements ah_diffs = subData.select(
                            "span[id] > span");
                    ahChangePoint = parseDouble(ah_diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
                    ahChangePercent = parseDouble(ah_diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));
                    final StockWithAhVals ahStock = (StockWithAhVals) stock;
                    ahStock.setAfterHoursPrice(ahPrice);
                    ahStock.setAfterHoursChangePoint(ahChangePoint);
                    ahStock.setAfterHoursChangePercent(ahChangePercent);

                    stateStr = subData.selectFirst("span").ownText();
                    state = stateStr.equals("AFTER HOURS") ? AFTER_HOURS : PREMARKET;
                } else {
                    // Ensure stock is the correct type
                    if (stock instanceof StockWithAhVals) {
                        stock = new ConcreteAdvancedStock(stock);
                    }

                    stateStr = mainData.selectFirst("span.timestamp_label").ownText();
                    state = stateStr.equals("REAL TIME") ? OPEN : CLOSED;
                }
                stock.setState(state);


                String strBuff;

                /* Values in the table (keyData1) can be either a real value (i.e.
                 * "310,540 - 313,799"), or something else (i.e. empty string). It
                 * is difficult to find examples of irregular values in this table.
                 * All the values are numeric and positive, and some of the values
                 * could start with a decimal, so check if the first char in the
                 * value is a digit or '.'. */
                final Elements keyData1 = module2.select(
                        "ul[class$=charts_info] > li > div > span.data_data");

                final String volume;
                strBuff = keyData1.get(0).ownText();
                if (!strBuff.isEmpty() && Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                    volume = strBuff;
                    stock.setVolume(volume);
                } else {
                    missingStats.add(Stat.VOLUME);
                }

                final String avgVolume;
                strBuff = keyData1.get(1).ownText();
                if (!strBuff.isEmpty() && Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                    avgVolume = strBuff;
                    stock.setAverageVolume(avgVolume);
                } else {
                    missingStats.add(Stat.AVG_VOLUME);
                }

                final double todaysLow, todaysHigh;
                strBuff = keyData1.get(2).ownText();
                if (!strBuff.isEmpty() && Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                    // " - " is between low and high values
                    final String[] todaysRange = strBuff.split("\\s-\\s");
                    todaysLow = parseDouble(todaysRange[0].replaceAll("[^0-9.]+", ""));
                    todaysHigh = parseDouble(todaysRange[1].replaceAll("[^0-9.]+", ""));
                    stock.setTodaysLow(todaysLow);
                    stock.setTodaysHigh(todaysHigh);
                } else {
                    missingStats.add(Stat.TODAYS_LOW);
                    missingStats.add(Stat.TODAYS_HIGH);
                }

                final double fiftyTwoWeekLow, fiftyTwoWeekHigh;
                strBuff = keyData1.get(3).ownText();
                if (!strBuff.isEmpty() && Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                    // " - " is between low and high values
                    final String[] fiftyTwoWeekRange = strBuff.split("\\s-\\s");
                    fiftyTwoWeekLow = parseDouble(fiftyTwoWeekRange[0].replaceAll("[^0-9.]+", ""));
                    fiftyTwoWeekHigh = parseDouble(fiftyTwoWeekRange[1].replaceAll("[^0-9.]+", ""));
                    stock.setFiftyTwoWeekLow(fiftyTwoWeekLow);
                    stock.setFiftyTwoWeekHigh(fiftyTwoWeekHigh);
                } else {
                    missingStats.add(Stat.FIFTY_TWO_WEEK_LOW);
                    missingStats.add(Stat.FIFTY_TWO_WEEK_HIGH);
                }


                /* Values in the table (keyData2) can be either a real value (i.e.
                 * "366,452"), a missing value (i.e. "N/A"), or something else (i.e.
                 * "BRK.A has not issued dividends in more than 1 year"). All the
                 * values are numeric, and some values could be negative, or start
                 * with a decimal, so check if the first char in the value is a
                 * digit, '.', or '-'. */
                final Element module6 = contentFrame.selectFirst(
                        ":root > section[class$=section_2] > div#contentCol > " +
                                "div:eq(1) > div.zonedModule[data-module-id=6]");
                final Elements keyData2 = module6.select(
                        "div > div[class$=keystock_drawer] > div > ul > li > div > span");

                final double peRatio; // P/E ratio can be negative
                strBuff = keyData2.get(0).ownText();
                if (Util.Char.isDigitOrDecOrMinus(strBuff.charAt(0))) {
                    peRatio = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
                    stock.setPeRatio(peRatio);
                } else {
                    missingStats.add(Stat.PE_RATIO);
                }

                final double eps; // EPS can be negative
                strBuff = keyData2.get(1).ownText();
                if (Util.Char.isDigitOrDecOrMinus(strBuff.charAt(0))) {
                    eps = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
                    stock.setEps(eps);
                } else {
                    missingStats.add(Stat.EPS);
                }

                final String marketCap;
                // Example market cap value: "1.4 T"
                strBuff = keyData2.get(2).ownText();
                if (Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                    marketCap = strBuff;
                    stock.setMarketCap(marketCap);
                } else {
                    missingStats.add(Stat.MARKET_CAP);
                }

                final double yield; // Yield can be negative
                strBuff = keyData2.get(5).ownText();
                if (Util.Char.isDigitOrDecOrMinus(strBuff.charAt(0))) {
                    yield = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
                    stock.setYield(yield);
                } else {
                    missingStats.add(Stat.YIELD);
                }

                final String description;
                final Element descriptionElmnt = contentFrame.selectFirst(
                        ":root > section[class$=section_2] > div#contentCol + div > " +
                                "div:eq(1) > div.zonedModule[data-module-id=11] > div > " +
                                "div[class$=data] > div[class$=description] > p.txtBody");
                // If there is no description, the description element (p.txtBody) doesn't exist
                if (descriptionElmnt != null) {
                    description = descriptionElmnt.ownText();
                    stock.setDescription(description);
                } else {
                    missingStats.add(Stat.DESCRIPTION);
                }
            }

            return status;
        }

        /**
         * Notifies {@link #completionListener} that the task is complete.
         * <p>
         * {@link #missingStats} is passed to completionListener so that
         * completionListener can be aware of which stats were missing.
         *
         * @param status The Status of the task
         */
        @Override
        protected void onPostExecute(final Integer status) {
            if (completionListener.get() != null) {
                completionListener.get().onDownloadStatsTaskCompleted(status, missingStats);
            }
        }


        interface Status {

            int GOOD = 0;
            int IO_EXCEPTION = 1;

        }

    }


    /**
     * An AsyncTask that fills a {@link SparseArray<Article>} with {@link
     * Article}. This method creates Articles by parsing {@link #stock}'s page
     * on Finviz.
     */
    private static final class DownloadNewsTask extends AsyncTask<Void, Integer, Integer> {

        private final String ticker;

        /**
         * To understand why a {@link SparseArray<Article>} is used instead of
         * a more traditional container of {@link Article}, look at {@link
         * NewsRecyclerAdapter}.
         */
        private final SparseArray<Article> sparseArray;

        private final WeakReference<DownloadNewsTaskListener> completionListener;

        /**
         * The only constructor of a DownloadNewsTask.
         * <p>
         * The Finviz website uses '-' in their stock tickers, not '.'. For
         * example, BRK.A is BRK-A on Finviz. Replace '.' with '-' immediately,
         * so that {@link #ticker} is the ticker for Finviz.
         *
         * @param ticker             The ticker passed from
         *                           IndividualStockActivity. This ticker could
         *                           have '.' in it.
         * @param sparseArray        The sparseArray to fill with Article
         * @param completionListener The listener to notify when this task is
         *                           completed
         */
        private DownloadNewsTask(final String ticker, final SparseArray<Article> sparseArray,
                                 final DownloadNewsTaskListener completionListener) {
            this.ticker = ticker.replaceAll("\\.", "-");
            this.sparseArray = sparseArray;
            this.completionListener = new WeakReference<>(completionListener);
        }

        /**
         * Connects to the Finviz website for {@link #stock} and parses the
         * page for article information. This information is used to create
         * {@link Article}s that are added to {@link #sparseArray}.
         * <p>
         * If an {@link IOException} is thrown while connecting to the Finviz
         * website, this method returns {@link Status#IO_EXCEPTION}. If no
         * articles are found, this returns {@link Status#NO_NEWS_ARTICLES}. If
         * at least one article is found, this returns {@link Status#GOOD}.
         *
         * @param voids Take no parameters
         * @return The Status of the task
         */
        @Override
        protected Integer doInBackground(final Void... voids) {
            int status = Status.GOOD;

            final String base_url = "https://finviz.com/quote.ashx?t=";
            final String url = base_url + ticker;

            Document doc;
            try {
                doc = Jsoup.connect(url)
                        .timeout(20000)
                        .get();
            } catch (final IOException ioe) {
                ioe.printStackTrace();
                doc = null;
                status = Status.IO_EXCEPTION;
            }

            if (doc != null) {
                final Element tableBody = doc.selectFirst("table#news-table > tbody");

                final Elements articleElmnts = tableBody.children();

                /* The HTML table that lists these values only displays the date
                 * when it is has not been seen before. For example, if an
                 * article from "Aug-20" has not been seen, the row will have
                 * a td[style] element that could be "Aug-20-18 08:46pm. But if
                 * an article from "Aug-20" has already been seen, the row does
                 * not have a td[style] element. The date values have a lot of
                 * extra whitespace - trim it off. */
                String curTitle, curSource, curDate, curUrl;
                String prevDate;
                if (articleElmnts.size() > 0) {
                    Element dateElmnt;
                    dateElmnt = articleElmnts.get(0).selectFirst("td[style]");
                    // Trim off the time; get the date only
                    curDate = StringUtils.substringBefore(dateElmnt.ownText().trim(), " ");
                    prevDate = curDate;

                    curTitle = articleElmnts.get(0).selectFirst("a").ownText();
                    curUrl = articleElmnts.get(0).selectFirst("a").attr("href");
                    curSource = articleElmnts.get(0).selectFirst("span").ownText();

                    /* Everytime a new date is found, that takes a spot in the
                     * sparse array. */
                    int sparseNdx = 1; // Index 0 is a date
                    sparseArray.append(sparseNdx++, new Article(curDate, curTitle, curSource, curUrl));

                    for (int articleNdx = 1; articleNdx < articleElmnts.size(); articleNdx++, sparseNdx++) {
                        dateElmnt = articleElmnts.get(articleNdx).selectFirst("td[style]");
                        if (dateElmnt == null) {
                            curDate = prevDate;
                        } else {
                            curDate = StringUtils.substringBefore(dateElmnt.ownText().trim(), " ");
                            prevDate = curDate;

                            sparseNdx++; // Date added, pushing sparseNdx back 1
                        }

                        curTitle = articleElmnts.get(articleNdx).selectFirst("a").ownText();
                        curUrl = articleElmnts.get(articleNdx).selectFirst("a").attr("href");
                        curSource = articleElmnts.get(articleNdx).selectFirst("td > span").ownText();

                        sparseArray.append(sparseNdx, new Article(curDate, curTitle, curSource, curUrl));
                    }
                } else {
                    status = Status.NO_NEWS_ARTICLES;
                }
            }

            return status;
        }

        /**
         * Notifies {@link #completionListener} that the task is complete.
         *
         * @param status The Status of the task
         */
        @Override
        protected void onPostExecute(final Integer status) {
            if (completionListener.get() != null) {
                completionListener.get().onDownloadNewsTaskCompleted(status);
            }
        }


        public interface Status {

            int GOOD = 0;
            int NO_NEWS_ARTICLES = 1;
            int IO_EXCEPTION = 2;

        }

    }

}
