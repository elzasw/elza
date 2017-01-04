package cz.tacr.elza.dao.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import cz.tacr.elza.dao.FileUtils;
import cz.tacr.elza.dao.GlobalLock;
import cz.tacr.elza.dao.bo.DaoBatchInfoBo;
import cz.tacr.elza.dao.bo.DaoBo;
import cz.tacr.elza.dao.bo.DaoFileBo;
import cz.tacr.elza.dao.bo.DaoPackageBo;
import cz.tacr.elza.dao.descriptor.DaoConfig;
import cz.tacr.elza.dao.descriptor.DaoPackageConfig;

@Service
public class DcsResourceService {

	private static final Logger LOG = LoggerFactory.getLogger(DcsResourceService.class);

	@Autowired
	private DcsFileManager fileManager;

	/**
	 * Returns DAO file resource. Existence of file is not checked.
	 * @return DaoFileResource or null when definition not found.
	 * @throws IOException When error occurs while reading DAO configuration.
	 */
	public DaoFileResource getFileResource(String packageIdentifier, String daoIdentifier, String fileIdentifier) throws IOException {
		Path daoConfigPath = fileManager.resolveDaoConfigPath(packageIdentifier, daoIdentifier);
		DaoConfig daoConfig = GlobalLock.runAtomicFunction(() -> FileUtils.readYamlFile(daoConfigPath, DaoConfig.class));
		Path filePath = daoConfigPath.getParent().resolve(fileIdentifier);
		for (Map<String, String> attrs : daoConfig.getFiles()) {
			if (fileIdentifier.equals(attrs.get(DaoConfig.FILE_IDENTIFIER_ATTR_NAME))) {
				return DaoFileResource.create(filePath, attrs);
			}
		}
		return null;
	}

	private static DaoFileBo createDaoFileBo(Map<String, String> attributes) {
		// TODO Auto-generated method stub
		return null;
	}

	DaoPackageBo initPackage(String packageIdentifier) throws IOException {
		Path packageConfigPath = fileManager.resolvePackageConfigPath(packageIdentifier);
		DaoPackageConfig packageConfig = FileUtils.readYamlFile(packageConfigPath, DaoPackageConfig.class);

		DaoPackageBo daoPackage = new DaoPackageBo(packageIdentifier);
		daoPackage.setFundIdentifier(packageConfig.getFundIdentifier());
		daoPackage.setDaoSet(getDaoSet(packageIdentifier));
		if (StringUtils.hasText(packageConfig.getBatchIdentifier())) {
			DaoBatchInfoBo daoBatchInfo = new DaoBatchInfoBo(packageConfig.getBatchIdentifier());
			daoBatchInfo.setLabel(packageConfig.getBatchLabel());
			daoPackage.setDaoBatchInfo(daoBatchInfo);
		}
		return daoPackage;
	}

	private Map<String, DaoBo> getDaoSet(String packageIdentifier) throws IOException {
		Map<DaoBo, List<String>> relatedDaoIdentifiers = new HashMap<>();
		Map<String, DaoBo> daoSet = new HashMap<>();

		fileManager.forEachActiveDaoConfig(packageIdentifier, c -> {
			DaoBo dao = new DaoBo(packageIdentifier, c.getIdentifier());
			c.getFiles().forEach(a -> addDaoFile(dao, a));
			dao.setDidIdentifier(c.getDidIdentifier());
			dao.setLabel(c.getLabel());

			if (c.getRelatedDaoIdentifiers().size() > 0) {
				relatedDaoIdentifiers.put(dao, c.getRelatedDaoIdentifiers());
			}
			if (daoSet.putIfAbsent(c.getIdentifier(), dao) != null) {
				LOG.error("duplicit dao: " + dao);
			}
		});
		relatedDaoIdentifiers.forEach((k, v) -> {
			for (String relatedIdentifier : v) {
				DaoBo related = daoSet.get(relatedIdentifier);
				if (related == null) {
					LOG.error("related dao not found, identifier:" + relatedIdentifier);
				} else {
					k.getRelatedDaoSet().add(related);
				}
			}
		});
		return daoSet;
	}

	private static void addDaoFile(DaoBo dao, Map<String, String> attributes) {
		String identifier = attributes.get("identifier");
		if (!StringUtils.hasText(identifier)) {
			LOG.error("file identifier missing: " + dao);
		} else {
			DaoFileBo daoFile = new DaoFileBo(dao.getIdentifier(), identifier);
			if (!dao.getDaoFiles().add(daoFile)) {
				LOG.error("duplicit dao file: " + daoFile);
			}
		}
	}

	public static class DaoFileResource {

		private final Path path;

		private String mimeType;

		private Long size;

		public DaoFileResource(Path path) {
			Assert.notNull(path);
			this.path = path;
		}

		public String getFileName() {
			return path.getFileName().toString();
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public Long getSize() {
			return size;
		}

		public void setSize(Long size) {
			this.size = size;
		}

		public InputStream getInputStream() throws IOException {
			return Files.newInputStream(path);WebApplicationContextUtils.

		public static DaoFileResource create(Path path, Map<String, String> attributes) {
			DaoFileResource daofr = new DaoFileResource(path);
			daofr.setMimeType(attributes.get(DaoConfig.FILE_MIME_TYPE_ATTR_NAME));
			try {
				daofr.setSize(Long.valueOf(attributes.get(DaoConfig.FILE_SIZE_ATTR_NAME)));
			} catch (NumberFormatException e) {
				// keep null size
			}
			return daofr;
		}
	}
}
