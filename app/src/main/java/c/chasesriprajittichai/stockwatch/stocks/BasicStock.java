package c.chasesriprajittichai.stockwatch.stocks;

public interface BasicStock {

    enum State {ERROR, PREMARKET, OPEN, AFTER_HOURS, CLOSED}

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

    double getLivePrice();

    double getLiveChangePoint();

    double getLiveChangePercent();

    double getNetChangePoint();

    double getNetChangePercent();

}
