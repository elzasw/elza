package cz.tacr.elza.packageimport;

import java.io.*;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.domain.*;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import cz.tacr.elza.core.ResourcePathResolver;

public class PackageUpdateContext {

	private static Logger logger = LoggerFactory.getLogger(PackageUpdateContext.class);

	Integer oldVersion;

	RulPackage rulPackage;

	private File dirRules;

	private File dirGroovies;

	private ResourcePathResolver resourcePathResolver;

	private final Map<String, ByteArrayInputStream> byteStreams;

	private List<RulStructuredType> structuredTypes;

	private List<ApFragmentType> fragmentTypes;

	public PackageUpdateContext(ResourcePathResolver resourcePathResolver, Map<String, ByteArrayInputStream> byteStreams) {
		this.resourcePathResolver = resourcePathResolver;
		this.byteStreams = byteStreams;
	}

	Integer getOldPackageVersion() {
		return oldVersion;
	}

	public void setOldPackageVersion(Integer version) {
		oldVersion = version;
	}

	public RulPackage getPackage() {
		return rulPackage;
	}

	public void setPackage(RulPackage rulPackage) {
		this.rulPackage = rulPackage;
	}

	public File preparePackageDir() {
        // create path for package data
        File packageDir = this.resourcePathResolver.getPackageDir(rulPackage).toFile();

        logger.info("New package directory: {}", packageDir);

        if(packageDir.exists()) {
        	// drop old files
        	boolean deleted = FileSystemUtils.deleteRecursively(packageDir);
        	if(!deleted) {
        		logger.error("Failed to delete directory: "+packageDir);
        	}
        }

        // create directory
        if(!packageDir.exists()) {
        	packageDir.mkdirs();
        }

		dirRules = resourcePathResolver.getDroolsDir(rulPackage).toFile();
		if (!dirRules.exists()) {
			dirRules.mkdirs();
		}

		dirGroovies = resourcePathResolver.getGroovyDir(rulPackage).toFile();
		if (!dirGroovies.exists()) {
			dirGroovies.mkdirs();
		}

        return packageDir;
	}

	public File getOldPackageDir() {
		if(oldVersion==null) {
			return null;
		} else {
			return resourcePathResolver.getPackageDirVersion(rulPackage, oldVersion).toFile();
		}
	}

	public File saveFile(File dir, String zipDir, String filename) throws IOException {

		File file = new File(dir.getPath() + File.separator + filename);

		if (file.exists()) {
			throw new IllegalStateException("Soubor " + file.getPath() + " jiz existuje");
		}

		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new FileWriter(file));
			ByteArrayInputStream byteArrayInputStream = byteStreams.get(zipDir + "/" + filename);

			if (byteArrayInputStream == null) {
				throw new IllegalStateException("Soubor " + zipDir + "/" + filename + " neexistuje v zip");
			}

			FileOutputStream bw = new FileOutputStream(file);

			byte[] buf = new byte[8192];
			for (; ; ) {
				int nread = byteArrayInputStream.read(buf, 0, buf.length);
				if (nread <= 0) {
					break;
				}
				bw.write(buf, 0, nread);
			}

			bw.close();

		} finally {
			if (output != null) {
				output.close();
			}
		}

		return file;
	}

	public ByteArrayInputStream getByteStream(String key) {
		return byteStreams.get(key);
	}

	public File getDir(RulStructureDefinition definition) {
		switch (definition.getDefType()) {
			case ATTRIBUTE_TYPES:
				return dirRules;
			case SERIALIZED_VALUE:
				return dirGroovies;
			default:
				throw new NotImplementedException("Def type: " + definition.getDefType());
		}
	}

	public File getDir(ApFragmentRule fragmentRule) {
		switch (fragmentRule.getRuleType()) {
			case FRAGMENT_ITEMS:
				return dirRules;
			case TEXT_GENERATOR:
				return dirGroovies;
			default:
				throw new NotImplementedException("Rule type: " + fragmentRule.getRuleType());
		}
	}

	public File getDir(ApRule apRule) {
		switch (apRule.getRuleType()) {
			case BODY_ITEMS:
			case NAME_ITEMS:
				return dirRules;
			case TEXT_GENERATOR:
				return dirGroovies;
			default:
				throw new NotImplementedException("Rule type: " + apRule.getRuleType());
		}
	}

	public File getDir(RulStructureExtensionDefinition def) {
		switch (def.getDefType()) {
			case ATTRIBUTE_TYPES:
				return dirRules;
			case SERIALIZED_VALUE:
				return dirGroovies;
			default:
				throw new NotImplementedException("Def type: " + def.getDefType());
		}
	}

	public List<RulStructuredType> getStructuredTypes() {
		return structuredTypes;
	}

	public void setStructureTypes(List<RulStructuredType> structuredTypes) {
		this.structuredTypes = structuredTypes;
	}

	public List<ApFragmentType> getFragmentTypes() {
		return fragmentTypes;
	}

	public void setFragmentTypes(final List<ApFragmentType> fragmentTypes) {
		this.fragmentTypes = fragmentTypes;
	}
}
