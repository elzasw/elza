package cz.tacr.elza.service.output;

import java.util.ArrayList;
import java.util.List;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputTemplate;

/**
 * Output with sub entitities 
 *
 */
public class OutputData {

	private final ArrOutput output;
	
	private final List<ArrOutputTemplate> outputTemplates;

	public OutputData(final ArrOutput output, 
			final List<ArrOutputTemplate> outputTemplates) {
		this.output = output;
		this.outputTemplates = outputTemplates;
	}	

	public ArrOutput getOutput() {
		return output;
	}

	public ArrChange getCreateChange() {
		return output.getCreateChange();
	}
	
	List<ArrOutputTemplate> getOutputTemplates() {
		return outputTemplates;
	}
}
