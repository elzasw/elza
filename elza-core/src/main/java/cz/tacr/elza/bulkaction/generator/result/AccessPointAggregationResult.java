package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.service.OutputItemConnector;

import java.util.List;

public class AccessPointAggregationResult extends ActionResult {

    private String outputType;

    private List<AccessPointAggregationStructResult> structs;

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public List<AccessPointAggregationStructResult> getStructs() {
        return structs;
    }

    public void setStructs(List<AccessPointAggregationStructResult> structs) {
        this.structs = structs;
    }

    @Override
    public void createOutputItems(OutputItemConnector connector) {
        if (structs == null) {
            return;
        }
        ItemType outputType = connector.getItemTypeByCode(this.outputType);
        StructType structuredType = connector.getStructuredTypeByCode(outputType.getEntity().getStructuredType().getCode());
        for (AccessPointAggregationStructResult struct : structs) {
            connector.addStructuredItem(outputType, structuredType, struct.getItems());
        }
    }
}
