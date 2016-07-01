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

/**
 * Factory metoda pro vytváření objektů s dependency injections.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 29.6.16
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Component
@Configuration
public class OutputGeneratorWorkerFactory {


    /**
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public OutputGeneratorWorker getOutputGeneratorWorker() {
        return new OutputGeneratorWorker();
    }

    /**
     * @param output output ke kterému je record zařazen
     * @param node node ke kterému je record zařazen, pokud je null, je zařazen přímo k outputu
     * @param record zdrojový record
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public Record getRecord(Output output, Node node, RegRecord record) {
        return new Record(output, node, record);
    }

    /**
     * @param arrOutput zdrojová deinice výstupu
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public Output getOutput(ArrOutput arrOutput) {
        return new Output(arrOutput);
    }
}
