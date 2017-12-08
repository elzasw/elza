package cz.tacr.elza.service.output.dev.generators;

import cz.tacr.elza.service.output.dev.OutputParams;

public interface OutputGenerator {

    void init(OutputParams params);

    void generate();
}
