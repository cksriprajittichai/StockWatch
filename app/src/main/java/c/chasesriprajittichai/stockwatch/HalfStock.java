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


    public void setTicker(String ticker) {
        this.ticker = ticker;
    }


    public double getPrice() {
        return price;
    }


    public void setPrice(double price) {
        this.price = price;
    }


    public double getPriceChange() {
        return priceChange;
    }


    public void setPriceChange(double priceChange) {
        this.priceChange = priceChange;
    }


    public double getPriceChangePercent() {
        return priceChangePercent;
    }
    

    public void setPriceChangePercent(double priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

}
