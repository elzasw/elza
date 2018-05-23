package cz.tacr.elza.packageimport;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.RulStructureDefinition;
import cz.tacr.elza.domain.RulStructureExtensionDefinition;
import cz.tacr.elza.domain.RulStructuredType;
import cz.tacr.elza.repository.StructuredTypeRepository;

/**
 * Context and basic infrastructure for rule update.
 *
 */
public class RuleUpdateContext {

	final ResourcePathResolver resourcePathResolver;

	final RulPackage rulPackage;

	final RulRuleSet rulRuleSet;

	private File dirActions;

	private File dirRules;

	private File dirGroovies;

	private File dirTemplates;
	
	/**
	 * Base path into byteStreams map
	 * 
	 * This is usualy parent folder for the input ZIP file
	 */
	private final String keyDirPath;

	private final Map<String, ByteArrayInputStream> byteStreams;
	
	private List<RulStructuredType> rulStructureTypes; 

	public RuleUpdateContext(RulPackage rulPackage, RulRuleSet rulRuleSet, ResourcePathResolver resourcePathResolver, Map<String, ByteArrayInputStream> mapEntry, String ruleDirPath) {
		this.rulPackage = rulPackage;
		this.rulRuleSet = rulRuleSet;
		this.resourcePathResolver = resourcePathResolver;
		this.byteStreams = mapEntry;
		this.keyDirPath = ruleDirPath; 
	}

	/**
	 * Initialize rule update context
	 */
	public void init(StructuredTypeRepository structureTypeRepository) 
	{
		dirActions = resourcePathResolver.getFunctionsDir(rulPackage, rulRuleSet).toFile();
		if (!dirActions.exists()) {
			dirActions.mkdirs();
		}

		dirRules = resourcePathResolver.getDroolsDir(rulPackage, rulRuleSet).toFile();
		if (!dirRules.exists()) {
			dirRules.mkdirs();
		}

		dirGroovies = resourcePathResolver.getGroovyDir(rulPackage, rulRuleSet).toFile();
		if (!dirGroovies.exists()) {
			dirGroovies.mkdirs();
		}

		dirTemplates = resourcePathResolver.getTemplatesDir(rulPackage, rulRuleSet).toFile();
		if (!dirTemplates.exists()) {
			dirTemplates.mkdirs();
		}
		
		this.rulStructureTypes = structureTypeRepository.findByRuleSet(rulRuleSet);
	}
	
	public File getRulesDir() {
		return dirRules;
	}

	public File getTemplatesDir() {
		return dirTemplates;
	}

	public File getActionsDir() {
		return dirActions;
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

	public RulPackage getRulPackage() {
		return rulPackage;
	}

	public String getRulSetCode() {
		return rulRuleSet.getCode();
	}

	public RulRuleSet getRulSet() {
		return rulRuleSet;
	}

	public ByteArrayInputStream getByteStream(String key) {
		return byteStreams.get(keyDirPath+key);
	}

	public List<RulStructuredType> getStructureTypes() {
		return rulStructureTypes;
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
}
