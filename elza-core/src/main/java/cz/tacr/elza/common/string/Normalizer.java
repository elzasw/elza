package cz.tacr.elza.common.string;

import org.springframework.util.StringUtils;

public class Normalizer {

    public static String normalizeLineEnds(String text) {
        if (text == null) {
            return null;
        }

        // first, replace Windows line endings (\r\n) with Unix style (\n)
        String normalizedText = StringUtils.replace(text, "\r\n", "\n");

        // then, replace old Mac line endings (\r) with Unix style (\n)
        normalizedText = StringUtils.replace(normalizedText, "\r", "\n");
        return normalizedText;
    }	
}
