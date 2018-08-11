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

import butterknife.BindView;
import butterknife.ButterKnife;
import c.chasesriprajittichai.stockwatch.listeners.DownloadIndividualStockTaskListener;
import c.chasesriprajittichai.stockwatch.recyclerview.CustomScrubGestureDetector;
import c.chasesriprajittichai.stockwatch.stocks.AdvancedStock;
import c.chasesriprajittichai.stockwatch.stocks.AfterHoursStock;
import c.chasesriprajittichai.stockwatch.stocks.BasicStock;
import c.chasesriprajittichai.stockwatch.stocks.PremarketStock;
import c.chasesriprajittichai.stockwatch.stocks.StockWithAfterHoursValues;

import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.AFTER_HOURS;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.CLOSED;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.ERROR;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.OPEN;
import static c.chasesriprajittichai.stockwatch.stocks.BasicStock.State.PREMARKET;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public final class IndividualStockActivity extends AppCompatActivity implements
        DownloadIndividualStockTaskListener, HorizontalPicker.OnItemSelected, CustomScrubGestureDetector.ScrubIndexListener {

    public enum Stat {
        TODAYS_RANGE, FIFTY_TWO_WEEK_RANGE, MARKET_CAP, PREV_CLOSE, PE_RATIO, EPS, YIELD, AVG_VOLUME,
        DESCRIPTION, CHART_1D, CHART_2W, CHART_1M, CHART_3M, CHART_1Y, CHART_5Y
    }

    @BindView(R.id.viewFlipper_individual) ViewFlipper mviewFlipper;
    @BindView(R.id.progressBar_individual) ProgressBar mprogressBar;
    @BindView(R.id.scrollView_allStockViews_individual) ScrollView mallStockViewsScrollView;
    @BindView(R.id.textView_scrubPrice_individual) TextView mscrubPrice;
    @BindView(R.id.textView_scrubChangePoint_individual) TextView mscrubChangePoint;
    @BindView(R.id.textView_scrubChangePercent_individual) TextView mscrubChangePercent;
    @BindView(R.id.linearLayout_afterHoursData_individual) LinearLayout mafterHoursLinearLayout;
    @BindView(R.id.textView_afterHoursChangePoint_individual) TextView mafterHoursChangePoint;
    @BindView(R.id.textView_afterHoursChangePercent_individual) TextView mafterHoursChangePercent;
    @BindView(R.id.textView_afterHoursTime_individual) TextView mafterHoursTime;
    @BindView(R.id.textView_scrubTime_individual) TextView mscrubTime;
    @BindView(R.id.sparkView_individual) CustomSparkView msparkView;
    @BindView(R.id.horizontalPicker_chartPeriod_individual) HorizontalPicker mchartPeriodPicker;
    @BindView(R.id.view_chartPeriodPickerUnderline_individual) View mchartPeriodPickerUnderline;
    @BindView(R.id.view_divider_sparkViewToStats_individual) View msparkViewToStatsDivider;
    @BindView(R.id.textView_keyStatisticsHeader_individual) View mkeyStatisticsHeader;
    @BindView(R.id.textView_todaysLow_individual) TextView mtodaysLow;
    @BindView(R.id.textView_todaysHigh_individual) TextView mtodaysHigh;
    @BindView(R.id.textView_fiftyTwoWeekLow_individual) TextView mfiftyTwoWeekLow;
    @BindView(R.id.textView_fiftyTwoWeekHigh_individual) TextView mfiftyTwoWeekHigh;
    @BindView(R.id.textView_marketCap_individual) TextView mmarketCap;
    @BindView(R.id.textView_prevClose_individual) TextView mprevClose;
    @BindView(R.id.textView_peRatio_individual) TextView mpeRatio;
    @BindView(R.id.textView_eps_individual) TextView meps;
    @BindView(R.id.textView_yield_individual) TextView myield;
    @BindView(R.id.textView_averageVolume_individual) TextView mavgVolume;
    @BindView(R.id.view_divider_statisticsToDescription_individual) View mstatsToDescriptionDivider;
    @BindView(R.id.textView_description_individual) TextView mdescriptionTextView;

    private String mticker; // Needed to create mstock
    private AdvancedStock mstock;
    private boolean mstockHasBeenInitialized = false; // mstock is initialized in onDownloadIndividualStockTaskCompleted()
    private AdvancedStock.ChartPeriod mcurChartPeriod = AdvancedStock.ChartPeriod.ONE_DAY;
    private boolean mwasInFavoritesInitially;
    private boolean misInFavorites;
    private SparkViewAdapter msparkViewAdapter;
    private SharedPreferences mpreferences;

    /* Called from DownloadStockDataTask.onPostExecute(). */
    @Override
    public void onDownloadIndividualStockTaskCompleted(final AdvancedStock stock, final Set<Stat> missingStats) {
        mstock = stock;
        mstockHasBeenInitialized = true;

        if (!getTitle().equals(mstock.getName())) {
            setTitle(mstock.getName());
        }

        if (!mstock.getYData_1day().isEmpty()) { /** Change: check for available charts using missingStats. */
            msparkViewAdapter.setyData(mstock.getYData_1day());
            msparkViewAdapter.notifyDataSetChanged();

            initScrubViews();
        }

        if (!missingStats.contains(Stat.TODAYS_RANGE)) {
            mtodaysLow.setText(getString(R.string.double2dec, mstock.getTodaysLow()));
            mtodaysHigh.setText(getString(R.string.double2dec, mstock.getTodaysHigh()));
        } else {
            mtodaysLow.setText("N/A");
            mtodaysHigh.setText("N/A");
        }
        if (!missingStats.contains(Stat.FIFTY_TWO_WEEK_RANGE)) {
            mfiftyTwoWeekLow.setText(getString(R.string.double2dec, mstock.getFiftyTwoWeekLow()));
            mfiftyTwoWeekHigh.setText(getString(R.string.double2dec, mstock.getFiftyTwoWeekHigh()));
        } else {
            mfiftyTwoWeekLow.setText("N/A");
            mfiftyTwoWeekHigh.setText("N/A");
        }
        if (!missingStats.contains(Stat.MARKET_CAP)) {
            mmarketCap.setText(getString(R.string.string, mstock.getMarketCap()));
        } else {
            mmarketCap.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.PREV_CLOSE)) {
            mprevClose.setText(getString(R.string.double2dec, mstock.getPrevClose()));
        } else {
            mprevClose.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.PE_RATIO)) {
            mpeRatio.setText(getString(R.string.double2dec, mstock.getPeRatio()));
        } else {
            mpeRatio.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.EPS)) {
            meps.setText(getString(R.string.double2dec, mstock.getEps()));
        } else {
            meps.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.YIELD)) {
            myield.setText(getString(R.string.double2dec, mstock.getYield()));
        } else {
            myield.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.AVG_VOLUME)) {
            mavgVolume.setText(getString(R.string.string, mstock.getAverageVolume()));
        } else {
            mavgVolume.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains(Stat.DESCRIPTION)) {
            mdescriptionTextView.setText(mstock.getDescription());
        } else {
            mdescriptionTextView.setText(getString(R.string.string, "Description not found"));
        }

        mviewFlipper.showNext(); // Was showing layout with mprogressBar, now show mallStockViewScrollView
    }

    /**
     * Initialize the views that are changed during scrubbing. This function should
     * also be called to restore these views to their initial states.
     */
    private void initScrubViews() {
        mscrubPrice.setText(getString(R.string.double2dec, mstock.getLivePrice()));
        setScrubTime(mcurChartPeriod);

        final double firstPriceOfPeriod = msparkViewAdapter.getY(0);
        final double changePoint;
        final double changePercent;

        if (mcurChartPeriod == AdvancedStock.ChartPeriod.ONE_DAY) {
            changePoint = mstock.getChangePoint();
            changePercent = mstock.getChangePercent();

            /* Init TextViews for after hours data in mafterHoursLinearLayout and init
             * their visibility. */
            if (mstock instanceof StockWithAfterHoursValues) {
                if (mstock.getLiveChangePoint() < 0) {
                    // '-' is already part of the number
                    mafterHoursChangePoint.setText(getString(R.string.double2dec, mstock.getLiveChangePoint()));
                    mafterHoursChangePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, mstock.getLiveChangePercent()));
                } else {
                    mafterHoursChangePoint.setText(getString(R.string.plus_double2dec, mstock.getLiveChangePoint()));
                    mafterHoursChangePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, mstock.getLiveChangePercent()));
                }
                mafterHoursLinearLayout.setVisibility(View.VISIBLE);
            } else {
                mafterHoursLinearLayout.setVisibility(View.INVISIBLE);
            }
        } else {
            mafterHoursLinearLayout.setVisibility(View.INVISIBLE);

            // Compare to price at last close if mstock instanceof StockWithAfterHoursValues
            changePoint = mstock.getPrice() - firstPriceOfPeriod;
            changePercent = (changePoint / firstPriceOfPeriod) * 100;
        }

        if (changePoint < 0) {
            // '-' is already part of the number
            mscrubChangePoint.setText(getString(R.string.double2dec, changePoint));
            mscrubChangePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, changePercent));
        } else {
            mscrubChangePoint.setText(getString(R.string.plus_double2dec, changePoint));
            mscrubChangePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, changePercent));
        }
    }

    /* Called from CustomScrubGestureDetector.onScrubbed(). */
    @Override
    public void onScrubbed(final int index) {
        // Get scrubbing price from the chart data for the selected ChartPeriod
        final double scrubPrice = msparkViewAdapter.getY(index);
        mscrubPrice.setText(getString(R.string.double2dec, scrubPrice));

        final double firstPriceOfSection;
        final double changePoint;
        final double changePercent;

        if (mcurChartPeriod == AdvancedStock.ChartPeriod.ONE_DAY) {
            /* After hours begins as soon as the open market closes. Therefore, the after
             * hours section of the chart should begin at 4:00pm, which is at index 78.
             * If the user is scrubbing in the after hours range of the chart, the change
             * values should change so that they are relative to the price at close. */
            if (index >= 78) {
                /* Because mstock instanceof StockWithAfterHoursValues, mstock.getPrice()
                 * returns the price at close. */
                firstPriceOfSection = mstock.getPrice();

                // "Hide" after hours change data, only show "After-Hours"
                mafterHoursChangePoint.setText("");
                mafterHoursChangePercent.setText("");
                mafterHoursTime.setText(getString(R.string.afterHours));
                mafterHoursLinearLayout.setVisibility(View.VISIBLE);
            } else {
                firstPriceOfSection = msparkViewAdapter.getY(0);

                mafterHoursLinearLayout.setVisibility(View.INVISIBLE);
            }

            /* There are 78 data points representing the open hours data (9:30am - 4:00pm ET).
             * This means that 78 data points represent 6.5 hours. Therefore, there is one data
             * point for every 5 minutes. This 5 minute step size is constant throughout all
             * states. The number of data points is dependent on the time of day. */
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
                mscrubTime.setText(getString(R.string.int_colon_int2dig_pm_ET, hour, minute));
            } else {
                mscrubTime.setText(getString(R.string.int_colon_int2dig_am_ET, hour, minute));
            }
        } else {
            firstPriceOfSection = msparkViewAdapter.getY(0);

            // Get scrubbing date from the chart data for the selected ChartPeriod
            mscrubTime.setText(getString(R.string.string, msparkViewAdapter.getDate(index)));
        }

        changePoint = scrubPrice - firstPriceOfSection;
        changePercent = (changePoint / firstPriceOfSection) * 100;

        if (changePoint < 0) {
            // '-' is already part of the number
            mscrubChangePoint.setText(getString(R.string.double2dec, changePoint));
            mscrubChangePercent.setText(getString(R.string.openParen_double2dec_percent_closeParen, changePercent));
        } else {
            mscrubChangePoint.setText(getString(R.string.plus_double2dec, changePoint));
            mscrubChangePercent.setText(getString(R.string.openParen_plus_double2dec_percent_closeParen, changePercent));
        }
    }

    /* Called from CustomScrubGestureDetector.onScrubEnded(). */
    @Override
    public void onScrubEnded() {
        initScrubViews(); // Restore views that were effected by scrubbing
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_individual_stock);
        setTitle(""); // Show empty title now, company name will be shown (in onPostExecute())
        ButterKnife.bind(this);
        mticker = getIntent().getStringExtra("Ticker");

        // Start task ASAP
        new DownloadStockDataTask(mticker, this).execute();

        AndroidThreeTen.init(this); // Init, timezone not actually used
        mpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        msparkViewAdapter = new SparkViewAdapter(); // Init as empty
        msparkView.setAdapter(msparkViewAdapter);
        /* SparkView needs its OnScrubListener member variable to non-null in order for
         * the scrubbing line to work properly. The purpose of this line is to make the
         * scrubbing line work. All the scrub listening is handled in onScrubbed(). */
        msparkView.setScrubListener(value -> {
        });
        final float touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        msparkView.setOnTouchListener(
                new CustomScrubGestureDetector(msparkView, this, touchSlop));
        mchartPeriodPicker.setOnItemSelectedListener(this);
        misInFavorites = getIntent().getBooleanExtra("Is in favorites", false);
        mwasInFavoritesInitially = misInFavorites;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (misInFavorites != mwasInFavoritesInitially) {
            // If the star status (favorite status) has changed

            if (mstockHasBeenInitialized) {
                if (mwasInFavoritesInitially) {
                    removeStockFromPreferences();
                } else {
                    addStockToPreferences();
                }
            } else {
                if (mwasInFavoritesInitially) {
                    removeStockFromPreferences();
                } else {
                    addStockToPreferences();
                }
            }

            /* The activity has paused. Update the favorites status.
             * The condition to be able to edit Tickers CSV and Data CSV are dependent on whether
             * or not the favorites status has changed. */
            mwasInFavoritesInitially = misInFavorites;
        }
    }

    @Override
    public void onBackPressed() {
        /* The parent (non-override) onBackPressed() does not create a new HomeActivity. So when we
         * go back back to HomeActivity, the first function called is onResume(); onCreate() is not
         * called. HomeActivity depends on Tickers CSV and Data CSV not being changed in between
         * calls to HomeActivity.onPause() and HomeActivity.onResume() (where
         * HomeActivity.onCreate() is not called in between HomeActivity.onPause() and
         * HomeActivity.onResume()). Tickers CSV and Data CSV can be changed within this class.
         * Therefore, if we don't start a new HomeActivity in this function, then it is possible
         * that Tickers CSV and Data CSV are changed in between calls to HomeActivity.onResume()
         * and HomeActivity.onPause(), which would cause HomeActivity to function incorrectly. */
        final Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    /* For mchartPeriodPicker. */
    @Override
    public void onItemSelected(final int index) {
        mcurChartPeriod = AdvancedStock.ChartPeriod.values()[index]; // Update mcurChartPeriod

        switch (index) {
            case 0: // 1D
                if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.ONE_DAY) {
                    msparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.ONE_DAY);
                    msparkViewAdapter.setyData(mstock.getYData_1day());
                    /* This activity shouldn't access msparkView's dates if the current
                     * ChartPeriod is ONE_DAY. The updating of the dates to the UI are
                     * managed in initScrubViews() and onScrubbed(). */
                }
                break;
            case 1: // 2W
                if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.TWO_WEEKS) {
                    msparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.TWO_WEEKS);
                    msparkViewAdapter.setyData(mstock.getYData_2weeks());
                    msparkViewAdapter.setDates(mstock.getDates_2weeks());
                }
                break;
            case 2: // 1M
                if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.ONE_MONTH) {
                    msparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.ONE_MONTH);
                    msparkViewAdapter.setyData(mstock.getYData_1month());
                    msparkViewAdapter.setDates(mstock.getDates_1month());
                }
                break;
            case 3: // 3M
                if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.THREE_MONTHS) {
                    msparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.THREE_MONTHS);
                    msparkViewAdapter.setyData(mstock.getYData_3months());
                    msparkViewAdapter.setDates(mstock.getDates_3months());
                }
                break;
            case 4: // 1Y
                if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.ONE_YEAR) {
                    msparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.ONE_YEAR);
                    msparkViewAdapter.setyData(mstock.getYData_1year());
                    msparkViewAdapter.setDates(mstock.getDates_1year());
                }
                break;
            case 5: // 5Y
                if (msparkViewAdapter.getChartPeriod() != AdvancedStock.ChartPeriod.FIVE_YEARS) {
                    msparkViewAdapter.setChartPeriod(AdvancedStock.ChartPeriod.FIVE_YEARS);
                    msparkViewAdapter.setyData(mstock.getYData_5years());
                    msparkViewAdapter.setDates(mstock.getDates_5years());
                }
                break;
        }

        msparkViewAdapter.notifyDataSetChanged();
        initScrubViews();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stock_activity, menu);
        final MenuItem starItem = menu.findItem(R.id.starMenuItem);
        starItem.setIcon(misInFavorites ? R.drawable.star_on : R.drawable.star_off);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.starMenuItem:
                misInFavorites = !misInFavorites; // Toggle
                item.setIcon(misInFavorites ? R.drawable.star_on : R.drawable.star_off);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adds mstock to mpreferences: adds mstock's ticker to Tickers CSV and and adds
     * mstock's data to Data CSV. mstock is added to the front of each preference
     * string, meaning that mstock is inserted at the top of the list of stocks. This
     * function does not check if mstock is already in mpreferences before adding mstock.
     * <p>
     * If mstock's state is ERROR, this function does nothing. If mstock's state is OPEN,
     * If mstock's state is OPEN, the live data (i.e. mstock.getPrice()) is added to
     * mpreferences. If mstock's state is not ERROR or OPEN (mstock instanceof
     * StockWithAfterHoursValues), the data at the last close (i.e. still use
     * mstock.getPrice()) is added to mpreferences.
     * <p>
     * Functionality changes depending on the value of mstockHasBeenInitialized.
     * If mstock has not been initialized (mstockHasBeenInitialized), mticker (same
     * value as what mstock.getTicker() once mstockHasBeenInitialized) is added to
     * Tickers CSV, and the uninitialized data values of mstock are set to specific
     * values that are then added to Data CSV. The state is set to CLOSED, because the
     * OPEN and CLOSED states are the most limiting states (less functionality than
     * PREMARKET state, for example). The change point and change percent values are set
     * to 0.
     */
    private void addStockToPreferences() {
        final String tickersCSV = mpreferences.getString("Tickers CSV", "");
        final String dataCSV = mpreferences.getString("Data CSV", "");

        final String dataStr;
        if (!mstockHasBeenInitialized) {
            dataStr = String.format(Locale.US, "%s,%d,%d,%d",
                    OPEN.toString(), 0, 0, 0);
        } else if (mstock.getState() == ERROR) {
            return;
        } else {
            dataStr = String.format(Locale.US, "%s,%f,%f,%f",
                    mstock.getState().toString(),
                    mstock.getPrice(),
                    mstock.getChangePoint(),
                    mstock.getChangePercent());
        }

        if (!tickersCSV.isEmpty()) {
            // There are other stocks in favorites
            mpreferences.edit().putString("Tickers CSV", mticker + ',' + tickersCSV).apply();
            mpreferences.edit().putString("Data CSV", dataStr + ',' + dataCSV).apply();
        } else {
            // There are no stocks in favorites, this will be the first
            mpreferences.edit().putString("Tickers CSV", mticker).apply();
            mpreferences.edit().putString("Data CSV", dataStr).apply();
        }
    }

    /**
     * Removes mstock from mpreferences; removes mstock's ticker from Tickers CSV and
     * removes mStock's data from Data CSV.
     * <p>
     * Functionality does not change depending on the value of mstockHasBeenInitialized.
     */
    private void removeStockFromPreferences() {
        final String tickersCSV = mpreferences.getString("Tickers CSV", "");
        final String[] tickerArr = tickersCSV.split(","); // "".split(",") returns {""}

        if (!tickerArr[0].isEmpty()) {
            final List<String> tickerList = new ArrayList<>(Arrays.asList(tickerArr));
            final List<String> dataList = new ArrayList<>(Arrays.asList(
                    mpreferences.getString("Data CSV", "").split(",")));

            final int tickerNdx = tickerList.indexOf(mticker);
            tickerList.remove(tickerNdx);
            mpreferences.edit().putString("Tickers CSV", TextUtils.join(",", tickerList)).apply();

            // 4 data elements per 1 ticker. DataNdx is the index of the first element to delete.
            final int dataNdx = tickerNdx * 4;
            for (int deleteCount = 1; deleteCount <= 4; deleteCount++) { // Delete 4 data elements
                dataList.remove(dataNdx);
            }
            mpreferences.edit().putString("Data CSV", TextUtils.join(",", dataList)).apply();
        }
    }

    private void setScrubTime(final AdvancedStock.ChartPeriod chartPeriod) {
        switch (chartPeriod) {
            case ONE_DAY:
                mscrubTime.setText(getString(R.string.today));
                break;
            case TWO_WEEKS:
                mscrubTime.setText(getString(R.string.pastTwoWeeks));
                break;
            case ONE_MONTH:
                mscrubTime.setText(getString(R.string.pastMonth));
                break;
            case THREE_MONTHS:
                mscrubTime.setText(getString(R.string.pastThreeMonths));
                break;
            case ONE_YEAR:
                mscrubTime.setText(getString(R.string.pastYear));
                break;
            case FIVE_YEARS:
                mscrubTime.setText(getString(R.string.pastFiveYears));
                break;
        }
    }


    private static final class DownloadStockDataTask extends AsyncTask<Void, Integer, AdvancedStock> {

        private final String mticker;

        /* It is common that a stock's stat is missing. If this is the case, "n/a" replaces the
         * value that should be there, or in rarer cases, there is an empty string replacing the
         * value that should be there. */
        private final Set<Stat> missingStats = new HashSet<>();

        private final WeakReference<DownloadIndividualStockTaskListener> mcompletionListener;

        private DownloadStockDataTask(final String ticker, final DownloadIndividualStockTaskListener completionListener) {
            mticker = ticker;
            mcompletionListener = new WeakReference<>(completionListener);
        }

        @Override
        protected AdvancedStock doInBackground(final Void... params) {
            final AdvancedStock ret;

            final Document multiDoc;
            try {
                multiDoc = Jsoup.connect("https://www.marketwatch.com/investing/multi?tickers=" + mticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                return AdvancedStock.ERROR_AdvancedStock;
            }

            String name = mticker; // Init as ticker, should change to company name found in JSON
            /* Some stocks have no chart data. If this is the case, chart_prices will be an
             * empty array list. */
            final BasicStock.State state;
            final ArrayList<Double> chartPrices_1day = new ArrayList<>();

            final Element multiQuoteValueRoot = multiDoc.selectFirst("html > body > div#blanket > " +
                    "div[class*=multi] > div#maincontent > div[class^=block multiquote] > " +
                    "div[class^=quotedisplay] > div[class^=section activeQuote bgQuote]");
            final Element javascriptElmnt = multiQuoteValueRoot.selectFirst(":root > div.intradaychart > script[type=text/javascript]");
            final String jsonString = substringBetween(javascriptElmnt.toString(), "var chartData = [", "];");

            final String stateStr = multiQuoteValueRoot.selectFirst(":root > div.marketheader > p.column.marketstate").ownText();
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
                                    "Ticker: %s", stateStr, mticker));
                    return AdvancedStock.ERROR_AdvancedStock;
            }

            /* If there is no chart data, javascriptElmnt element still exists in the HTML and
             * there is still some javascript code in javascriptElmnt.toString(). There is just no
             * chart data embedded in the javascript. This means that the call to substringBetween()
             * on javascriptElmnt.toString() will return null, because no substring between the open
             * and close parameters (substringBetween() parameters) exists. */
            final String javascriptStr = substringBetween(javascriptElmnt.toString(), "Trades\":[", "]");
            if (javascriptStr != null) {
                try {
                    final JSONObject topObj = new JSONObject(jsonString);
                    final JSONObject valueOuterObj = topObj.getJSONObject("Value");
                    final JSONArray dataArr = valueOuterObj.getJSONArray("Data");
                    final JSONObject valueInnerObj = dataArr.getJSONObject(0).getJSONObject("Value");
                    name = valueInnerObj.getString("Name");

                    final JSONArray sessionsArr = valueInnerObj.getJSONArray("Sessions");
                    final JSONObject sessionsNdxZero = sessionsArr.getJSONObject(0);
                    final JSONArray tradesArr = sessionsNdxZero.getJSONArray("Trades");
                    final int numPrices = tradesArr.length();

                    /* Fill chartPrices_1day. If null values are found, replace them with the last
                     * non-null value. If the first value is null, replace it with the
                     * first non-null value. */
                    if (tradesArr.length() > 0) {
                        chartPrices_1day.ensureCapacity(numPrices);

                        /* Init as out of bounds. If firstNonNullNdx is never changed to an index in
                         * bounds, then all values in tradesArr are null. */
                        int firstNonNullNdx = numPrices;

                        // Find firstNonNullNdx and fill chartPrices up through firstNonNullNdx
                        /* After this if/else statement, chartPrices is filled with non-null values
                         * up through firstNonNullNdx. */
                        if (tradesArr.get(0).toString().equals("null")) {
                            for (int i = 1; i < numPrices; i++) { // Redundant to check index 0
                                if (!tradesArr.get(i).toString().equals("null")) {
                                    firstNonNullNdx = i;

                                    /* The first non-null value has been found. The indexes <
                                     * firstNonNullNdx have null values and therefore should be
                                     * replaced with the first non-null value (firstNonNullValue)
                                     * which is at firstNonNullNdx. */
                                    final double firstNonNullValue = parseDouble(tradesArr.get(firstNonNullNdx).toString());
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
                }
            }


            final Document individualDoc;
            try {
                individualDoc = Jsoup.connect("https://quotes.wsj.com/" + mticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                return AdvancedStock.ERROR_AdvancedStock;
            }

            /* Get chart data for periods greater than one day from Wall Street Journal.
             * Certain values from WSJ page are needed for the URL of the WSJ database of
             * historical prices. */
            final Element contentFrame = individualDoc.selectFirst(":root > body > div[class^=pageFrame] > div.contentFrame");
            final Element module2 = contentFrame.selectFirst(":root > section[class$=section_1] > div.zonedModule[data-module-id=2]");

            final String countryCode = module2.selectFirst(":root > input#quote_country_code").ownText();
            final String exchangeCode = module2.selectFirst(":root > input#quote_exchange_code").ownText();
            final String quoteType = module2.selectFirst(":root > input#quote_type").ownText();

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

            /* If the WSJ URL parameters (ie. start date) request data from dates that are prior to
             * a stock's existence, then the WSJ response delivers all historical data available for
             * the stock. Because five years is the largest chart period we are using, data for the
             * smaller periods can be grabbed from the five year WSJ response page. The actual
             * number of data points that exists for a stock can be determined by parsing the WSJ
             * response and counting the number of certain elements (ie. table rows). */
            final String wsj_url_5years = String.format(Locale.US,
                    "https://quotes.wsj.com/ajax/historicalprices/4/%s?MOD_VIEW=page&ticker=%s&country=%s" +
                            "&exchange=%s&instrumentType=%s&num_rows=%d&range_days=%d&startDate=%s&endDate=%s",
                    mticker, mticker, countryCode, exchangeCode, quoteType,
                    period_5years, period_5years, wsj_5yearsAgoDateStr, wsj_todayDateStr);


            final List<Double> chartPrices_5years = new ArrayList<>();
            final List<Double> chartPrices_1year = new ArrayList<>();
            final List<Double> chartPrices_3months = new ArrayList<>();
            final List<Double> chartPrices_1month = new ArrayList<>();
            final List<Double> chartPrices_2weeks = new ArrayList<>();
            final List<List<Double>> chartPricesList = new ArrayList<>(Arrays.asList(
                    chartPrices_5years, chartPrices_1year, chartPrices_3months, chartPrices_1month, chartPrices_2weeks));
            final List<String> chartDates_5years = new ArrayList<>();
            final List<String> chartDates_1year = new ArrayList<>();
            final List<String> chartDates_3months = new ArrayList<>();
            final List<String> chartDates_1month = new ArrayList<>();
            final List<String> chartDates_2weeks = new ArrayList<>();
            final List<List<String>> chartDatesList = new ArrayList<>(Arrays.asList(
                    chartDates_5years, chartDates_1year, chartDates_3months, chartDates_1month, chartDates_2weeks));

            Document fiveYearDoc = null;
            // Loop and fill chart prices for each chart period
            try {
                fiveYearDoc = Jsoup.connect(wsj_url_5years).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                /** Add to missing stats? */
            }

            if (fiveYearDoc != null) {
                int reverseNdx, periodNdx, i;

                /* This is the number of data points needed for each period. The stock market is
                 * only open on weekdays, and there are 9 holidays that the stock market closes for.
                 * So the stock market is open for ~252 days a year. Use this value to approximate
                 * the number of data points that should be in each period. */
                final int[] PERIODS = {1260, 252, 63, 21, 10};
                /* Don't get too many data points for long chart periods because it takes too long
                 * and is unnecessary. Take no more than 150 data points max in a period. These
                 * increments mean that for chartPrices for periods greater than 1 day, the list
                 * will either be empty (not enough data), or have a constant size. The non-empty
                 * sizes are: [5 year]=140, [1 year]=126, [3 month]=63, [1 month]=21, [2 weeks]=10. */
                final int[] PERIOD_INCREMENTS = {9, 2, 1, 1, 1};

                final Elements rowElmnts = fiveYearDoc.select(":root > body > div > " +
                        "div#historical_data_table > div > table > tbody > tr");
                final int NUM_DATA_POINTS = rowElmnts.size() <= 1260 ? rowElmnts.size() : 1260;
                final double[] allChartPrices = new double[NUM_DATA_POINTS];
                final String[] allChartDates = new String[NUM_DATA_POINTS];

                /* The most recent prices are at the top of the WSJ page (top of the HTML table), and
                 * the oldest prices are at the bottom. Fill allChartPrices starting with the last
                 * price elements so that the oldest prices are the front of allPrices and the
                 * recent prices are at the end. Do the same for allChartTimes. */
                for (i = 0, reverseNdx = NUM_DATA_POINTS - 1; reverseNdx >= 0; i++, reverseNdx--) {
                    /* Charts use the closing price of each day. The closing price is the 5th column
                     * in each row. The date is the 1st column in each row. */
                    allChartPrices[i] = parseDouble(rowElmnts.get(reverseNdx).selectFirst(":root > :eq(4)").text());
                    allChartDates[i] = rowElmnts.get(reverseNdx).selectFirst(":root > :eq(0)").text();
                }

                /* Fill chartPrices and chartDates for each period. If there is not enough data to
                 * represent a full period, then leave that period's chartPrices and chartDates empty. */
                List<Double> curChartPrices;
                List<String> curChartDates;
                for (periodNdx = 0; periodNdx < chartPricesList.size(); periodNdx++) {
                    if (PERIODS[periodNdx] <= NUM_DATA_POINTS) {
                        // If there are enough data points to fill this period

                        curChartPrices = chartPricesList.get(periodNdx);
                        curChartDates = chartDatesList.get(periodNdx);
                        for (reverseNdx = NUM_DATA_POINTS - PERIODS[periodNdx]; reverseNdx < NUM_DATA_POINTS; reverseNdx += PERIOD_INCREMENTS[periodNdx]) {
                            curChartPrices.add(allChartPrices[reverseNdx]);
                            curChartDates.add(allChartDates[reverseNdx]);
                        }
                    }
                }
            }


            /* Get non-chart data. */
            final Element mainData = module2.selectFirst("ul[class$=info_main]");
            // Remove ',' or '%' that could be in strings
            final double price = parseDouble(mainData.selectFirst(":root > li[class$=quote] > " +
                    "span.curr_price > span > span#quote_val").ownText().replaceAll("[^0-9.]+", ""));
            final Elements diffs = mainData.select(":root > li[class$=diff] > span > span");
            final double changePoint = parseDouble(diffs.get(0).ownText().replaceAll("[^0-9.-]+", ""));
            final double changePercent = parseDouble(diffs.get(1).ownText().replaceAll("[^0-9.-]+", ""));
            /** Not sure what HTML looks like when prevClose doesn't exist */
            final double prevClose = parseDouble(module2.selectFirst(":root > div > div[id$=divId] > div[class$=compare] > " +
                    "div[class$=compare_data] > ul > li:eq(1) > span.data_data").ownText().replaceAll("[^0-9.]+", ""));


            /* The 1 day data is gathered from the Market Watch multi stock page. The values
             * given on that page can be slightly off by minutes. For example, the Market
             * Watch multi stock page may correctly say that AAPL closed at $171.00, but the
             * value used for the Market Watch graph (which is where we're getting the 1 day
             * graph data) may say that the AAPL price at 4:00pm (time of close) is $171.21;
             * this issue occurs if AAPL's is changing at the end of the open trading period
             * and into the after hours period; for this example, it is likely that around
             * the time of 4:00pm, AAPL's price moved toward $171.21 (or at least increased
             * from its closing price). The largest problem that this small price difference
             * causes is that a user could be scrubbing through the 1 day graph, and notice
             * that the price at 4:00pm is different from the closing price - this also
             * causes the change values at 4:00pm to be nonzero, which is incorrect.
             * Fix this issue by manually setting the price at 4:00pm to the price at close
             * for stocks that have 1 day chart data up to at least 4:00pm.
             * Recall that in the 1 day chart, the prices are taken every 5 minutes, starting
             * at 9:30am - the 4:00pm price is at index 78.
             * Now that we have the price at close (price), fix the issue in the 1 day chart
             * data. */
            if (chartPrices_1day.size() >= 78) {
                chartPrices_1day.set(78, price);
            }


            final double ah_price;
            final double ah_changePoint;
            final double ah_changePercent;
            if (state == BasicStock.State.PREMARKET || state == BasicStock.State.AFTER_HOURS) {
                // Equivalent to checking if this stock instanceof StockWithAfterHoursValues
                // After hours values are live values, and regular values are values at close
                final Element subData = module2.selectFirst("ul[class$=info_sub] > li[class]");
                // Remove ',' or '%' that could be in strings
                ah_price = parseDouble(subData.selectFirst("span#ms_quote_val").ownText().replaceAll("[^0-9.]+", ""));
                final Elements ah_diffs = subData.select("span[id] > span");
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


            /* Values in the table (keyData1) can be either a real value (i.e. "310,540 -
             * 313,799"), or something else (i.e. empty string). It is difficult to find
             * examples of irregular values in this table. All the values are numeric and
             * positive, and some of the values could start with a decimal, so check if
             * the first char in the value is a digit or '.'. */
            final Elements keyData1 = module2.select("ul[class$=charts_info] > li > div > span.data_data");

            final String avgVolume;
            strBuff = keyData1.get(1).ownText();
            if (isDigitOrDec(strBuff.charAt(0))) {
                avgVolume = strBuff;
            } else {
                avgVolume = NA;
                missingStats.add(Stat.AVG_VOLUME);
            }

            final double todaysLow, todaysHigh;
            strBuff = keyData1.get(2).ownText();
            if (isDigitOrDec(strBuff.charAt(0))) {
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
            if (isDigitOrDec(strBuff.charAt(0))) {
                // " - " is between low and high values
                final String[] todaysRange = strBuff.split("\\s-\\s");
                fiftyTwoWeekLow = parseDouble(todaysRange[0].replaceAll("[^0-9.]+", ""));
                fiftyTwoWeekHigh = parseDouble(todaysRange[1].replaceAll("[^0-9.]+", ""));
            } else {
                fiftyTwoWeekLow = -1;
                fiftyTwoWeekHigh = -1;
                missingStats.add(Stat.FIFTY_TWO_WEEK_RANGE);
            }


            /* Values in the table (keyData2) can be either a real value (i.e. "366,452"),
             * a missing value (i.e. "N/A"), or something else (i.e. "BRK.A has not issued
             * dividends in more than 1 year"). All the values are numeric, and some values
             * could be negative, or start with a decimal, so check if the first char in
             * the value is a digit, '.', or '-'. */
            final Element module6 = contentFrame.selectFirst(":root > section[class$=section_2] > " +
                    "div#contentCol > div:eq(1) > div.zonedModule[data-module-id=6]");
            final Elements keyData2 = module6.select("div > div[class$=keystock_drawer] > " +
                    "div > ul > li > div > span.data_data");

            final double peRatio; // P/E ratio can be negative
            strBuff = keyData2.get(0).ownText();
            if (isDigitOrDecOrMinus(strBuff.charAt(0))) {
                peRatio = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
            } else {
                peRatio = -1;
                missingStats.add(Stat.PE_RATIO);
            }

            final double eps; // EPS can be negative
            strBuff = keyData2.get(1).ownText();
            if (isDigitOrDecOrMinus(strBuff.charAt(0))) {
                eps = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
            } else {
                eps = -1;
                missingStats.add(Stat.EPS);
            }

            final String marketCap;
            // Example market cap value: "1.4 T"
            strBuff = keyData2.get(2).ownText();
            if (isDigitOrDec(strBuff.charAt(0))) {
                marketCap = strBuff;
            } else {
                marketCap = NA;
                missingStats.add(Stat.MARKET_CAP);
            }

            final double yield; // Yield can be negative
            strBuff = keyData2.get(5).ownText();
            if (isDigitOrDecOrMinus(strBuff.charAt(0))) {
                yield = parseDouble(strBuff.replaceAll("[^0-9.-]+", ""));
            } else {
                yield = -1;
                missingStats.add(Stat.YIELD);
            }

            final String description;
            strBuff = contentFrame.selectFirst(":root > section[class$=section_2] > " +
                    "div#contentCol + div > div:eq(1) > div.zonedModule[data-module-id=11] > div > " +
                    "div[class$=data] > div[class$=description] > p.txtBody").ownText();
            // If there is no description, the element (p.txtBody) doesn't exist
            if (strBuff != null) {
                description = strBuff;
            } else {
                description = NA;
                missingStats.add(Stat.DESCRIPTION);
            }


            switch (state) {
                case PREMARKET:
                    ret = new PremarketStock(state, mticker, name, price, changePoint, changePercent,
                            ah_price, ah_changePoint, ah_changePercent, todaysLow,
                            todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap,
                            prevClose, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months,
                            chartDates_1year, chartDates_5years);
                    break;
                case OPEN:
                    ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent,
                            todaysLow, todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh,
                            marketCap, prevClose, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months, chartDates_1year, chartDates_5years);
                    break;
                case AFTER_HOURS:
                    ret = new AfterHoursStock(state, mticker, name, price, changePoint, changePercent,
                            ah_price, ah_changePoint, ah_changePercent, todaysLow,
                            todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh, marketCap,
                            prevClose, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months,
                            chartDates_1year, chartDates_5years);
                    break;
                case CLOSED:
                    ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent,
                            todaysLow, todaysHigh, fiftyTwoWeekLow, fiftyTwoWeekHigh,
                            marketCap, prevClose, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months,
                            chartDates_1year, chartDates_5years);
                    break;
                case ERROR:
                default:
                    return AdvancedStock.ERROR_AdvancedStock;
            }

            return ret;
        }

        @Override
        protected void onPostExecute(final AdvancedStock stock) {
            mcompletionListener.get().onDownloadIndividualStockTaskCompleted(stock, missingStats);
        }

        private boolean isDigitOrDec(final char c) {
            return Character.isDigit(c) || c == '.';
        }

        private boolean isDigitOrDecOrMinus(final char c) {
            return Character.isDigit(c) || c == '.' || c == '-';
        }

    }

}
