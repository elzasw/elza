package cz.tacr.elza.print.format;

import org.apache.commons.lang3.StringUtils;

public class HtmlFormatContext extends FormatContext {

    /**
     * Append text to the result
     * 
     * Other functions should not directly manipulate with resultBuffer
     * @param value
     */
    @Override
    protected void appendResult(String value) {
        if (StringUtils.isNotEmpty(value)) {
            // append pending separator
            if (pendingSeparator != null) {
                resultBuffer.append(pendingSeparator);
                pendingSeparator = null;
            }

            // replace angle brackets
            value = value.replace("<", "&lt;");
            value = value.replace(">", "&gt;");

            // append result
            resultBuffer.append(value);
        }
    }
}
