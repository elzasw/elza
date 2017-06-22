package cz.tacr.elza.service.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.RulTemplate.Engine;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;

/**
 * Factory metoda pro vytváření objektů {@link OutputGeneratorWorkerJasper} a objektů vytvářených při jeho běhu s dependency injections.
 *
 */
@Component
@Configuration
public class OutputGeneratorWorkerFactory {


    /**
     * @return vytvořený objekt s provedeným dependency injections
     */
    @Bean
    @Scope("prototype")
    public OutputGeneratorWorkerAbstract getOutputGeneratorWorker(final Engine engine) {
        // skutečné vytvoření workeru na základě typu output
        if (Engine.JASPER.equals(engine)) {
            return new OutputGeneratorWorkerJasper();
        } else if (Engine.FREEMARKER.equals(engine)) {
            return new OutputGeneratorWorkerFreemarker();
        } else {
            throw new IllegalStateException("Nepodporovaný typ výstupu: " + engine.toString());
        }
    }

    /**
     * @param arrOutput zdrojová deinice výstupu
     * @return vytvořený objekt s provedeným dependency injections
     */
    public Output getOutput(final ArrOutput arrOutput) {
        return new Output(arrOutput);
    }


    /**
     *
     * @param output output ke kterému je node zařazen
     * @param arrNodeId ID příslušného DB objektu vytvářeného node
     * @param parentNodeId ID nadřazeného node, pokud je null je objekt kořenem stromu
     * @param position pozice uzlu
     * @param depth hloubka uzlu od kořene
     * @return vytvořený objekt s provedeným dependency injections
     */
    public NodeId getNodeId(final Output output, final Integer arrNodeId, final Integer parentNodeId,
            final Integer position, final Integer depth) {
        return new NodeId(output, arrNodeId, parentNodeId, position, depth);
    }

    /**
     *
     * @param nodeId ID požadovaného node
     * @param output output ke kterému je node zařazen
     * @return vytvořený objekt s provedeným dependency injections
     */
    public Node getNode(final NodeId nodeId, final Output output) {
        return new Node(nodeId, output);
    }
}
