package cz.tacr.elza.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("dcstorage")
public class DCStorageConfig implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(DCStorageConfig.class);

	private static DCStorageConfig instance;

	private String repositoryIdentifier = "repo";

	private String basePath = "storage";

	private boolean rejectMode = false;

    public DCStorageConfig() {
        log.debug("Initializing DCStorageConfig object");
    }

	public String getRepositoryIdentifier() {
		return repositoryIdentifier;
	}

	public void setRepositoryIdentifier(String repositoryIdentifier) {
		this.repositoryIdentifier = repositoryIdentifier;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public boolean isRejectMode() {
		return rejectMode;
	}

	public void setRejectMode(boolean rejectMode) {
		this.rejectMode = rejectMode;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		instance = this;
        log.debug("DCStorageConfig - properties set: basePath={}", basePath);
	}

	public static DCStorageConfig get() {
		return instance;
	}
}