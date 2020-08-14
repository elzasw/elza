package cz.tacr.elza.service.output;

import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrOutputTemplate;
import cz.tacr.elza.domain.RulTemplate;

public class OutputParams {

    private final ArrOutput output;

    private final ArrChange change;

    private final ArrFundVersion fundVersion;

    private final List<Integer> outputNodeIds;

    private final List<ArrOutputItem> outputItems;

    private RulTemplate template;

    private Path templateDir;

    public OutputParams(ArrOutput output,
                        ArrChange change,
                        ArrFundVersion fundVersion,
                        List<Integer> nodeIds,
                        List<ArrOutputItem> outputItems) {
        // sanity check
        Validate.isTrue(output.getFundId().equals(fundVersion.getFundId()));

        this.output = output;
        this.change = change;
        this.fundVersion = fundVersion;
        this.outputNodeIds = nodeIds;
        this.outputItems = outputItems;
    }

    /**
     * Shortcut method for output definition id.
     */
    public Integer getOutputId() {
        return output.getOutputId();
    }

    /**
     * Shortcut method for output definition template.
     */
    public RulTemplate getTemplate() {
        return template;
    }

    public void setTemplate(RulTemplate template) {
        this.template = template;
    }

    public ArrOutput getOutput() {
        return output;
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

    public void setTemplateDir(Path templateDir) {
        this.templateDir = templateDir;
    }
}
