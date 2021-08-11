package cz.tacr.elza.print.format;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlFormatContext extends FormatContext {

    private static final Logger log = LoggerFactory.getLogger(HtmlFormatContext.class);

    /**
     * Append value to result
     *
     * @param value
     */
    public void appendValue(String value) {
        log.debug("Append value, value: {}", value);

        if (StringUtils.isBlank(value)) {
            return;
        }

        appendResult(value);
        
        this.pendingSeparator = itemSeparator;
    }

    /**
     * Append text to the result
     * 
     * Other functions should not directly manipulate with resultBuffer
     * @param value
     */
    private void appendResult(String value) {
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
