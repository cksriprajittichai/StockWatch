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
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBetween;


public final class IndividualStockActivity extends AppCompatActivity implements
        DownloadIndividualStockTaskListener, HorizontalPicker.OnItemSelected, CustomScrubGestureDetector.ScrubIndexListener {

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
    @BindView(R.id.textView_beta_individual) TextView mbeta;
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
    public void onDownloadIndividualStockTaskCompleted(final AdvancedStock stock, final Set<String> missingStats) {
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

        if (!missingStats.contains("Day Range")) {
            mtodaysLow.setText(getString(R.string.double2dec, mstock.getTodaysLow()));
            mtodaysHigh.setText(getString(R.string.double2dec, mstock.getTodaysHigh()));
        } else {
            mtodaysLow.setText("N/A");
            mtodaysHigh.setText("N/A");
        }
        if (!missingStats.contains("52 Week Range")) {
            mfiftyTwoWeekLow.setText(getString(R.string.double2dec, mstock.getFiftyTwoWeekLow()));
            mfiftyTwoWeekHigh.setText(getString(R.string.double2dec, mstock.getFiftyTwoWeekHigh()));
        } else {
            mfiftyTwoWeekLow.setText("N/A");
            mfiftyTwoWeekHigh.setText("N/A");
        }
        if (!missingStats.contains("Market Cap")) {
            mmarketCap.setText(getString(R.string.string, mstock.getMarketCap()));
        } else {
            mmarketCap.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains("Beta")) {
            mbeta.setText(getString(R.string.double2dec, mstock.getBeta()));
        } else {
            mbeta.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains("P/E Ratio")) {
            mpeRatio.setText(getString(R.string.double2dec, mstock.getPeRatio()));
        } else {
            mpeRatio.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains("EPS")) {
            meps.setText(getString(R.string.double2dec, mstock.getEps()));
        } else {
            meps.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains("Yield")) {
            myield.setText(getString(R.string.double2dec, mstock.getYield()));
        } else {
            myield.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains("Average Volume")) {
            mavgVolume.setText(getString(R.string.string, mstock.getAverageVolume()));
        } else {
            mavgVolume.setText(getString(R.string.string, "N/A"));
        }
        if (!missingStats.contains("Description")) {
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
            final ArrayList<String> tickerList = new ArrayList<>(Arrays.asList(tickerArr));
            final ArrayList<String> dataList = new ArrayList<>(Arrays.asList(
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
        private final HashSet<String> missingStats = new HashSet<>();

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

            String name = mticker; // Init as ticker, should change to company name from JSON
            /* Some stocks have no chart data. If this is the case, chart_prices will be an
             * empty array list. */
            final ArrayList<Double> chartPrices_1day = new ArrayList<>();

            final Element multiQuoteRoot = multiDoc.selectFirst("html > body > div#blanket > div[class*=multi] > div#maincontent > " +
                    "div[class^=block multiquote] > div[class^=quotedisplay] > div[class^=section activeQuote bgQuote]");
            final Element javascriptElmnt = multiQuoteRoot.selectFirst(":root > div.intradaychart > script[type=text/javascript]");
            final String jsonString = substringBetween(javascriptElmnt.toString(), "var chartData = [", "];");

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
                individualDoc = Jsoup.connect("https://www.marketwatch.com/investing/stock/" + mticker).get();
            } catch (final IOException ioe) {
                Log.e("IOException", ioe.getLocalizedMessage());
                return AdvancedStock.ERROR_AdvancedStock;
            }

            /* Get chart data for periods greater than one day from Wall Street Journal. */
            // Certain values from Market Watch's individual-stock-site are needed in the WSJ url
            final String wsj_instrumentType = individualDoc.selectFirst(":root > head > meta[name=instrumentType]").attr("content");
            final String wsj_exchange = individualDoc.selectFirst(":root > head > meta[name=exchangeIso]").attr("content");
            final String wsj_exchangeCountry = individualDoc.selectFirst(":root > head > meta[name=exchangeCountry]").attr("content");

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
                    mticker, mticker, wsj_exchangeCountry, wsj_exchange, wsj_instrumentType,
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

                final Elements rowElmnts = fiveYearDoc.select(":root > body > div > div#historical_data_table > div > table > tbody > tr");
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
            final Element quoteRoot = individualDoc.selectFirst(":root > body[role=document] > div[data-symbol=" + mticker + "]");
            final Element intraday = quoteRoot.selectFirst(":root > div.content-region.region--fixed > div.template.template--aside > div > div.element.element--intraday");
            final Element intradayData = intraday.selectFirst(":root > div.intraday__data");

            final Element icon = intraday.selectFirst(":root > small[class~=intraday__status status--(before|open|after|closed)] > i[class^=icon]");
            final String stateStr = icon.nextSibling().toString();
            final BasicStock.State state;
            switch (stateStr.toLowerCase(Locale.US)) {
                case "before the bell": // Multiple stock view site uses this
                case "premarket": // Individual stock site uses this
                    state = PREMARKET;
                    break;
                case "countdown to close":
                case "open": // Individual stock site uses this
                    state = OPEN;
                    break;
                case "after hours":
                    state = AFTER_HOURS;
                    break;
                case "market closed": // Multiple stock view site uses this
                case "closed":
                    state = CLOSED;
                    break;
                default:
                    state = ERROR;
                    break;
            }

            final Element priceElmnt, changePointElmnt, changePercentElmnt, close_priceElmnt, close_changePointElmnt, close_changePercentElmnt;
            final Elements close_tableCells;
            final double price, changePoint, changePercent, afterHoursPrice, afterHoursChangePoint, afterHoursChangePercent;
            // Parsing of certain data varies depending on the state of the stock
            switch (state) {
                case PREMARKET: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > bg-quote[class^=value]");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q > bg-quote[field=change]");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q > bg-quote[field=percentchange]");

                    close_tableCells = intraday.select(":root > div.intraday__close > table > tbody > tr.table__row > td[class^=table__cell]");
                    close_priceElmnt = close_tableCells.get(0);
                    close_changePointElmnt = close_tableCells.get(1);
                    close_changePercentElmnt = close_tableCells.get(2);

                    // Remove ',' or '%' that could be in strings
                    afterHoursPrice = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    afterHoursChangePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    afterHoursChangePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    // price, changePoint, and changePercent represent the values at close
                    price = parseDouble(close_priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(close_changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(close_changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    break;
                }
                case OPEN: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > bg-quote[class^=value]");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q > bg-quote[field=change]");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q > bg-quote[field=percentchange]");

                    // Remove ',' or '%' that could be in strings
                    price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                    // Initialize unused data
                    afterHoursPrice = 0;
                    afterHoursChangePoint = 0;
                    afterHoursChangePercent = 0;
                    break;
                }
                case AFTER_HOURS: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > bg-quote[class^=value]");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q > bg-quote[field=change]");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q > bg-quote[field=percentchange]");

                    close_tableCells = intraday.select(":root > div.intraday__close > table > tbody > tr.table__row > td");
                    close_priceElmnt = close_tableCells.get(0);
                    close_changePointElmnt = close_tableCells.get(1);
                    close_changePercentElmnt = close_tableCells.get(2);

                    // Remove ',' or '%' that could be in strings
                    afterHoursPrice = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    afterHoursChangePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    afterHoursChangePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    // price, changePoint, and changePercent represent the values at close
                    price = parseDouble(close_priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(close_changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(close_changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));
                    break;
                }
                case CLOSED: {
                    priceElmnt = intradayData.selectFirst(":root > h3.intraday__price > span.value");
                    changePointElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--point--q");
                    changePercentElmnt = intradayData.selectFirst(":root > bg-quote[class^=intraday__change] > span.change--percent--q");

                    // Remove ',' or '%' that could be in strings
                    price = parseDouble(priceElmnt.text().replaceAll("[^0-9.]+", ""));
                    changePoint = parseDouble(changePointElmnt.text().replaceAll("[^0-9.-]+", ""));
                    changePercent = parseDouble(changePercentElmnt.text().replaceAll("[^0-9.-]+", ""));

                    // Initialize unused data
                    afterHoursPrice = 0;
                    afterHoursChangePoint = 0;
                    afterHoursChangePercent = 0;
                    break;
                }
                case ERROR:
                default:
                    return AdvancedStock.ERROR_AdvancedStock;
            }


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


            final Element regionPrimary = quoteRoot.selectFirst(":root > div.content-region.region--primary");
            final Element keyDataElmnt = regionPrimary.selectFirst(":root > div.template.template--aside > div.column.column--full.left.clearfix > div.element.element--list > ul.list.list--kv.list--col50");
            final Elements keyDataItemElmnts = keyDataElmnt.select(":root > li");

            final Element dayRangeElmnt = keyDataItemElmnts.get(1);
            final Element fiftyTwoWeekRangeElmnt = keyDataItemElmnts.get(2);
            final Element marketCapElmnt = keyDataItemElmnts.get(3);
            final Element betaElmnt = keyDataItemElmnts.get(6);
            final Element peRatioElmnt = keyDataItemElmnts.get(8);
            final Element epsElmnt = keyDataItemElmnts.get(9);
            final Element yieldElmnt = keyDataItemElmnts.get(10);
            final Element avgVolumeElmnt = keyDataItemElmnts.get(15);

            final double dayRangeLow, dayRangeHigh;
            if (!dayRangeElmnt.text().contains("n/a") && !dayRangeElmnt.text().equalsIgnoreCase("Day Range")) {
                dayRangeLow = parseDouble(substringBefore(dayRangeElmnt.text(), "-").replaceAll("[^0-9.]+", ""));
                dayRangeHigh = parseDouble(substringAfter(dayRangeElmnt.text(), "-").replaceAll("[^0-9.]+", ""));
            } else {
                dayRangeLow = -1;
                dayRangeHigh = -1;
                missingStats.add("Day Range");
            }
            final double fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh;
            if (!fiftyTwoWeekRangeElmnt.text().contains("n/a") && !fiftyTwoWeekRangeElmnt.text().equalsIgnoreCase("52 Week Range")) {
                fiftyTwoWeekRangeLow = parseDouble(substringBetween(fiftyTwoWeekRangeElmnt.text(), "52 Week Range", "-").replaceAll("[^0-9.]+", ""));
                fiftyTwoWeekRangeHigh = parseDouble(substringAfter(fiftyTwoWeekRangeElmnt.text(), "-").replaceAll("[^0-9.]+", ""));
            } else {
                fiftyTwoWeekRangeLow = -1;
                fiftyTwoWeekRangeHigh = -1;
                missingStats.add("52 Week Range");
            }
            final String marketCap;
            if (!marketCapElmnt.text().contains("n/a") && !marketCapElmnt.text().equalsIgnoreCase("Market Cap")) {
                marketCap = substringAfter(marketCapElmnt.text(), "Market Cap $").trim();
            } else {
                marketCap = "";
                missingStats.add("Market Cap");
            }
            final double beta;
            if (!betaElmnt.text().contains("n/a") && !betaElmnt.text().equalsIgnoreCase("Beta")) {
                beta = parseDouble(betaElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                beta = -1;
                missingStats.add("Beta");
            }
            final double peRatio;
            if (!peRatioElmnt.text().contains("n/a") && !peRatioElmnt.text().equalsIgnoreCase("P/E Ratio")) {
                peRatio = parseDouble(peRatioElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                peRatio = -1;
                missingStats.add("P/E Ratio");
            }
            final double eps;
            if (!epsElmnt.text().contains("n/a") && !epsElmnt.text().equalsIgnoreCase("EPS")) {
                eps = parseDouble(epsElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                eps = -1;
                missingStats.add("EPS");
            }
            final double yield;
            if (!yieldElmnt.text().contains("n/a") && !yieldElmnt.text().equalsIgnoreCase("Yield")) {
                yield = parseDouble(yieldElmnt.text().replaceAll("[^0-9.]+", ""));
            } else {
                yield = -1;
                missingStats.add("Yield");
            }
            final String avgVolume;
            if (!avgVolumeElmnt.text().contains("n/a") && !avgVolumeElmnt.text().equalsIgnoreCase("Average Volume")) {
                avgVolume = substringAfter(avgVolumeElmnt.text(), "Average Volume").trim();
            } else {
                avgVolume = "";
                missingStats.add("Average Volume");
            }


            /* Some stocks don't have a description. If there is no description, then
             * descriptionElmnt does not exist. */
            final Element descriptionElmnt = regionPrimary.selectFirst(":root > div.template.template--primary > div.column.column--full > div[class*=description] > p.description__text");
            final String description;
            if (descriptionElmnt != null) {
                /* There is a button at the bottom of the description that is a link to the profile
                 * tab of the individual stock site. The button's title shows up as part of the
                 * text - remove it. */
                description = substringBefore(descriptionElmnt.text(), " (See Full Profile)");
            } else {
                description = "";
                missingStats.add("Description");
            }


            switch (state) {
                case PREMARKET:
                    ret = new PremarketStock(state, mticker, name, price, changePoint, changePercent,
                            afterHoursPrice, afterHoursChangePoint, afterHoursChangePercent, dayRangeLow,
                            dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh, marketCap,
                            beta, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months,
                            chartDates_1year, chartDates_5years);
                    break;
                case OPEN:
                    ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent,
                            dayRangeLow, dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh,
                            marketCap, beta, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months, chartDates_1year, chartDates_5years);
                    break;
                case AFTER_HOURS:
                    ret = new AfterHoursStock(state, mticker, name, price, changePoint, changePercent,
                            afterHoursPrice, afterHoursChangePoint, afterHoursChangePercent, dayRangeLow,
                            dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh, marketCap,
                            beta, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
                            chartPrices_2weeks, chartPrices_1month, chartPrices_3months, chartPrices_1year,
                            chartPrices_5years, chartDates_2weeks, chartDates_1month, chartDates_3months,
                            chartDates_1year, chartDates_5years);
                    break;
                case CLOSED:
                    ret = new AdvancedStock(state, mticker, name, price, changePoint, changePercent,
                            dayRangeLow, dayRangeHigh, fiftyTwoWeekRangeLow, fiftyTwoWeekRangeHigh,
                            marketCap, beta, peRatio, eps, yield, avgVolume, description, chartPrices_1day,
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

    }

}
