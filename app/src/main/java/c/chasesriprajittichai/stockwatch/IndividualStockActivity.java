package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import c.chasesriprajittichai.stockwatch.listeners.DownloadChartTaskListener;
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
import c.chasesriprajittichai.stockwatch.stocks.StockWithAhVals;

import static c.chasesriprajittichai.stockwatch.stocks.AdvancedStock.Stat;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.ERROR;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.Stock.State.PREMARKET;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public final class IndividualStockActivity extends AppCompatActivity implements
        HorizontalPicker.OnItemSelected,
        CustomScrubGestureDetector.ScrubIndexListener,
        DownloadChartTaskListener,
        DownloadStatsTaskListener,
        DownloadNewsTaskListener {

    @BindView(R.id.viewFlipper_overview_or_news_flipper) ViewFlipper viewFlipper;
    @BindView(R.id.button_overview) Button overviewBtn;
    @BindView(R.id.button_news) Button newsBtn;

    @BindView(R.id.textView_topPrice_individual) TextView top_price;
    @BindView(R.id.textView_topChangePoint_individual) TextView top_changePoint;
    @BindView(R.id.textView_topChangePercent_individual) TextView top_changePercent;
    @BindView(R.id.textView_topTime_individual) TextView top_time;
    @BindView(R.id.linearLayout_afterHoursData_individual) LinearLayout ah_linearLayout;
    @BindView(R.id.textView_topAfterHoursChangePoint_individual) TextView top_ah_changePoint;
    @BindView(R.id.textView_topAfterHoursChangePercent_individual) TextView top_ah_changePercent;
    @BindView(R.id.textView_topAfterHoursTime_individual) TextView top_ah_time;
    @BindView(R.id.progressBar_loadingCharts) ProgressBar loadingChartsProgressBar;
    @BindView(R.id.textView_chartsStatus) TextView chartsStatus;
    @BindView(R.id.sparkView_individual) CustomSparkView sparkView;
    @BindView(R.id.horizontalPicker_chartPeriod_individual) HorizontalPicker chartPeriodPicker;
    @BindView(R.id.view_chartPeriodPickerUnderline_individual) View chartPeriodPickerUnderline;
    @BindView(R.id.textView_keyStatisticsHeader_individual) TextView keyStatisticsHeader;
    @BindView(R.id.textSwitcher_todaysLow) TextSwitcher todaysLow;
    @BindView(R.id.textSwitcher_todaysHigh) TextSwitcher todaysHigh;
    @BindView(R.id.textSwitcher_fiftyTwoWeekLow) TextSwitcher fiftyTwoWeekLow;
    @BindView(R.id.textSwitcher_fiftyTwoWeekHigh) TextSwitcher fiftyTwoWeekHigh;
    @BindView(R.id.textSwitcher_marketCap) TextSwitcher marketCap;
    @BindView(R.id.textSwitcher_prevClose) TextSwitcher prevClose;
    @BindView(R.id.textSwitcher_peRatio) TextSwitcher peRatio;
    @BindView(R.id.textSwitcher_eps) TextSwitcher eps;
    @BindView(R.id.textSwitcher_yield) TextSwitcher yield;
    @BindView(R.id.textSwitcher_averageVolume) TextSwitcher avgVolume;
    @BindView(R.id.textSwitcher_description) TextSwitcher description;

    @BindView(R.id.recyclerView_newsRecycler) RecyclerView newsRv;
    @BindView(R.id.progressBar_loadingNews) ProgressBar loadingNewsProgressBar;
    @BindView(R.id.textView_newsStatus) TextView newsStatus;

    // Big ChartPeriods excludes 1D chart
    private static final ChartPeriod[] BIG_CHART_PERIODS = {
            ChartPeriod.FIVE_YEARS, ChartPeriod.ONE_YEAR,
            ChartPeriod.THREE_MONTHS, ChartPeriod.ONE_MONTH,
            ChartPeriod.TWO_WEEKS
    };

    private AdvancedStock stock;
    private boolean wasInFavoritesInitially;
    private boolean isInFavorites;
    private SparkViewAdapter sparkViewAdapter;
    private SharedPreferences prefs;
    private NewsRecyclerAdapter newsRecyclerAdapter;

    /* Called from DownloadChartTask.onPostExecute(). */
    @Override
    public void onDownloadChartTaskCompleted(final int status,
                                             final Set<ChartPeriod> missingChartPeriods) {
        switch (status) {
            case DownloadChartTask.Status.GOOD:
                // ChartPeriods from string-array resource are in increasing order (1D -> 5Y)
                final List<CharSequence> displayChartPeriods = Arrays.stream(
                        getResources().getStringArray(R.array.chartPeriods)).collect(Collectors.toList());

                if (!missingChartPeriods.contains(ChartPeriod.ONE_DAY)) {
                    sparkViewAdapter.setChartPeriod(ChartPeriod.ONE_DAY);
                    sparkViewAdapter.setyData(this.stock.getPrices_1day());
                    // Don't set dates for sparkViewAdapter for 1D
                    sparkViewAdapter.notifyDataSetChanged();
                } else {
                    displayChartPeriods.remove(0); // 1D is at index 0 of chartPeriods
                }

                for (final ChartPeriod p : BIG_CHART_PERIODS) {
                    if (missingChartPeriods.contains(p)) {
                        /* Always remove last node because displayChartPeriods is in
                         * increasing order (1D -> 5Y), unlike BIG_CHART_PERIODS which
                         * is in decreasing order. */
                        displayChartPeriods.remove(displayChartPeriods.size() - 1);
                    } else {
                        if (missingChartPeriods.contains(ChartPeriod.ONE_DAY)) {
                            /* If the 1D chart is filled, the current ChartPeriod will
                             * be set to ONE_DAY. Otherwise, if the 1D chart is not
                             * filled, the initially selected ChartPeriod should be the
                             * smallest big ChartPeriod (TWO_WEEKS). By the way that
                             * we're filling the charts, if at least one big ChartPeriod
                             * is filled, that guarantees that the 2W chart is filled. */
                            sparkViewAdapter.setChartPeriod(ChartPeriod.TWO_WEEKS);
                            sparkViewAdapter.setyData(this.stock.getPrices_2weeks());
                            sparkViewAdapter.setDates(this.stock.getDates_2weeks());
                            sparkViewAdapter.notifyDataSetChanged();
                        }

                        /* Smaller charts (shorter ChartPeriods) take their data from
                         * the largest chart that is filled. So once a filled chart is
                         * found, there is no need to check if the smaller charts are
                         * filled. */
                        break;
                    }
                }

                if (missingChartPeriods.size() == ChartPeriod.values().length) {
                    // If there are no filled charts
                    initTopViewsStatic();
                    chartsStatus.setText(getString(R.string.chartsUnavailable));

                    chartsStatus.setVisibility(View.VISIBLE);
                } else {
                    // If there is at least 1 filled chart
                    initTopViewsDynamic();

                    final CharSequence[] displayChartPeriodsArr = new CharSequence[displayChartPeriods.size()];
                    displayChartPeriods.toArray(displayChartPeriodsArr);
                    chartPeriodPicker.setValues(displayChartPeriodsArr);

                    sparkView.setVisibility(View.VISIBLE);
                    chartPeriodPicker.setVisibility(View.VISIBLE);
                    chartPeriodPickerUnderline.setVisibility(View.VISIBLE);
                }
                break;
            case DownloadChartTask.Status.IO_EXCEPTION_FOR_MW_AND_WSJ:
            case DownloadChartTask.Status.IO_EXCEPTION_FOR_MW_ONLY:
            case DownloadChartTask.Status.IO_EXCEPTION_FOR_WSJ_ONLY:
                initTopViewsStatic();
                chartsStatus.setText(getString(R.string.ioException_loadingCharts));

                chartsStatus.setVisibility(View.VISIBLE);
                break;
        }

        loadingChartsProgressBar.setVisibility(View.GONE);
    }

    /* Called from DownloadStatsTask.onPostExecute(). */
    @Override
    public void onDownloadStatsTaskCompleted(final int status,
                                             final Set<Stat> missingStats) {
        switch (status) {
            case DownloadStatsTask.Status.GOOD:
                if (!missingStats.contains(Stat.TODAYS_RANGE)) {
                    todaysLow.setText(getString(R.string.double2dec, stock.getTodaysLow()));
                    todaysHigh.setText(getString(R.string.double2dec, stock.getTodaysHigh()));
                } else {
                    todaysLow.setText(getString(R.string.na));
                    todaysHigh.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.FIFTY_TWO_WEEK_RANGE)) {
                    fiftyTwoWeekLow.setText(getString(R.string.double2dec, stock.getFiftyTwoWeekLow()));
                    fiftyTwoWeekHigh.setText(getString(R.string.double2dec, stock.getFiftyTwoWeekHigh()));
                } else {
                    fiftyTwoWeekLow.setText(getString(R.string.na));
                    fiftyTwoWeekHigh.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.MARKET_CAP)) {
                    marketCap.setText(getString(R.string.string, stock.getMarketCap()));
                } else {
                    marketCap.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.PREV_CLOSE)) {
                    prevClose.setText(getString(R.string.double2dec, stock.getPrevClose()));
                } else {
                    prevClose.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.PE_RATIO)) {
                    peRatio.setText(getString(R.string.double2dec, stock.getPeRatio()));
                } else {
                    peRatio.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.EPS)) {
                    eps.setText(getString(R.string.double2dec, stock.getEps()));
                } else {
                    eps.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.YIELD)) {
                    yield.setText(getString(R.string.double2dec_percent, stock.getYield()));
                } else {
                    yield.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.AVG_VOLUME)) {
                    avgVolume.setText(getString(R.string.string, stock.getAverageVolume()));
                } else {
                    avgVolume.setText(getString(R.string.na));
                }
                if (!missingStats.contains(Stat.DESCRIPTION)) {
                    description.setText(stock.getDescription());
                } else {
                    description.setText(getString(R.string.descriptionNotFound));
                }
                break;
            case DownloadStatsTask.Status.IO_EXCEPTION:
                todaysLow.setText(getString(R.string.x));
                todaysHigh.setText(getString(R.string.x));
                fiftyTwoWeekLow.setText(getString(R.string.x));
                fiftyTwoWeekHigh.setText(getString(R.string.x));
                marketCap.setText(getString(R.string.x));
                prevClose.setText(getString(R.string.x));
                peRatio.setText(getString(R.string.x));
                eps.setText(getString(R.string.x));
                yield.setText(getString(R.string.x));
                avgVolume.setText(getString(R.string.x));

                description.setText(getString(R.string.ioException_loadingDescription));
                break;
        }
    }

    /* Called from DownloadNewsTask.onPostExecute(). */
    @Override
    public void onDownloadNewsTaskCompleted(final Integer status) {
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
                loadingNewsProgressBar.setVisibility(View.GONE);
                newsRv.setVisibility(View.GONE);

                newsStatus.setText(getString(R.string.ioException_loadingNews));
                break;
        }
    }

    /**
     * Initialize the top TextViews that are changed during scrubbing. This
     * includes the live price (large), live change point, live change percent,
     * change point at close, change percent at close, and the two views that
     * show values related to time. Values are taken from {@link #stock}.
     * <p>
     * This function should be used when at least one chart is filled with data.
     * If a {@link ChartPeriod} that is larger than {@link ChartPeriod#ONE_DAY}
     * is filled and selected, the top change point and change percent values
     * displayed, are relative to the first price of the selected chart - but if
     * no charts are filled, then the top views should always display the values
     * from the current/most recent day. This function displays values that are
     * relative to the first price of the chart for the selected ChartPeriod,
     * unlike {@link #initTopViewsStatic()}.
     * <p>
     * This function should be called any time that the selected chart changes,
     * so that the top views can update to display the values for the selected
     * ChartPeriod.
     */
    private void initTopViewsDynamic() {
        top_price.setText(getString(R.string.double2dec, stock.getLivePrice()));
        setScrubTime(sparkViewAdapter.getChartPeriod());

        final double firstPriceOfPeriod = sparkViewAdapter.getY(0);
        final double changePoint;
        final double changePercent;

        if (sparkViewAdapter.getChartPeriod() == ChartPeriod.ONE_DAY) {
            changePoint = stock.getChangePoint();
            changePercent = stock.getChangePercent();

            /* Init TextViews for after hours data in ah_linearLayout and init
             * their visibility. */
            if (stock instanceof StockWithAhVals) {
                top_ah_time.setText(getString(R.string.afterHours));

                if (stock.getLiveChangePoint() < 0) {
                    // '-' is already part of the number
                    top_ah_changePoint.setText(getString(R.string.double2dec, stock.getLiveChangePoint()));
                    top_ah_changePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, stock.getLiveChangePercent()));
                } else {
                    top_ah_changePoint.setText(getString(R.string.plus_double2dec, stock.getLiveChangePoint()));
                    top_ah_changePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, stock.getLiveChangePercent()));
                }
                ah_linearLayout.setVisibility(View.VISIBLE);
            } else {
                ah_linearLayout.setVisibility(View.INVISIBLE);
            }
        } else {
            ah_linearLayout.setVisibility(View.INVISIBLE);

            /* If stock instanceof StockWithAhVals, change values are
             * comparisons between live values and the first price of the
             * current ChartPeriod. */
            changePoint = stock.getLivePrice() - firstPriceOfPeriod;
            changePercent = (changePoint / firstPriceOfPeriod) * 100;
        }

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
     * Initialize the top TextViews that are changed during scrubbing. This
     * includes the live price (large), live change point, live change percent,
     * change point at close, change percent at close, and the two views that
     * show values related to time. Values are taken from {@link #stock}.
     * <p>
     * This function is used in {@link #onCreate(Bundle)} to initialize the top
     * views to show the top values which are known before {@link
     * #onDownloadChartTaskCompleted(int, Set)} or {@link
     * #onDownloadStatsTaskCompleted(int, Set)} is called.
     * <p>
     * This function should be used instead of {@link #initTopViewsDynamic()}
     * when no charts are filled with data. If a {@link ChartPeriod} that is
     * larger than {@link ChartPeriod#ONE_DAY} is filled and selected, the top
     * change values displayed are relative to the first price of the selected
     * chart - but if no charts are filled, then the top views should always
     * display the values from the current/most recent day. This function does
     * not display values that are relative to the first price of the chart for
     * the selected ChartPeriod, unlike {@link #initTopViewsDynamic()}.
     */
    private void initTopViewsStatic() {
        top_price.setText(getString(R.string.double2dec, stock.getLivePrice()));
        setScrubTime(ChartPeriod.ONE_DAY);

        // Init views for after hours data in ah_linearLayout
        if (stock instanceof StockWithAhVals) {
            top_ah_time.setText(getString(R.string.afterHours));

            if (stock.getLiveChangePoint() < 0) {
                // '-' is already part of the number
                top_ah_changePoint.setText(getString(R.string.double2dec, stock.getLiveChangePoint()));
                top_ah_changePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, stock.getLiveChangePercent()));
            } else {
                top_ah_changePoint.setText(getString(R.string.plus_double2dec, stock.getLiveChangePoint()));
                top_ah_changePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, stock.getLiveChangePercent()));
            }
            ah_linearLayout.setVisibility(View.VISIBLE);
        } else {
            ah_linearLayout.setVisibility(View.INVISIBLE);
        }

        if (stock.getChangePoint() < 0) {
            // '-' is already part of the number
            top_changePoint.setText(getString(R.string.double2dec, stock.getChangePoint()));
            top_changePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, stock.getChangePercent()));
        } else {
            top_changePoint.setText(getString(R.string.plus_double2dec, stock.getChangePoint()));
            top_changePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, stock.getChangePercent()));
        }
    }

    /* Called from CustomScrubGestureDetector.onScrubbed(). */
    @Override
    public void onScrubbed(final int index) {
        // Get scrubbing price from the chart data for the selected ChartPeriod
        final double scrubPrice = sparkViewAdapter.getY(index);
        this.top_price.setText(getString(R.string.double2dec, scrubPrice));

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
                firstPriceOfSection = sparkViewAdapter.getY(0);

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
            firstPriceOfSection = sparkViewAdapter.getY(0);

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

    /* Called from CustomScrubGestureDetector.onScrubEnded(). */
    @Override
    public void onScrubEnded() {
        /* This function is called only if the spark view was scrubbed. The
         * spark view can only be scrubbed if there is at least one chart that
         * is filled. this is why initTopViewsDynamic() is called, not
         * initTopViewsStatic(). */
        initTopViewsDynamic(); // Restore views that were effected by scrubbing
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_stock);
        ButterKnife.bind(this);
        initStockFromHomeActivity();
        setTitle(stock.getName());
        initTopViewsStatic();
        initOverviewAndNewsButtons();
        initNewsRecyclerView();
        initSparkView();
        AndroidThreeTen.init(this); // Used in DownloadChartTask
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isInFavorites = getIntent().getBooleanExtra("Is in favorites", false);
        wasInFavoritesInitially = isInFavorites;

        // Start tasks
        new DownloadChartTask(stock, this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new DownloadStatsTask(stock, this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new DownloadNewsTask(stock.getTicker(), newsRecyclerAdapter.getArticleSparseArray(), this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Through intent extras, {@link HomeActivity} passes information about the
     * stock that will be displayed in this activity. The information from
     * HomeActivity can represent two instances of {@link Stock}: either a
     * {@link ConcreteStock} is represented - ticker, name, state, price, change
     * point, and change percent are passed; or a {@link
     * ConcreteStockWithAhVals} is represented - ticker, name, state, price,
     * change point, change percent, after hours price, after hours change
     * point, and after hours change percent are passed.
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

    private void initSparkView() {
        sparkViewAdapter = new SparkViewAdapter(); // Init as empty
        sparkView.setAdapter(sparkViewAdapter);
        /* SparkView needs its OnScrubListener member variable to non-null in
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

    private void initNewsRecyclerView() {
        newsRv.setLayoutManager(new LinearLayoutManager(this));
        newsRv.addItemDecoration(new NewsRecyclerDivider(this));
        newsRecyclerAdapter = new NewsRecyclerAdapter(article -> {
            final Intent webViewIntent = new Intent(this, WebViewActivity.class);
            webViewIntent.putExtra("URL", article.getUrl());
            startActivity(webViewIntent);
        });
        newsRv.setAdapter(newsRecyclerAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isInFavorites != wasInFavoritesInitially) {
            // If the star status (favorite status) has changed

            if (wasInFavoritesInitially) {
                removeStockFromPreferences();
            } else {
                addStockToPreferences();
            }

            /* The activity has paused. Update the favorites status.
             * The condition to be able to edit Tickers TSV and Data TSV are
             * dependent on whether or not the favorites status has changed. */
            wasInFavoritesInitially = isInFavorites;
        }
    }

    @Override
    public void onBackPressed() {
        /* The parent (non-override) onBackPressed() does not create a new
         * HomeActivity. So when we go back back to HomeActivity, the first
         * function called is onResume(); onCreate() is not called. HomeActivity
         * depends on Tickers TSV and Data TSV not being changed in between
         * calls to HomeActivity.onPause() and HomeActivity.onResume() (where
         * HomeActivity.onCreate() is not called in between
         * HomeActivity.onPause() and HomeActivity.onResume()). The TSV strings
         * stored in preferences can be changed within this class. Therefore, if
         * we don't start a new HomeActivity in this function, then it is
         * possible that Tickers TSV and Data TSV are changed in between calls
         * to HomeActivity.onResume() and HomeActivity.onPause(), which would
         * cause HomeActivity to function incorrectly. */
        final Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    /* For chartPeriodPicker. */
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
                    sparkViewAdapter.setyData(stock.getPrices(selected));
                    sparkViewAdapter.setDates(stock.getDates(selected));
                    sparkViewAdapter.notifyDataSetChanged();

                    /* This function is called only if the a ChartPeriod picker
                     * item was selected. An item can only be selected if there
                     * is at least one chart that is filled. this is why
                     * initTopViewsDynamic() is called, not initTopViewsStatic(). */
                    initTopViewsDynamic();
                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_activity, menu);
        final MenuItem starItem = menu.findItem(R.id.starMenuItem);
        starItem.setIcon(isInFavorites ? R.drawable.star_on : R.drawable.star_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.starMenuItem:
                isInFavorites = !isInFavorites; // Toggle
                item.setIcon(isInFavorites ? R.drawable.star_on : R.drawable.star_off);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds values from {@link #stock} to {@link #prefs}: adds stock's ticker to
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
     * Removes {@link #stock} from {@link #prefs}: removes stock's ticker from
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

    private void setScrubTime(final ChartPeriod chartPeriod) {
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


    private static final class DownloadChartTask extends AsyncTask<Void, Integer, Integer> {

        private final AdvancedStock stock;
        private final Set<ChartPeriod> missingChartPeriods = new HashSet<>();
        private final WeakReference<DownloadChartTaskListener> completionListener;

        private DownloadChartTask(final AdvancedStock stock,
                                  final DownloadChartTaskListener completionListener) {
            this.stock = stock;
            this.completionListener = new WeakReference<>(completionListener);
        }

        /**
         * Gets the prices and dates needed for all the charts of {@link #stock}.
         * The one day chart is taken from the MarketWatch multiple-stock-page,
         * and all other "big" charts are taken from the Wall Street Journal.
         * This function calls {@link Jsoup#connect(String)} three times: once
         * to the MarketWatch multiple-stock-page, and twice to Wall Street
         * Journal pages. The {@link DownloadChartTask.Status} code returned
         * tells us which website is causing thrown IOExceptions.
         * <p>
         * The one day chart and big charts are treated separately. Meaning that
         * the loading of the big charts is unaffected by the status of the one
         * day chart, and vice versa.
         *
         * @param voids Take no parameters
         * @return The {@link DownloadChartTask.Status} of the function
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

        @Override
        protected void onPostExecute(final Integer status) {
            completionListener.get().onDownloadChartTaskCompleted(status, missingChartPeriods);
        }


        interface Status {

            int GOOD = 0;
            int IO_EXCEPTION_FOR_MW_ONLY = 1;
            int IO_EXCEPTION_FOR_WSJ_ONLY = 2;
            int IO_EXCEPTION_FOR_MW_AND_WSJ = 3;

        }

    }


    private static final class DownloadStatsTask extends AsyncTask<Void, Integer, Integer> {

        private AdvancedStock stock;
        private final Set<Stat> missingStats = new HashSet<>();
        private final WeakReference<DownloadStatsTaskListener> completionListener;

        private DownloadStatsTask(final AdvancedStock stock,
                                  final DownloadStatsTaskListener completionListener) {
            this.stock = stock;
            this.completionListener = new WeakReference<>(completionListener);
        }

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
                final double price, changePoint, changePercent, prevClose;
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
                prevClose = parseDouble(
                        module2.selectFirst(
                                ":root > div > div[id$=divId] > div[class$=compare] > " +
                                        "div[class$=compare_data] > ul > li:eq(1) > " +
                                        "span.data_data").ownText().replaceAll("[^0-9.]+", ""));
                /* If previous close isn't applicable (stock just had IPO), element
                 * exists and has value 0. */
                if (prevClose == 0) {
                    missingStats.add(Stat.PREV_CLOSE);
                } else {
                    stock.setPrevClose(prevClose);
                }
                stock.setPrice(price);
                stock.setChangePoint(changePoint);
                stock.setChangePercent(changePercent);


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
                    missingStats.add(Stat.TODAYS_RANGE);
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
                    missingStats.add(Stat.FIFTY_TWO_WEEK_RANGE);
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

        @Override
        protected void onPostExecute(final Integer status) {
            completionListener.get().onDownloadStatsTaskCompleted(status, missingStats);
        }


        interface Status {

            int GOOD = 0;
            int IO_EXCEPTION = 1;

        }

    }


    private static final class DownloadNewsTask extends AsyncTask<Void, Integer, Integer> {

        private final String ticker;
        private final SparseArray<Article> sparseArray;
        private final WeakReference<DownloadNewsTaskListener> completionListener;

        private DownloadNewsTask(final String ticker, final SparseArray<Article> sparseArray,
                                 final DownloadNewsTaskListener completionListener) {
            this.ticker = ticker;
            this.sparseArray = sparseArray;
            this.completionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
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

        @Override
        protected void onPostExecute(final Integer status) {
            completionListener.get().onDownloadNewsTaskCompleted(status);
        }


        public interface Status {

            int GOOD = 0;
            int NO_NEWS_ARTICLES = 1;
            int IO_EXCEPTION = 2;

        }

    }

}
