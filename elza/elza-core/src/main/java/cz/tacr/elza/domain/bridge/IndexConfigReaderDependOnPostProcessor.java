package cz.tacr.elza.domain.bridge;

import cz.tacr.elza.service.SpringContext;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * Způsobí inicializaci beanu IndexConfigurationReader (a všech návazností) před inicializací Hibernate Search.
 * V ApCachedAccessPointBinder jsou potom dostupné tyto beany pomocí SpringContext.getBean()
 *
 * @author <a href="mailto:jaroslav.pubal@marbes.cz">Jaroslav Půbal</a>
 */
@Configuration
public class IndexConfigReaderDependOnPostProcessor extends EntityManagerFactoryDependsOnPostProcessor {

    public IndexConfigReaderDependOnPostProcessor() {
        super(IndexConfigReader.class, SpringContext.class);
    }

}
