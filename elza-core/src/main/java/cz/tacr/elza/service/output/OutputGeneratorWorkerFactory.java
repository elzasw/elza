package cz.tacr.elza.service.output;

import cz.tacr.elza.api.RulTemplate.Engine;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Record;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Factory metoda pro vytváření objektů {@link OutputGeneratorWorkerJasper} a objektů vytvářených při jeho běhu s dependency injections.
 *
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 29.6.16
 */
@SuppressWarnings("SpringJavaAutowiringInspection")
@Component
@Configuration
class OutputGeneratorWorkerFactory {


    /**
     * @return vytvořený objekt s provedeným dependency injections
     * @param rulTemplateEngine
     */
    @Bean
    @Scope("prototype")
    public OutputGeneratorWorkerAbstract getOutputGeneratorWorker(final Engine rulTemplateEngine) {
        // skutečné vytvoření workeru na základě typu output
        if (Engine.JASPER.equals(rulTemplateEngine)) {
            return new OutputGeneratorWorkerJasper();
        } else if (Engine.FREEMARKER.equals(rulTemplateEngine)) {
            return new OutputGeneratorWorkerFreemarker();
        } else {
            throw new IllegalStateException("Nepodporovaný typ výstupu: " + rulTemplateEngine.toString());
        }
    }

    /**
     * @param output output ke kterému je record zařazen
     * @param nodeId node ke kterému je record zařazen, pokud je null, je zařazen přímo k outputu
     * @param record zdrojový record
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public Record getRecord(Output output, NodeId nodeId, RegRecord record) {
        return new Record(output, nodeId, record);
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


    /**
     *
     * @param output output ke kterému je node zařazen
     * @param arrNodeId ID příslušného DB objektu vytvářeného node
     * @param arrLevelId ID příslušného DB objektu vytvářeného node
     * @param parentNodeId ID nadřazeného node, pokud je null je objekt kořenem stromu
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public NodeId getNodeId(Output output, Integer arrNodeId, Integer arrLevelId, Integer parentNodeId) {
        return new NodeId(output, arrNodeId, arrLevelId, parentNodeId);
    }

    /**
     *
     * @param nodeId ID požadovaného node
     * @param output output ke kterému je node zařazen
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public Node getNode(NodeId nodeId, Output output) {
        return new Node(nodeId, output);
    }
}
