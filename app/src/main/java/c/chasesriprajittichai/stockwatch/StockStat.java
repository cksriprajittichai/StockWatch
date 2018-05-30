package c.chasesriprajittichai.stockwatch;

public class StockStat {

    private String name;
    private String value;


    public StockStat(String name, String value) {
        this.name = name;
        this.value = value;
    }


    @Override
    public String toString() {
        return name + ": " + value;
    }


    public String getName() {
        return name;
    }


    public String getValue() {
        return value;
    }
}
