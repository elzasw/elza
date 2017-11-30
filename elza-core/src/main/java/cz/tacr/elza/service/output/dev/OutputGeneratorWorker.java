package cz.tacr.elza.service.output.dev;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrOutputDefinition;

// - inicializovat generator ?
// - pripravit transakci
// - spustit prirazeny generator
// - aktualizace stavu vystupu

@Component
@Scope("prototype")
public class OutputGeneratorWorker implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final int outputDefinitionId;

    private OutputGenerator outputGenerator;

    public void init(OutputGenerator outputGenerator) {
        Validate.isTrue(outputGenerator == null);

        this.outputGenerator = outputGenerator;
        updateState(outputDefinitionId, ArrOutputDefinition.OutputState.GENERATING);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            template -> {
                generate();
            }
        } catch (Throwable t) {
            handleException(t);
        }
    }

    private void handleException(Throwable t) {
        template -> {
            ...
        }
    }
}
