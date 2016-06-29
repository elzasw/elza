package cz.tacr.elza.service.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// TODO - JavaDoc - Lebeda

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 29.6.16
 */
@Component
@Configuration
public class OutputGeneratorWorkerFactory {


    // TODO - JavaDoc - Lebeda
    @Bean
    @Scope("prototype")
    public OutputGeneratorWorker getOutputGeneratorWorker() {
        return new OutputGeneratorWorker();
    }
}
