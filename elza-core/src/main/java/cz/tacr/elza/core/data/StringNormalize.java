package cz.tacr.elza.core.data;

public class StringNormalize {

    // trim and replace unprinted characters with spaces and delete double space
    public static String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sb.append(c > 0x1f ? c : " ");
        }
        return sb.toString().replaceAll(" +", " ");
    }

    // trim and replace unprinted characters (exclude 0x0D and 0x0A) with spaces
    public static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sb.append(c > 0x1f || c == 0x0D || c == 0x0A ? c : " ");
        }
        return sb.toString();
    }
}