package c.chasesriprajittichai.stockwatch;

import java.util.HashMap;
import java.util.Map;

import c.chasesriprajittichai.stockwatch.stocks.Stock;


public final class Util {

    // Prevent instantiation by making only constructor private
    private Util() {
    }


    /* Maps each State.toString -> State */
    static final Map<String, Stock.State> stringToStateMap = new HashMap<String, Stock.State>() {
        {
            put("ERROR", Stock.State.ERROR);
            put("PREMARKET", Stock.State.PREMARKET);
            put("OPEN", Stock.State.OPEN);
            put("AFTER_HOURS", Stock.State.AFTER_HOURS);
            put("CLOSED", Stock.State.CLOSED);
        }
    };


    static class Char {

        public static boolean isDigitOrDec(final char c) {
            return Character.isDigit(c) || c == '.';
        }

        public static boolean isDigitOrDecOrMinus(final char c) {
            return Character.isDigit(c) || c == '.' || c == '-';
        }

    }

}
