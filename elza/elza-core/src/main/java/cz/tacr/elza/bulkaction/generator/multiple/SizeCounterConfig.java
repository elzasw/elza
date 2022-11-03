package cz.tacr.elza.bulkaction.generator.multiple;

public class SizeCounterConfig implements ActionConfig {

    /**
     * Exclude condition
     */
    WhenConditionConfig excludeWhen;

    String storageType;

    String sizeType;
    String outputType;

    String outputPostfix;
    String outputPostfixMissing;

    @Override
    public Class<? extends Action> getActionClass() {
        return SizeCounterAction.class;
    }

    public WhenConditionConfig getExcludeWhen() {
        return excludeWhen;
    }

    public void setExcludeWhen(WhenConditionConfig excludeWhen) {
        this.excludeWhen = excludeWhen;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getSizeType() {
        return sizeType;
    }

    public void setSizeType(String sizeType) {
        this.sizeType = sizeType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getOutputPostfix() {
        return outputPostfix;
    }

    public void setOutputPostfix(String outputPostfix) {
        this.outputPostfix = outputPostfix;
    }

    public String getOutputPostfixMissing() {
        return outputPostfixMissing;
    }

    public void setOutputPostfixMissing(String outputPostfixMissing) {
        this.outputPostfixMissing = outputPostfixMissing;
    }
}
