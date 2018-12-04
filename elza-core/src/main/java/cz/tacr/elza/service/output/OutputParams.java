package cz.tacr.elza.service.output;

import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.RulTemplate;

public class OutputParams {

    private final ArrOutputDefinition definition;

    private final ArrChange change;

    private final ArrFundVersion fundVersion;

    private final List<Integer> outputNodeIds;

    private final List<ArrOutputItem> outputItems;

    private final Path templateDir;

    public OutputParams(ArrOutputDefinition definition,
                        ArrChange change,
                        ArrFundVersion fundVersion,
                        List<Integer> nodeIds,
                        List<ArrOutputItem> outputItems,
                        Path templateDir) {
        // sanity check
        Validate.isTrue(definition.getFundId().equals(fundVersion.getFundId()));

        this.definition = definition;
        this.change = change;
        this.fundVersion = fundVersion;
        this.outputNodeIds = nodeIds;
        this.outputItems = outputItems;
        this.templateDir = templateDir;
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

    public List<Integer> getOutputNodeIds() {
        return outputNodeIds;
    }

    public List<ArrOutputItem> getOutputItems() {
        return outputItems;
    }

    public Path getTemplateDir() {
        return templateDir;
    }
}
