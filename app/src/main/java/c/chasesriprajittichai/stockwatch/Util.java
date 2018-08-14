package c.chasesriprajittichai.stockwatch;

public final class Util {

    // Prevent instantiation by making only constructor private
    private Util() {
    }


    static class Char {

        public static boolean isDigitOrDec(final char c) {
            return Character.isDigit(c) || c == '.';
        }

        public static boolean isDigitOrDecOrMinus(final char c) {
            return Character.isDigit(c) || c == '.' || c == '-';
        }

    }

}
