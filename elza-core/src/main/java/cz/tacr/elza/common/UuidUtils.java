package cz.tacr.elza.common;

import java.util.regex.Pattern;

public class UuidUtils {

    public static final Pattern UUID_REGEX_PATTERN = Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    public static boolean isUUID(String uuid) {
        return UUID_REGEX_PATTERN.matcher(uuid).matches();
    }
}
