package cz.tacr.elza.service.output;

import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulTemplate;

public class OutputParams {

    private final ArrOutputDefinition definition;

    private final ArrChange change;

    private final ArrFundVersion fundVersion;

    private final List<ArrNodeOutput> outputNodes;

    private final List<ArrOutputItem> directItems;

    public OutputParams(ArrOutputDefinition definition,
                        ArrChange change,
                        ArrFundVersion fundVersion,
                        List<ArrNodeOutput> outputNodes,
                        List<ArrOutputItem> directItems) {
        // sanity check
        Validate.isTrue(definition.getFundId().equals(fundVersion.getFundId()));

        this.definition = definition;
        this.change = change;
        this.fundVersion = fundVersion;
        this.outputNodes = outputNodes;
        this.directItems = directItems;
    }

    /**
     * Shortcut method for output definition id.
     */
    public Integer getDefinitionId() {
        return definition.getOutputDefinitionId();
    }

    /**
     * Shortcut method for output definition template.
     */
    public RulTemplate getTemplate() {
        return definition.getTemplate();
    }

    public ArrOutputDefinition getDefinition() {
        return definition;
    }

    public ArrChange getChange() {
        return change;
    }

    /**
     * Shortcut method for fund version id.
     */
    public Integer getFundVersionId() {
        return fundVersion.getFundVersionId();
    }

    public ArrFundVersion getFundVersion() {
        return fundVersion;
    }

    public List<ArrNodeOutput> getOutputNodes() {
        return outputNodes;
    }

    public List<ArrOutputItem> getDirectItems() {
        return directItems;
    }
}
