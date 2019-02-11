package cz.tacr.elza.dao;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dcstorage")
public class DCStorageConfig implements InitializingBean {

	private static DCStorageConfig instance;

	private String repositoryIdentifier = "repo";

	private String basePath = "storage";

	private boolean rejectMode = false;

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
	}

	public static DCStorageConfig get() {
		return instance;
	}
}