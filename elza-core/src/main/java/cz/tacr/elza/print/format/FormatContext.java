package cz.tacr.elza.print.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Context of active formatting
 */
public class FormatContext {

    /**
     * Active separator
     */
    private String itemSeparator = "; ";

    /**
     * Active separator for specifications
     */
    private String specificationSeparator = " ";

    /**
     * Begin block separator
     */
    private String beginBlockSeparator = "\n";

    /**
     * End block separator
     */
    private String endBlockSeparator = "";

    /**
     * Buffer with result
     */
    private StringBuilder resultBuffer = new StringBuilder();

    /**
     * Stack of opened blocks
     */
    private List<StringBuilder> blockStack = new LinkedList<>();

    private String titleSeparator;

    public String getItemSeparator() {
        return itemSeparator;
    }

    public void setItemSeparator(String separator) {
        this.itemSeparator = separator;
    }

    public void setSpecificationSeparator(String specSeparator) {
        this.specificationSeparator = specSeparator;
    }

    public String getSpecificationSeparator() {
        return specificationSeparator;
    }

    public void setTitleSeparator(String titleSeparator) {
        this.titleSeparator = titleSeparator;
    }

    public String getTitleSeparator() {
        return titleSeparator;
    }

    public String getBeginBlockSeparator() {
        return beginBlockSeparator;
    }

    public void setBeginBlockSeparator(String beginBlockSeparator) {
        this.beginBlockSeparator = beginBlockSeparator;
    }

    public String getEndBlockSeparator() {
        return endBlockSeparator;
    }

    public void setEndBlockSeparator(String endBlockSeparator) {
        this.endBlockSeparator = endBlockSeparator;
    }

    /**
     * Return formatted result
     *
     * @return
     */
    public String getResult() {
        if (blockStack.size() > 0) {
            throw new IllegalStateException("There are unclosed blocks. Each block have to be properly closed");
        }
        return resultBuffer.toString();
    }

    /**
     * Append value to result
     *
     * @param value
     */
    public void appendValue(String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (resultBuffer.length() > 0) {
            resultBuffer.append(itemSeparator);
        }

        resultBuffer.append(value);
    }

    /**
     * Append specificationa and value to result
     *
     * @param value
     */
    public void appendSpecWithValue(String spec, String value) {
        if (resultBuffer.length() > 0) {
            resultBuffer.append(itemSeparator);
        }
        resultBuffer.append(spec);
        if (StringUtils.isNoneBlank(value)) {
            resultBuffer.append(specificationSeparator);
            resultBuffer.append(value);
        }
    }

    /**
     * Begin new block
     */
    public void beginBlock() {
        blockStack.add(resultBuffer);
        resultBuffer = new StringBuilder();
    }

    /**
     * End current block
     */
    public void endBlock() {
        // Get from stack
        StringBuilder sb = blockStack.remove(0);

        if (resultBuffer.length() > 0) {
            // begin block
            if (beginBlockSeparator != null) {
                sb.append(beginBlockSeparator);
            }

            sb.append(resultBuffer.toString());

            // end block
            if (endBlockSeparator != null) {
                sb.append(endBlockSeparator);
            }
        }
        resultBuffer = sb;
    }
}
