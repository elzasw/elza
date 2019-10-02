package cz.tacr.elza.service.output.generator;

import java.io.Closeable;

import cz.tacr.elza.service.output.OutputParams;

public interface OutputGenerator
        extends Closeable {

    void init(OutputParams params);

    void generate();
}
