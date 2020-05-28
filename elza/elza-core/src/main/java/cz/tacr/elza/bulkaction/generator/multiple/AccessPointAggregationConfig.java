package cz.tacr.elza.bulkaction.generator.multiple;

import java.util.List;
import java.util.Map;

public class AccessPointAggregationConfig implements ActionConfig {

    private List<String> inputTypes;

    private String outputType;

    private String outputTypeApRef;

    private List<ApAggregationPartConfig> mappingPartValue;

    private List<ApAggregationItemConfig> mappingPartItem;

    private List<ApAggregationItemsConfig> mappingPartItems;

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(List<String> inputTypes) {
        this.inputTypes = inputTypes;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getOutputTypeApRef() {
        return outputTypeApRef;
    }

    public void setOutputTypeApRef(String outputTypeApRef) {
        this.outputTypeApRef = outputTypeApRef;
    }

    public List<ApAggregationPartConfig> getMappingPartValue() {
        return mappingPartValue;
    }

    public void setMappingPartValue(List<ApAggregationPartConfig> mappingPartValue) {
        this.mappingPartValue = mappingPartValue;
    }

    public List<ApAggregationItemConfig> getMappingPartItem() {
        return mappingPartItem;
    }

    public void setMappingPartItem(List<ApAggregationItemConfig> mappingPartItem) {
        this.mappingPartItem = mappingPartItem;
    }

    public List<ApAggregationItemsConfig> getMappingPartItems() {
        return mappingPartItems;
    }

    public void setMappingPartItems(List<ApAggregationItemsConfig> mappingPartItems) {
        this.mappingPartItems = mappingPartItems;
    }

    @Override
    public Class<? extends Action> getActionClass() {
        return AccessPointAggregationAction.class;
    }
}
