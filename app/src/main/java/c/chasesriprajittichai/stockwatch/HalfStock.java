package c.chasesriprajittichai.stockwatch;

public class HalfStock {

    private String ticker;
    private double price;
    private double priceChange;
    private double priceChangePercent;


    public HalfStock(String ticker, double price, double priceChange, double priceChangePercent) {
        this.ticker = ticker;
        this.price = price;
        this.priceChange = priceChange;
        this.priceChangePercent = priceChangePercent;
    }


    public String getTicker() {
        return ticker;
    }


    public double getPrice() {
        return price;
    }


    public double getPriceChange() {
        return priceChange;
    }


    public double getPriceChangePercent() {
        return priceChangePercent;
    }

}
