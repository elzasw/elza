package cz.tacr.elza.domain.bridge;

import cz.tacr.elza.service.SpringContext;

import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Způsobí inicializaci beanu IndexConfigurationReader (a všech návazností) před inicializací Hibernate Search.
 * V ApCachedAccessPointBinder jsou potom dostupné tyto beany pomocí SpringContext.getBean()
 *
 */
@Configuration
public class IndexConfigReaderDependOnPostProcessor	
	extends EntityManagerFactoryDependsOnPostProcessor
{

    public IndexConfigReaderDependOnPostProcessor() {
        super(IndexConfigReader.class, OutboxPollingConfigurer.class, SpringContext.class);
    }
    
    @Bean(name = "cz.tacr.elza.domain.bridge.OutboxPollingConfigurer")
    public OutboxPollingConfigurer myOutboxPollingInternalConfigurer() {
    	return new OutboxPollingConfigurer();
    }

}
