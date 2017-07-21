package cz.tacr.elza.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility pro práci s textem.
 *
 * @since 18.07.2017
 */
public class StringUtils {

    public static final String REG_NAME = "(.*\\()([0-9]+)(\\))";

    public static void main(String[] args) {

        String name = "test";

        List<String> existsNames = new ArrayList<>();
        existsNames.add("testd");
        existsNames.add("sdfsdf");

        System.out.println(renameConflictName(name, existsNames));
    }

    public static String renameConflictName(final String name, final Iterable<String> existsNames) {
        String tmpName = name;

        boolean conflict = false;
        int i = 0;
        do {
            if (conflict) {
                i++;
                conflict = false;
                tmpName = includeNumber(tmpName, i);
            }
            for (String existsName : existsNames) {
                if (tmpName.equalsIgnoreCase(existsName)) {
                    conflict = true;
                    break;
                }
            }
        } while (conflict);
        return tmpName;
    }

    private static String includeNumber(final String name, final int i) {
        if (name.matches(REG_NAME)) {
            int tmpI = i;
            Matcher m = Pattern.compile(REG_NAME).matcher(name);
            if (m.find()) {
                tmpI = Integer.valueOf(m.group(2)) + 1;
            }
            return name.replaceAll(REG_NAME, "$1" + tmpI + "$3");
        }
        return name + "(" + i + ")";
    }

}
