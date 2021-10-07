package cz.tacr.elza.utils;

public class MapyCzUtils {

    private static final String REGULAR_WEST_EAST = "^([0-9.]+[WE])$";
    private static final String REGULAR_SOUTH_NORTH = "^([0-9.]+[SN])$";

    /**
     * Dektekce, zda-li se jedná o souřadnice bodu z Mapy.cz.
     *
     * @param value vstupní souřadnice
     * @return true pokud se jedná o souřadnice bodu z Mapy.cz
     */
    public static boolean isFromMapyCz(final String value) {
        if (value == null) {
            return false;
        }
        String val = value.trim();
        String[] split = val.split(",");
        if (split.length != 2) {
            return false;
        }
        boolean x = false;
        boolean y = false;
        for (String substr : split) {
            substr = substr.trim().toUpperCase();
            if (!substr.matches(REGULAR_WEST_EAST)) {
                if (!substr.matches(REGULAR_SOUTH_NORTH)) {
                    return false;
                } else {
                    y = true;
                }
            } else {
                x = true;
            }
        }
        return x && y;
    }

    /**
     * Transformace souřadnic bodu z Mapy.cz do WKT.
     *
     * @param value vstupní souřadnice
     * @return WKT souřadnice bodu
     */
    public static String transformToWKT(final String value) {
        if (value == null) {
            throw getException(value);
        }
        String val = value.trim();
        String[] split = val.split(",");
        if (split.length != 2) {
            throw getException(value);
        }

        String x = null;
        String y = null;
        for (String substr : split) {
            substr = substr.trim().toUpperCase();
            if (substr.matches(REGULAR_WEST_EAST)) {
                x = substr;
            } else {
                if (substr.matches(REGULAR_SOUTH_NORTH)) {
                    y = substr;
                } else {
                    throw getException(value);
                }
            }
        }

        if (x == null || y == null) {
            throw getException(value);
        }

        return "POINT(" + toNumber(x) + " " + toNumber(y) + ")";
    }

    private static Double toNumber(final String coordinate) {
        double mul;
        if (coordinate.endsWith("W") || coordinate.endsWith("S")) {
            mul = -1;
        } else if (coordinate.endsWith("N") || coordinate.endsWith("E")) {
            mul = 1;
        } else {
            throw new IllegalArgumentException("Neplatný vstupní koodinát pro transformaci: " + coordinate);
        }
        String numberStr = coordinate.substring(0, coordinate.length() - 1);
        double number = Double.parseDouble(numberStr);
        return number * mul;
    }

    private static IllegalArgumentException getException(String value) {
        return new IllegalArgumentException("Neplatný vstup pro transformaci souřadnic do WKT: " + value);
    }

}
