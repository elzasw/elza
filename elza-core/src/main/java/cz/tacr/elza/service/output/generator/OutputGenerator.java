package cz.tacr.elza.service.output.generator;

import cz.tacr.elza.service.output.OutputParams;

public interface OutputGenerator {

    void init(OutputParams params);

    void generate();
}
