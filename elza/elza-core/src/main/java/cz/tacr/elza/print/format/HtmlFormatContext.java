package cz.tacr.elza.print.format;

import org.apache.commons.lang3.StringUtils;

public class HtmlFormatContext extends FormatContext {

    /**
     * Append text to the result
     * @param value
     */
    @Override
    protected String preprocessText(String value) {
    	// replace angle brackets
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");

        // replace line break
        value = value.replace("\n", "<br>");
        value = value.replace("\r", "");
        value = value.replace('\t', ' ');

        // append result
        return value;
    }
    
    /**
     * Append block result
     * @param appendText
     */
    @Override
    protected void appendBlockResult(String appendText) {
    	// Text is already preprocessed
    	// we can append raw value
        if (StringUtils.isEmpty(appendText)) {
        	return;
        }
        
        // append pending separator
        if (StringUtils.isNotEmpty(pendingSeparator)) {
        	pendingSeparator = preprocessText(pendingSeparator);
        	resultBuffer.append(pendingSeparator);
            pendingSeparator = null;
        }
        
        // append result
        resultBuffer.append(appendText);
	}
}
