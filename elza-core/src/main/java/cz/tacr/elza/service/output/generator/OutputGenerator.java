package cz.tacr.elza.service.output.generator;

import java.io.Closeable;
import java.util.List;

import cz.tacr.elza.domain.ArrOutputResult;
import cz.tacr.elza.service.output.OutputParams;

public interface OutputGenerator extends Closeable {

    void init(OutputParams params);

    ArrOutputResult generate();
}
