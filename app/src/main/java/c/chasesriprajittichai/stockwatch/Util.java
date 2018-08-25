package c.chasesriprajittichai.stockwatch;

import java.util.HashMap;
import java.util.Map;

import static c.chasesriprajittichai.stockwatch.stocks.Stock.State;


/**
 * A class of static objects, classes, and methods that are useful in various
 * places in the project. Everything in this class doesn't fit in well where
 * they are used, so they have been put into this class.
 */
public final class Util {

    /**
     * Prevent instantiation by making this private. Do not allow use of default
     * constructor.
     */
    private Util() {
    }


    /**
     * Maps each {@link State#toString()} -> {@link State}. Used often because
     * stock information, including it's State, is constantly being written to
     * and read from preferences.
     */
    static final Map<String, State> stringToStateMap = new HashMap<String, State>() {
        {
            for (final State s : State.values()) {
                put(s.toString(), s);
            }
        }
    };


    static class Char {

        /**
         * @param c Char to evaluate
         * @return True if {@param c} is a digit or a '.'.
         */
        public static boolean isDigitOrDec(final char c) {
            return Character.isDigit(c) || c == '.';
        }

        /**
         * @param c Char to evaluate
         * @return True if {@param c} is a digit, a '.', or a '-'.
         */
        public static boolean isDigitOrDecOrMinus(final char c) {
            return Character.isDigit(c) || c == '.' || c == '-';
        }

    }

}
