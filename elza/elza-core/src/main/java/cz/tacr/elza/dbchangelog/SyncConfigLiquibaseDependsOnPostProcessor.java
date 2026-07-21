package cz.tacr.elza.dbchangelog;

import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.context.annotation.Configuration;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Forces SpringContext and SyncConfig to be initialised before SpringLiquibase, so custom
 * Liquibase changes can look them up via SpringContext.getBean(...) during their execute().
 */
@Deprecated
@Configuration
public class SyncConfigLiquibaseDependsOnPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

	public SyncConfigLiquibaseDependsOnPostProcessor() {
	    super(SpringLiquibase.class, "springContext", "syncConfig");
	}
}