package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Record;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// TODO - JavaDoc - Lebeda

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 29.6.16
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Component
@Configuration
public class OutputGeneratorWorkerFactory {


    // TODO - JavaDoc - Lebeda
    @Bean
    @Scope("prototype")
    public OutputGeneratorWorker getOutputGeneratorWorker() {
        return new OutputGeneratorWorker();
    }

    // TODO - JavaDoc - Lebeda
    @Bean
    @Scope("prototype")
    public Record getRecord(Output output, Node node, RegRecord record) {
        return new Record(output, node, record);
    }

    // TODO - JavaDoc - Lebeda
    @Bean
    @Scope("prototype")
    public Output getOutput(ArrOutput arrOutput) {
        return new Output(arrOutput);
    }
}
