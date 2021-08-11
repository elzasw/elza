package cz.tacr.elza.print.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.print.item.ItemSpec;

/**
 * Context of active formatting
 */
public class FormatContext {
    
    private static final Logger log = LoggerFactory.getLogger(FormatContext.class);

    /**
     * Helper class to store block on stack
     */
    static class Block {
        StringBuilder resultBuffer;
        String pendingSeparator;
        
        public Block(StringBuilder resultBuffer, String pendingSeparator) {
            this.resultBuffer = resultBuffer;
            this.pendingSeparator = pendingSeparator;
        }

        public StringBuilder getResultBuffer() {
            return resultBuffer;
        }

        public String getPendingSeparator() {
            return pendingSeparator;
        }
    }

    /**
     * Active separator
     */
    protected String itemSeparator = "; ";

    protected SpecTitleSource specTitleSource = SpecTitleSource.SHORTCUT;

    /**
     * Active separator for specifications
     */
    protected String specificationPostfix = " ";


    protected String specificationPrefix = "";

    /**
     * Separator between same specifications
     */
    protected String sameSpecItemSeparator = "; ";

    /**
     * Defines whether the specification is placed before or after the value
     */
    protected boolean specificationAfterValue = false;

    /**
     * Flag if begin block separator should be use always
     */
    protected boolean useBeginBlockSeparatorAlways = true;

    /**
     * Begin block separator
     */
    protected String beginBlockSeparator = "\n";

    /**
     * Flag if end block separator should be use always
     */
    protected boolean useEndBlockSeparatorAlways = true;

    /**
     * End block separator
     */
    protected String endBlockSeparator = "";

    /**
     * Buffer with result
     * 
     * Use appendResult to append data to the buffer.
     * Do not append data directly!
     */
    protected StringBuilder resultBuffer = new StringBuilder();

    /**
     * Conditional separator
     * 
     * This pending separator will be added to the resultBuffer if some 
     * other text will be added also.
     */
    protected String pendingSeparator;

    /**
     * Stack of opened blocks
     */
    protected List<Block> blockStack = new LinkedList<>();

    protected String titleSeparator;

    /**
     * Flag if item with specification should be grouped
     */
    protected boolean groupBySpec = true;

    protected Map<String, String> specNames = new HashMap<>();

    public String getItemSeparator() {
        return itemSeparator;
    }

    public void setItemSeparator(String separator) {
        this.itemSeparator = separator;
    }

    public void setSpecificationPrefix(String prefix) {
    	this.specificationPrefix = prefix;
	}
    
    public void setSpecificationSeparator(String specSeparator) {
        this.specificationPostfix = specSeparator;
    }
    
	public void setSpecificationAfterValue(boolean afterValue) {
		this.specificationAfterValue = afterValue;
	}

	public SpecTitleSource getSpecTitleSource() {
		return specTitleSource;
	}

	public void setSpecTitleSource(SpecTitleSource specTitleSource) {
		this.specTitleSource = specTitleSource;
	}
    
    public void setTitleSeparator(String titleSeparator) {
        this.titleSeparator = titleSeparator;
    }

    public String getTitleSeparator() {
        return titleSeparator;
    }

    public void setGroupFormat(final String sameSpecItemSeparator) {
        this.sameSpecItemSeparator = sameSpecItemSeparator;
    }

    public String getSameSpecItemSeparator() {
        return sameSpecItemSeparator;
    }

    public String getBeginBlockSeparator() {
        return beginBlockSeparator;
    }

    public void setBeginBlockSeparator(String beginBlockSeparator, boolean useBeginSeparatorAlways) {
        this.beginBlockSeparator = beginBlockSeparator;
        this.useBeginBlockSeparatorAlways = useBeginSeparatorAlways;
    }

    public String getEndBlockSeparator() {
        return endBlockSeparator;
    }

    public void setEndBlockSeparator(String endBlockSeparator, boolean useEndSeparatorAlways) {
        this.endBlockSeparator = endBlockSeparator;
        this.useEndBlockSeparatorAlways = useEndSeparatorAlways;
    }

    /**
     * Return formatted result
     *
     * Method is called after all formatting actions are finished
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
        log.debug("Append value, value: {}", value);

        if (StringUtils.isBlank(value)) {
            return;
        }

        appendResult(value);
        
        this.pendingSeparator = itemSeparator;
    }

    /**
     * Append specification and value to result
     *
     * @param value
     */
    public void appendSpecWithValue(String spec, String value) {
        appendSpecWithValues(spec, Collections.singletonList(value));
    }

    /**
     * Append specification and value to result
     *
     * @param spec
     * @param values
     */
    public void appendSpecWithValues(String spec, List<String> values) {
        log.debug("Append values, spec: {}, values: {}", spec, values);

        boolean hasPrefix = StringUtils.isNotBlank(specificationPrefix);
        String value = String.join(sameSpecItemSeparator, values);
        boolean hasValue = StringUtils.isNotBlank(value);
        boolean hasPostfix = StringUtils.isNotBlank(specificationPostfix);

        if (specificationAfterValue) {
            if (hasValue) {
                appendResult(value);
            }

            if (hasPrefix) {
                appendResult(specificationPrefix);
            }

            appendResult(spec);

            if (hasPostfix) {
                appendResult(specificationPostfix);
            }
        } else {
            if(hasPrefix) {
                appendResult(specificationPrefix);
            }

            appendResult(spec);

            if (hasPostfix) {
                // Append postfix when prefix or value exist
                // This condition allows to print:  spec: val | spec | (spec)
                if (hasPrefix || hasValue) {
                    appendResult(specificationPostfix);
                }
            }

            if(hasValue) {
                appendResult(value);
            }
        }
        
        this.pendingSeparator = itemSeparator;

    }

    /**
     * Begin new block
     */
    public void beginBlock() {
        Block block = new Block(resultBuffer, pendingSeparator);
        blockStack.add(block);
        
        // prepare data for inner block
        resultBuffer = new StringBuilder();
        pendingSeparator = null;
    }

    /**
     * End current block
     */
    public void endBlock() {
        // get result of the block
        String appendText = resultBuffer.toString();
        
        // Restore from stack
        Block restoredBlock = blockStack.remove(0); 
        resultBuffer = restoredBlock.getResultBuffer();
        pendingSeparator = restoredBlock.getPendingSeparator(); 

        if (appendText.length() > 0) {
            // begin block
            if (beginBlockSeparator != null && beginBlockSeparator.length() > 0) {
                // append begin block if required or some text is already in builder
                if(useBeginBlockSeparatorAlways || resultBuffer.length()>0)
                {
                    // reset pending separator -> block separator is used instead
                    pendingSeparator = null;
                    appendResult(beginBlockSeparator);
                }
            }

            appendResult(appendText);

            // end block
            if (endBlockSeparator != null) {
                if(useEndBlockSeparatorAlways) {
                    appendResult(endBlockSeparator);
                } else {                    
                    this.pendingSeparator = endBlockSeparator;
                }
            }
        }        
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

            // replace unexpected characters
            value = value.replace('\t', ' ');

            // append result
            resultBuffer.append(value);
        }
    }

    /**
     * Set group by specification flag
     * 
     * @param groupBySpec
     */
    public void setGroupBySpec(final boolean groupBySpec) {
        this.groupBySpec = groupBySpec;
    }

    public boolean getGroupBySpec() {
        return groupBySpec;
    }

    /**
     * Set specification name
     * 
     * @param code
     *            spec code
     * @param name
     *            spec name
     */
    public void setSpecName(String code, String name) {
        log.debug("Set spec name, code: {}, name: {}", code, name);
        if (name == null) {
            this.specNames.remove(code);
        } else {
            this.specNames.put(code, name);
        }

    }

    public String getSpecName(ItemSpec spec) {
        String specName = this.specNames.get(spec.getCode());
        if (specName != null) {
            return specName;
        }
        return specTitleSource.getValue(spec);
    }

}
