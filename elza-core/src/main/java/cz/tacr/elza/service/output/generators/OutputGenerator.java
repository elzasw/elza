package cz.tacr.elza.service.output.generators;

import cz.tacr.elza.service.output.OutputParams;

public interface OutputGenerator {

    void init(OutputParams params);

    void generate();
}
