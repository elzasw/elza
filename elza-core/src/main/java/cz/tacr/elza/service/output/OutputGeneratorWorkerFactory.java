package cz.tacr.elza.service.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.RulTemplate.Engine;

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
}
