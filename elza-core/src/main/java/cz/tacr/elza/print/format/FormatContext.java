package cz.tacr.elza.print.format;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Context of active formatting
 */
public class FormatContext {
    
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
    private String itemSeparator = "; ";

    private SpecTitleSource specTitleSource = SpecTitleSource.SHORTCUT;
    /**
     * Active separator for specifications
     */
    private String specificationPostfix = " ";
    
    
    private String specificationPrefix = "";
    
    /**
     * Defines whether the specification is placed before or after the value
     */
    private boolean specificationAfterValue = false;
    
    /**
     * Flag if begin block separator should be use always
     */
    private boolean useBeginBlockSeparatorAlways = true;

    /**
     * Begin block separator
     */
    private String beginBlockSeparator = "\n";

    /**
     * Flag if end block separator should be use always
     */
    private boolean useEndBlockSeparatorAlways = true;

    /**
     * End block separator
     */
    private String endBlockSeparator = "";

    /**
     * Buffer with result
     * 
     * Use appendResult to append data to the buffer.
     * Do not append data directly!
     */
    private StringBuilder resultBuffer = new StringBuilder();
    
    /**
     * Conditional separator
     * 
     * This pending separator will be added to the resultBuffer if some 
     * other text will be added also.
     */
    private String pendingSeparator;

    /**
     * Stack of opened blocks
     */
    private List<Block> blockStack = new LinkedList<>();

    private String titleSeparator;

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
    	boolean hasPrefix = StringUtils.isNotBlank(specificationPrefix);
    	boolean hasValue = StringUtils.isNotBlank(value);
    	boolean hasPostfix = StringUtils.isNotBlank(specificationPostfix);
    	
    	if (specificationAfterValue) {
    		if(hasValue) {
    			appendResult(value);
    		}
    		
            if(hasPrefix) {
				appendResult(specificationPrefix);
			}
			
			appendResult(spec);
			
			if(hasPostfix) {
				appendResult(specificationPostfix);
			}
    	}		
    	else{
        	if(hasPrefix) {
				appendResult(specificationPrefix);
			}
			
			appendResult(spec);
			
			if(hasPostfix) {
				// Append postfix when prefix or value exist
				// This condition allows to print:  spec: val | spec | (spec)
				if(hasPrefix || hasValue) {
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
     * @param result
     */
    private void appendResult(String result) {
        if(StringUtils.isNotEmpty(result)) {
            // append pending separator
            if(pendingSeparator!=null) {
                resultBuffer.append(pendingSeparator);
                pendingSeparator = null;
            }
            
            // replace unexpected characters
            result = result.replace('\t', ' ');

            // append result
            resultBuffer.append(result);
        }
    }
}
