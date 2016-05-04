package cz.tacr.elza.bulkaction.factory;

import cz.tacr.elza.bulkaction.BulkActionConfig;
import cz.tacr.elza.bulkaction.BulkActionWorker;
import cz.tacr.elza.bulkaction.generator.BulkAction;
import cz.tacr.elza.domain.ArrBulkActionRun;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Factory na vytváření instancí workerů hromadných akcí.
 *
 * @author Petr Compel
 * @since 3.5.2016
 */
@Component
@Configuration
public class BulkActionWorkerFactory {

    /**
     * Vytvoření instance workeru
     *
     * @return instance workeru
     */
    @Bean
    @Scope("prototype")
    public BulkActionWorker getWorker() {
        return new BulkActionWorker();
    }
}
