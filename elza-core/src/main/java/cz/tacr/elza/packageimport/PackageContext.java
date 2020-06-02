package cz.tacr.elza.packageimport;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.ApRule;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.xml.PackageInfo;

public class PackageContext {

    /**
     * hlavní soubor v zipu
     */
    public static final String PACKAGE_XML = "package.xml";

	private static Logger logger = LoggerFactory.getLogger(PackageContext.class);

    ZipFile zipFile = null;

	Integer oldVersion;

	RulPackage rulPackage;

	private File dirRules;

	private File dirGroovies;

	private ResourcePathResolver resourcePathResolver;

    private Map<String, ByteArrayInputStream> byteStreams;

	private List<RulStructuredType> structuredTypes;

    PackageInfo packageInfo;

    /**
     * Contexts for each updated rule
     */
    private List<RuleUpdateContext> ruleUpdateContexts = new ArrayList<>();

    /**
     * Set of structured types to be regenerated
     */
    private Set<String> regenerateStructTypes = new HashSet<>();

    /**
     * Flag if node cache should be synchronized after update
     */
    private boolean syncNodeCache;

    public PackageContext(ResourcePathResolver resourcePathResolver) {
		this.resourcePathResolver = resourcePathResolver;
	}

    /**
     * Initialize package import from file
     * 
     * @param file
     *            ZIP file to be imported
     * @throws IOException
     * @throws ZipException
     */
    public void init(File file) throws ZipException, IOException {
        zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        Map<String, ByteArrayInputStream> bss = PackageUtils.createStreamsMap(zipFile, entries);
        init(bss);
    }

    public void init(final Map<String, ByteArrayInputStream> byteStreams) throws IOException {

        this.byteStreams = byteStreams;

        // read package info
        // načtení info o importu
        packageInfo = convertXmlStreamToObject(PackageInfo.class, PACKAGE_XML);
        if (packageInfo == null) {
            throw new BusinessException("Soubor " + PACKAGE_XML + " nenalezen", PackageCode.FILE_NOT_FOUND)
                    .set("file", PACKAGE_XML);
        }
    }

    public <T> T convertXmlStreamToObject(final Class<T> classObject, final String fileName) {
        ByteArrayInputStream inputStream = byteStreams.get(fileName);
        if (inputStream == null) {
            return null;
        }

        return PackageUtils.convertXmlStreamToObject(classObject, inputStream);
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

    /**
     * Uložení souboru.
     *
     * @param dir
     *            adresář
     * @param zipDir
     *            adresář v ZIP
     * @param filename
     *            název souboru
     */
	public File saveFile(File dir, String zipDir, String filename) throws IOException {

		File file = new File(dir.getPath() + File.separator + filename);

		if (file.exists()) {
			throw new IllegalStateException("Soubor " + file.getPath() + " jiz existuje");
		}

		if (filename.contains("/")) {
            file.getParentFile().mkdirs();
		}

		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new FileWriter(file));
			ByteArrayInputStream byteArrayInputStream = byteStreams.get(zipDir + "/" + filename);

			if (byteArrayInputStream == null) {
				throw new IllegalStateException("Soubor " + zipDir + "/" + filename + " neexistuje v zip");
			}

            try (FileOutputStream bw = new FileOutputStream(file);) {

                byte[] buf = new byte[8192];
                for (;;) {
                    int nread = byteArrayInputStream.read(buf, 0, buf.length);
                    if (nread <= 0) {
                        break;
                    }
                    bw.write(buf, 0, nread);
                }
			}

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
			case PARSE_VALUE:
			case SERIALIZED_VALUE:
				return dirGroovies;
			default:
				throw new NotImplementedException("Def type: " + definition.getDefType());
		}
	}

	public File getDir(ApRule apRule) {
		switch (apRule.getRuleType()) {
			case BODY_ITEMS:
			case NAME_ITEMS:
				return dirRules;
			case TEXT_GENERATOR:
			case MIGRATE:
				return dirGroovies;
			default:
				throw new NotImplementedException("Rule type: " + apRule.getRuleType());
		}
	}

	public File getDir(RulStructureExtensionDefinition def) {
		switch (def.getDefType()) {
			case ATTRIBUTE_TYPES:
				return dirRules;
			case PARSE_VALUE:
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

    public void addRegenerateStructureType(String code) {
        regenerateStructTypes.add(code);
    }

    public Set<String> getByteStreamKeys() {
        return byteStreams.keySet();
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public void close() {
        if (byteStreams != null) {
            byteStreams = null;
        }
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
                // ok
            }
            zipFile = null;
        }
    }

    public Set<String> getRegenerateStructureTypes() {
        return regenerateStructTypes;
    }

    public boolean isSyncNodeCache() {
        return syncNodeCache;
    }

    public void setSyncNodeCache(final boolean sync) {
        this.syncNodeCache = sync;
    }

    public void addRuleUpdateContext(RuleUpdateContext ruc) {
        // check that rules were not already added
        for (RuleUpdateContext crc : ruleUpdateContexts) {
            Validate.isTrue(!Objects.equals(crc.getRulSetCode(), ruc.getRulSetCode()));
        }

        ruleUpdateContexts.add(ruc);
    }

    public RuleUpdateContext getRuleUpdateContextByCode(String code) {
        // check that rules were not already added
        for (RuleUpdateContext ruc : ruleUpdateContexts) {
            if (ruc.getRulSetCode().equals(code)) {
                return ruc;
            }
        }
        return null;
    }

    public List<RuleUpdateContext> getRuleUpdateContexts() {
        return Collections.unmodifiableList(ruleUpdateContexts);
    }
}
