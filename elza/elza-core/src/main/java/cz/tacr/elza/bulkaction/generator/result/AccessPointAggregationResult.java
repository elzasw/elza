package cz.tacr.elza.bulkaction.generator.result;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.service.OutputItemConnector;

import java.util.List;

public class AccessPointAggregationResult extends ActionResult {

    private String outputType;

    private String outputTypeApRef;

    private List<ArrStructuredItem> dataItems;

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

    public List<ArrStructuredItem> getDataItems() {
        return dataItems;
    }

    public void setDataItems(List<ArrStructuredItem> dataItems) {
        this.dataItems = dataItems;
    }

    @Override
    public void createOutputItems(OutputItemConnector connector) {
        if (dataItems == null) {
            return;
        }
        ItemType rsit = connector.getItemTypeByCode(outputType);
        connector.addItems(dataItems, rsit);
    }
}
