package c.chasesriprajittichai.stockwatch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.wefika.horizontalpicker.HorizontalPicker;

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
import c.chasesriprajittichai.stockwatch.listeners.DownloadStockTaskListener;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;
import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteAdvancedStock;
import c.chasesriprajittichai.stockwatch.stocks.ConcreteAdvancedStockWithAhVals;
import c.chasesriprajittichai.stockwatch.stocks.StockWithAhVals;

import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.ERROR;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public final class IndividualStockActivity extends AppCompatActivity implements
        DownloadStockTaskListener,
        HorizontalPicker.OnItemSelected,
        CustomScrubGestureDetector.ScrubIndexListener {

    public enum Stat {
        TODAYS_RANGE, FIFTY_TWO_WEEK_RANGE, MARKET_CAP, PREV_CLOSE, PE_RATIO,
        EPS, YIELD, AVG_VOLUME, DESCRIPTION
    }

    // Big ChartPeriods excludes CHART_1D
    static final AdvancedStock.ChartPeriod[] BIG_CHART_PERIODS = {
            AdvancedStock.ChartPeriod.FIVE_YEARS, AdvancedStock.ChartPeriod.ONE_YEAR,
            AdvancedStock.ChartPeriod.THREE_MONTHS, AdvancedStock.ChartPeriod.ONE_MONTH,
            AdvancedStock.ChartPeriod.TWO_WEEKS
    };

    @BindView(R.id.viewFlipper_individual) ViewFlipper viewFlipper;
    @BindView(R.id.progressBar_individual) ProgressBar progressBar;
    @BindView(R.id.scrollView_allStockViews_individual) ScrollView allStockViewsScrollView;
    @BindView(R.id.textView_topPrice_individual) TextView top_price;
    @BindView(R.id.textView_topChangePoint_individual) TextView top_changePoint;
    @BindView(R.id.textView_topChangePercent_individual) TextView top_changePercent;
    @BindView(R.id.textView_topTime_individual) TextView top_time;
    @BindView(R.id.linearLayout_afterHoursData_individual) LinearLayout ah_linearLayout;
    @BindView(R.id.textView_topAfterHoursChangePoint_individual) TextView top_ah_changePoint;
    @BindView(R.id.textView_topAfterHoursChangePercent_individual) TextView top_ah_changePercent;
    @BindView(R.id.textView_topAfterHoursTime_individual) TextView top_ah_time;
    @BindView(R.id.sparkView_individual) CustomSparkView sparkView;
    @BindView(R.id.textView_chartsUnavailable) TextView chartsUnavailable;
    @BindView(R.id.horizontalPicker_chartPeriod_individual) HorizontalPicker chartPeriodPicker;
    @BindView(R.id.view_chartPeriodPickerUnderline_individual) View chartPeriodPickerUnderline;
    @BindView(R.id.view_divider_sparkViewToStats_individual) View sparkViewToStatsDivider;
    @BindView(R.id.textView_keyStatisticsHeader_individual) View keyStatisticsHeader;
    @BindView(R.id.textView_todaysLow_individual) TextView todaysLow;
    @BindView(R.id.textView_todaysHigh_individual) TextView todaysHigh;
    @BindView(R.id.textView_fiftyTwoWeekLow_individual) TextView fiftyTwoWeekLow;
    @BindView(R.id.textView_fiftyTwoWeekHigh_individual) TextView fiftyTwoWeekHigh;
    @BindView(R.id.textView_marketCap_individual) TextView marketCap;
    @BindView(R.id.textView_prevClose_individual) TextView prevClose;
    @BindView(R.id.textView_peRatio_individual) TextView peRatio;
    @BindView(R.id.textView_eps_individual) TextView eps;
    @BindView(R.id.textView_yield_individual) TextView yield;
    @BindView(R.id.textView_averageVolume_individual) TextView avgVolume;
    @BindView(R.id.view_divider_statisticsToDescription_individual) View statsToDescriptionDivider;
    @BindView(R.id.textView_description_individual) TextView description;

    private String ticker; // Needed to create stock
    private String name; // Needed to create stock
    private AdvancedStock stock;
    private boolean stockHasBeenInitialized = false; // stock is initialized in onDownloadStockTaskCompleted()
    private boolean wasInFavoritesInitially;
    private boolean isInFavorites;
    private SparkViewAdapter sparkViewAdapter;
    private SharedPreferences prefs;

    /* Called from DownloadStockDataTask.onPostExecute(). */
    @Override
    public void onDownloadStockTaskCompleted(final AdvancedStock stock,
                                             final Set<Stat> missingStats,
                                             final Set<AdvancedStock.ChartPeriod> missingChartPeriods) {
        this.stock = stock;
        stockHasBeenInitialized = true;


        // ChartPeriods from string-array resource are in increasing order (1D -> 5Y)
        final List<CharSequence> displayChartPeriods = Arrays.stream(
                getResources().getStringArray(R.array.chartPeriods)).collect(Collectors.toList());

        if (!missingChartPeriods.contains(AdvancedStock.ChartPeriod.ONE_DAY)) {
            sparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.ONE_DAY);
            sparkViewAdapter.setyData(this.stock.getPrices_1day());
            // Don't set dates for sparkViewAdapter for 1D
            sparkViewAdapter.notifyDataSetChanged();
        } else {
            displayChartPeriods.remove(0); // 1D is at index 0 of chartPeriods
        }

        for (final AdvancedStock.ChartPeriod p : BIG_CHART_PERIODS) {
            if (missingChartPeriods.contains(p)) {
                /* Always remove last node because displayChartPeriods is in
                 * increasing order (1D -> 5Y), unlike BIG_CHART_PERIODS which
                 * is in decreasing order. */
                displayChartPeriods.remove(displayChartPeriods.size() - 1);
            } else {
                if (missingChartPeriods.contains(AdvancedStock.ChartPeriod.ONE_DAY)) {
                    /* If the 1D chart is filled, the current ChartPeriod will
                     * be set to ONE_DAY. Otherwise, if the 1D chart is not
                     * filled, the initially selected ChartPeriod should be the
                     * smallest big ChartPeriod (TWO_WEEKS). By the way that
                     * we're filling the charts, if at least one big ChartPeriod
                     * is filled, that guarantees that the 2W chart is filled. */
                    sparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.TWO_WEEKS);
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

        if (missingChartPeriods.size() == AdvancedStock.ChartPeriod.values().length) {
            // If there are no filled charts
            initTopViewsStatic();
            sparkView.setVisibility(View.GONE);
            chartPeriodPicker.setVisibility(View.GONE);
            chartPeriodPickerUnderline.setVisibility(View.GONE);
        } else {
            // If there is at least 1 filled chart
            initTopViewsDynamic();
            chartsUnavailable.setVisibility(View.GONE);
        }

        final CharSequence[] chartPeriodsArr = new CharSequence[displayChartPeriods.size()];
        displayChartPeriods.toArray(chartPeriodsArr);
        chartPeriodPicker.setValues(chartPeriodsArr);


        if (!missingStats.contains(Stat.TODAYS_RANGE)) {
            todaysLow.setText(getString(R.string.double2dec, this.stock.getTodaysLow()));
            todaysHigh.setText(getString(R.string.double2dec, this.stock.getTodaysHigh()));
        } else {
            todaysLow.setText("N/A");
            todaysHigh.setText("N/A");
        }
        if (!missingStats.contains(Stat.FIFTY_TWO_WEEK_RANGE)) {
            fiftyTwoWeekLow.setText(getString(R.string.double2dec, this.stock.getFiftyTwoWeekLow()));
            fiftyTwoWeekHigh.setText(getString(R.string.double2dec, this.stock.getFiftyTwoWeekHigh()));
        } else {
            fiftyTwoWeekLow.setText("N/A");
            fiftyTwoWeekHigh.setText("N/A");
        }
        if (!missingStats.contains(Stat.MARKET_CAP)) {
            marketCap.setText(getString(R.string.string, this.stock.getMarketCap()));
        } else {
            marketCap.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.PREV_CLOSE)) {
            prevClose.setText(getString(R.string.double2dec, this.stock.getPrevClose()));
        } else {
            prevClose.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.PE_RATIO)) {
            peRatio.setText(getString(R.string.double2dec, this.stock.getPeRatio()));
        } else {
            peRatio.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.EPS)) {
            eps.setText(getString(R.string.double2dec, this.stock.getEps()));
        } else {
            eps.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.YIELD)) {
            yield.setText(getString(R.string.double2dec_percent, this.stock.getYield()));
        } else {
            yield.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.AVG_VOLUME)) {
            avgVolume.setText(getString(R.string.string, this.stock.getAverageVolume()));
        } else {
            avgVolume.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.DESCRIPTION)) {
            description.setText(this.stock.getDescription());
        } else {
            description.setText(getString(R.string.descriptionNotFound));
        }

        /* This activity was showing the progressBar, now show
         * allStockViewsScrollView. */
        viewFlipper.showNext();
    }

    /**
     * Initialize the top TextViews that are changed during scrubbing. This
     * includes the live price (large), the live change point, live change
     * percent, the change point at close, the change percent at close, and the
     * two TextViews that show values related to time.
     * <p>
     * This function contrasts {@link #initTopViewsStatic()} because this
     * function displays the time of the selected ChartPeriod when the user is
     * not scrubbing.
     * <p>
     * This function can also be called to restore these views to their initial
     * states.
     */
    private void initTopViewsDynamic() {
        top_price.setText(getString(R.string.double2dec, stock.getLivePrice()));
        setScrubTime(sparkViewAdapter.getChartPeriod());

        final double firstPriceOfPeriod = sparkViewAdapter.getY(0);
        final double changePoint;
        final double changePercent;

        if (sparkViewAdapter.getChartPeriod() == AdvancedStock.ChartPeriod.ONE_DAY) {
            changePoint = stock.getChangePoint();
            changePercent = stock.getChangePercent();

            /* Init TextViews for after hours data in ah_linearLayout and init
             * their visibility. */
            if (stock instanceof StockWithAhVals) {
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
     * includes the live price (large), the live change point, live change
     * percent, the change point at close, the change percent at close, and the
     * two TextViews that show values related to time.
     * <p>
     * This function should be used instead of {@link #initTopViewsDynamic()}
     * when no charts are filled with data. This function differs from
     * {@link #initTopViewsDynamic()} because it initializes the non-after-hours
     * time TextView to always show {@literal Today}.
     * <p>
     * This function can also be called to restore these views to their initial
     * states.
     */
    private void initTopViewsStatic() {
        top_price.setText(getString(R.string.double2dec, stock.getLivePrice()));
        setScrubTime(AdvancedStock.ChartPeriod.ONE_DAY);

        // Init views for after hours data in ah_linearLayout
        if (stock instanceof StockWithAhVals) {
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

        if (sparkViewAdapter.getChartPeriod() == AdvancedStock.ChartPeriod.ONE_DAY) {
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
        initTopViewsDynamic(); // Restore views that were effected by scrubbing
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_stock);
        ticker = getIntent().getStringExtra("Ticker");
        name = getIntent().getStringExtra("Name");
        setTitle(name);
        ButterKnife.bind(this);

        // Start task ASAP
        new DownloadStockDataTask(ticker, name, this).execute();

        AndroidThreeTen.init(this); // Init, timezone not actually used
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
        isInFavorites = getIntent().getBooleanExtra("Is in favorites", false);
        wasInFavoritesInitially = isInFavorites;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isInFavorites != wasInFavoritesInitially) {
            // If the star status (favorite status) has changed

            if (stockHasBeenInitialized) {
                if (wasInFavoritesInitially) {
                    removeStockFromPreferences();
                } else {
                    addStockToPreferences();
                }
            } else {
                if (wasInFavoritesInitially) {
                    removeStockFromPreferences();
                } else {
                    addStockToPreferences();
                }
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

        final AdvancedStock.ChartPeriod selected;

        for (int i = 0; i < CHART_PERIOD_STRS.length; i++) {
            if (CHART_PERIOD_STRS[i].equals(chartPeriodStr)) {
                selected = AdvancedStock.ChartPeriod.values()[i];

                if (selected != sparkViewAdapter.getChartPeriod()) {
                    // If the selected ChartPeriod has changed
                    sparkViewAdapter.setChartPeriod(selected);
                    sparkViewAdapter.setyData(stock.getPrices(selected));
                    sparkViewAdapter.setDates(stock.getDates(selected));
                    sparkViewAdapter.notifyDataSetChanged();
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
     * Adds stock to prefs: adds stock's ticker to Tickers TSV, adds
     * stock's name to Names TSV, and adds stock's data to Data TSV. stock is
     * added to the front of each preference string, meaning that stock is
     * inserted at the top of the list of stocks. This function does not check
     * if stock is already in prefs before adding stock.
     * <p>
     * If stock's state is ERROR, this function does nothing. If stock's state
     * is OPEN, the live data (i.e. stock.getPrice()) is added to prefs.
     * If stock's state is not ERROR or OPEN (stock instanceof
     * StockWithAhVals), the data at the last close (i.e. still use
     * stock.getPrice()) is added to prefs.
     * <p>
     * Functionality changes depending on the value of stockHasBeenInitialized.
     * If stock has not been initialized, the uninitialized data values of
     * stock are set to specific values (0) that are then added to Data TSV.
     * The state is set to OPEN, because the OPEN (and CLOSED) state are the
     * most limiting states (less functionality than the PREMARKET state, for
     * example).
     */
    private void addStockToPreferences() {
        final String tickersTSV = prefs.getString("Tickers TSV", "");
        final String namesTSV = prefs.getString("Names TSV", "");
        final String dataTSV = prefs.getString("Data TSV", "");

        final String dataStr;
        if (!stockHasBeenInitialized) {
            dataStr = String.format(Locale.US, "%s\t%d\t%d\t%d",
                    OPEN.toString(), 0, 0, 0);
        } else if (stock.getState() == ERROR) {
            return;
        } else {
            dataStr = String.format(Locale.US, "%s\t%f\t%f\t%f",
                    stock.getState().toString(),
                    stock.getPrice(),
                    stock.getChangePoint(),
                    stock.getChangePercent());
        }

        if (!tickersTSV.isEmpty()) {
            // There are other stocks in favorites
            prefs.edit().putString("Tickers TSV", ticker + '\t' + tickersTSV).apply();
            prefs.edit().putString("Names TSV", name + '\t' + namesTSV).apply();
            prefs.edit().putString("Data TSV", dataStr + '\t' + dataTSV).apply();
        } else {
            // There are no stocks in favorites, this will be the first
            prefs.edit().putString("Tickers TSV", ticker).apply();
            prefs.edit().putString("Names TSV", name).apply();
            prefs.edit().putString("Data TSV", dataStr).apply();
        }
    }

    /**
     * Removes stock from prefs: removes stock's ticker from Tickers TSV,
     * removes stock's name from Names TSV, and removes mStock's data from Data
     * TSV.
     * <p>
     * Functionality does not change depending on the value of
     * stockHasBeenInitialized.
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

            final int tickerNdx = tickerList.indexOf(ticker);
            tickerList.remove(tickerNdx);
            nameList.remove(tickerNdx);
            prefs.edit().putString("Tickers TSV", TextUtils.join("\t", tickerList)).apply();
            prefs.edit().putString("Names TSV", TextUtils.join("\t", nameList)).apply();

            // 4 data elements per 1 ticker. DataNdx is the index of the first element to delete.
            final int dataNdx = tickerNdx * 4;
            for (int deleteCount = 1; deleteCount <= 4; deleteCount++) { // Delete 4 data elements
                dataList.remove(dataNdx);
            }
            prefs.edit().putString("Data TSV", TextUtils.join("\t", dataList)).apply();
        }
    }

    private void setScrubTime(final AdvancedStock.ChartPeriod chartPeriod) {
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


    private static final class DownloadStockDataTask extends AsyncTask<Void, Integer, AdvancedStock> {

        // Needed to construct AdvancedStocks and for URLs. Get from intent.
        private final String ticker;

        // Needed to construct AdvancedStocks. Get from intent.
        private final String name;

        private final Set<Stat> missingStats = new HashSet<>();
        private final Set<AdvancedStock.ChartPeriod> missingChartPeriods = new HashSet<>();
        private final WeakReference<DownloadStockTaskListener> completionListener;

        private DownloadStockDataTask(final String ticker, final String name,
                                      final DownloadStockTaskListener completionListener) {
            this.ticker = ticker;
            this.name = name;
            this.completionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected AdvancedStock doInBackground(final Void... params) {
            final AdvancedStock ret;

            final Document multiDoc;
            try {
                multiDoc = Jsoup.connect(
                        "https://www.marketwatch.com/investing/multi?tickers=" + ticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                return ConcreteAdvancedStock.ERROR;
            }

            /* Some stocks have no chart data. If this is the case, chart_prices will be an
             * empty array list. */
            final BasicStock.State state;
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

            final String stateStr = multiQuoteValueRoot.selectFirst(
                    ":root > div.marketheader > p.column.marketstate").ownText();
            switch (stateStr.toLowerCase(Locale.US)) {
                case "before the bell":
                    state = PREMARKET;
                    break;
                case "market open":
                case "countdown to close":
                    state = OPEN;
                    break;
                case "after hours":
                    state = AFTER_HOURS;
                    break;
                case "market closed":
                    state = CLOSED;
                    break;
                default:
                    Log.e("UnrecognizedMarketWatchState", String.format(
                            "Unrecognized state string from Market Watch multiple stock page.%n" +
                                    "Unrecognized state string: %s%n" +
                                    "Ticker: %s", stateStr, ticker));
                    return ConcreteAdvancedStock.ERROR;
            }

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
                    }
                } catch (final JSONException jsone) {
                    Log.e("JSONException", jsone.getLocalizedMessage());
                    missingChartPeriods.add(AdvancedStock.ChartPeriod.ONE_DAY);
                }
            } else {
                missingChartPeriods.add(AdvancedStock.ChartPeriod.ONE_DAY);
            }


            final Document individualDoc;
            try {
                individualDoc = Jsoup.connect("https://quotes.wsj.com/" + ticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                return ConcreteAdvancedStock.ERROR;
            }

            /* Get chart data for periods greater than one day from Wall Street
             * Journal. Certain values from WSJ page are needed for the URL of
             * the WSJ database of historical prices. */
            final Element contentFrame = individualDoc.selectFirst(
                    ":root > body > div[class^=pageFrame] > div.contentFrame");
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
                    ticker, ticker, countryCode, exchangeCode, quoteType,
                    period_5years, period_5years, wsj_5yearsAgoDateStr, wsj_todayDateStr);


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

            Document fiveYearDoc = null;
            // Loop and fill chart prices for each chart period
            try {
                fiveYearDoc = Jsoup.connect(wsj_url_5years).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                missingChartPeriods.addAll(Arrays.asList(BIG_CHART_PERIODS));
            }

            if (fiveYearDoc != null) {
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
            }


            /* Get non-chart data. */
            final Element mainData = module2.selectFirst(
                    "ul[class$=info_main]");
            // Remove ',' or '%' that could be in strings
            final double price = parseDouble(mainData.selectFirst(
                    ":root > li[class$=quote] > span.curr_price > " +
                            "span > span#quote_val").ownText().replaceAll("[^0-9.]+", ""));
            final Elements diffs = mainData.select(
                    ":root > li[class$=diff] > span > span");
            final double changePoint = parseDouble(
                    diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
            final double changePercent = parseDouble(
                    diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));
            final double prevClose = parseDouble(
                    module2.selectFirst(
                            ":root > div > div[id$=divId] > div[class$=compare] > " +
                                    "div[class$=compare_data] > ul > li:eq(1) > " +
                                    "span.data_data").ownText().replaceAll("[^0-9.]+", ""));
            /* If previous close isn't applicable (stock just had IPO), element
             * exists and has value 0. */
            if (prevClose == 0) {
                missingStats.add(Stat.PREV_CLOSE);
            }


            /* The 1 day data is gathered from the Market Watch multi stock
             * page. The values given on that page can be slightly off by
             * minutes. For example, the Market Watch multi stock page may
             * correctly say that AAPL closed at $171.00, but the value used for
             * the Market Watch graph (which is where we're getting the 1 day
             * graph data) may say that the AAPL price at 4:00pm (time of close)
             * is $171.21; this issue occurs if AAPL's is changing at the end of
             * the open trading period and into the after hours period; for this
             * example, it is likely that around the time of 4:00pm, AAPL's
             * price moved toward $171.21 (or at least increased from its
             * closing price). The largest problem that this small price
             * difference causes is that a user could be scrubbing through the
             * 1 day graph, and notice that the price at 4:00pm is different
             * from the closing price - this also causes the change values at
             * 4:00pm to be nonzero, which is incorrect. Fix this issue by
             * manually setting the price at 4:00pm to the price at close for
             * stocks that have 1 day chart data up to at least 4:00pm. Recall
             * that in the 1 day chart, the prices are taken every 5 minutes,
             * starting at 9:30am - the 4:00pm price is at index 78. Now that we
             * have the price at close (price), fix the issue in the 1 day chart
             * data. */
            if (chartPrices_1day.size() >= 78) {
                chartPrices_1day.set(78, price);
            }


            final double ah_price;
            final double ah_changePoint;
            final double ah_changePercent;
            if (state == PREMARKET || state == AFTER_HOURS) {
                // Equivalent to checking if this stock instanceof StockWithAhVals
                // After hours values are live values, and regular values are values at close
                final Element subData = module2.selectFirst(
                        "ul[class$=info_sub] > li[class]");
                // Remove ',' or '%' that could be in strings
                ah_price = parseDouble(subData.selectFirst(
                        "span#ms_quote_val").ownText().replaceAll("[^0-9.]+", ""));
                final Elements ah_diffs = subData.select(
                        "span[id] > span");
                ah_changePoint = parseDouble(ah_diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
                ah_changePercent = parseDouble(ah_diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));
            } else {
                // Initialize unused data
                ah_price = -1;
                ah_changePoint = -1;
                ah_changePercent = -1;
            }


            String strBuff;
            final String NA = "N/A";


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
            } else {
                avgVolume = NA;
                missingStats.add(Stat.AVG_VOLUME);
            }

            final double todaysLow, todaysHigh;
            strBuff = keyData1.get(2).ownText();
            if (!strBuff.isEmpty() && Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                // " - " is between low and high values
                final String[] todaysRange = strBuff.split("\\s-\\s");
                todaysLow = parseDouble(todaysRange[0].replaceAll("[^0-9.]+", ""));
                todaysHigh = parseDouble(todaysRange[1].replaceAll("[^0-9.]+", ""));
            } else {
                todaysLow = -1;
                todaysHigh = -1;
                missingStats.add(Stat.TODAYS_RANGE);
            }

            final double fiftyTwoWeekLow, fiftyTwoWeekHigh;
            strBuff = keyData1.get(3).ownText();
            if (!strBuff.isEmpty() && Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                // " - " is between low and high values
                final String[] fiftyTwoWeekRange = strBuff.split("\\s-\\s");
                fiftyTwoWeekLow = parseDouble(fiftyTwoWeekRange[0].replaceAll("[^0-9.]+", ""));
                fiftyTwoWeekHigh = parseDouble(fiftyTwoWeekRange[1].replaceAll("[^0-9.]+", ""));
            } else {
                fiftyTwoWeekLow = -1;
                fiftyTwoWeekHigh = -1;
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
            } else {
                peRatio = -1;
                missingStats.add(Stat.PE_RATIO);
            }

            final double eps; // EPS can be negative
            strBuff = keyData2.get(1).ownText();
            if (Util.Char.isDigitOrDecOrMinus(strBuff.charAt(0))) {
                eps = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
            } else {
                eps = -1;
                missingStats.add(Stat.EPS);
            }

            final String marketCap;
            // Example market cap value: "1.4 T"
            strBuff = keyData2.get(2).ownText();
            if (Util.Char.isDigitOrDec(strBuff.charAt(0))) {
                marketCap = strBuff;
            } else {
                marketCap = NA;
                missingStats.add(Stat.MARKET_CAP);
            }

            final double yield; // Yield can be negative
            strBuff = keyData2.get(5).ownText();
            if (Util.Char.isDigitOrDecOrMinus(strBuff.charAt(0))) {
                yield = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
            } else {
                yield = -1;
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
            } else {
                description = NA;
                missingStats.add(Stat.DESCRIPTION);
            }


            if (state == PREMARKET || state == AFTER_HOURS) {
                // Equivalent to checking if this stock instanceof StockWithAhVals
                ret = new ConcreteAdvancedStockWithAhVals(state, ticker, name, price, changePoint,
                        changePercent, ah_price, ah_changePoint, ah_changePercent, todaysLow,
                        todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap, prevClose, peRatio,
                        eps, yield, avgVolume, description, chartPrices_1day, chartPrices_2weeks,
                        chartPrices_1month, chartPrices_3months, chartPrices_1year,
                        chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months,
                        chartDates_1year, chartDates_5years);
            } else {
                ret = new ConcreteAdvancedStock(state, ticker, name, price, changePoint, changePercent,
                        todaysLow, todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap,
                        prevClose, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                        chartPrices_2weeks, chartPrices_1month, chartPrices_3months,
                        chartPrices_1year, chartPrices_5years, chartDates_2weeks, chartDates_1month,
                        chartDates_3months, chartDates_1year, chartDates_5years);
            }

            return ret;
        }

        @Override
        protected void onPostExecute(final AdvancedStock stock) {
            completionListener.get().onDownloadStockTaskCompleted(stock, missingStats, missingChartPeriods);
        }

    }

}
