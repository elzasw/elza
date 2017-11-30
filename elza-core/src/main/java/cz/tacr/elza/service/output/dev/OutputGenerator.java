package cz.tacr.elza.service.output.dev;

import cz.tacr.elza.domain.ArrOutputDefinition;

public interface OutputGenerator {

    void init(ArrOutputDefinition outputDefinition, int outputId);

    void generate();
}
