package c.chasesriprajittichai.stockwatch.stocks;

public interface StockWithAfterHoursValues {
    /* Should be implemented by a stock that has a state that is not OPEN
     * or CLOSED, and has price, change point, and change percent values
     * at the the stock's last close. */

    double getAfterHoursPrice(); // Live price

    double getAfterHoursChangePoint(); // Change point during after hours

    double getAfterHoursChangePercent(); // Change percent during after hours

}
