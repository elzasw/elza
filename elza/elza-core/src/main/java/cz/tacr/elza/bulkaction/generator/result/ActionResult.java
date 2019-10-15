package cz.tacr.elza.bulkaction.generator.result;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.service.OutputItemConnector;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class ActionResult {

    /**
     * Result can implements conversion in to output items. Default implementation is empty.
     */
    public void createOutputItems(OutputItemConnector connector) {
    }
}
