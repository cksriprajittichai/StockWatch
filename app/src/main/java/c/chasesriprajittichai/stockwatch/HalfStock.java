package c.chasesriprajittichai.stockwatch;

public class HalfStock {

    private String ticker;
    private double price;
    private double priceChangePercent;


    public HalfStock(String ticker, double price, double priceChangePercent) {
        this.ticker = ticker;
        this.price = price;
        this.priceChangePercent = priceChangePercent;
    }


    public String getTicker() {
        return ticker;
    }


    public double getPrice() {
        return price;
    }


    public double getPriceChangePercent() {
        return priceChangePercent;
    }

}
