package c.chasesriprajittichai.stockwatch.stocks;

public interface Stock {

    enum State {ERROR, PREMARKET, OPEN, AFTER_HOURS, CLOSED}

    double getLivePrice();

    double getLiveChangePoint();

    double getLiveChangePercent();

    String getTicker();

    String getName();

    State getState();

    void setState(final State state);

    double getPrice();

    void setPrice(final double price);

    double getChangePoint();

    void setChangePoint(final double changePoint);

    double getChangePercent();

    void setChangePercent(final double changePercent);

}
