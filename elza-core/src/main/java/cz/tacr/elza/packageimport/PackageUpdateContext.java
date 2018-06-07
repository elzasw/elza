package cz.tacr.elza.packageimport;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.RulPackage;

public class PackageUpdateContext {
	
	private static Logger logger = LoggerFactory.getLogger(PackageUpdateContext.class); 
	
	Integer oldVersion;
	
	RulPackage rulPackage;

	private ResourcePathResolver resourcePathResolver;
	
	public PackageUpdateContext(ResourcePathResolver resourcePathResolver) {
		this.resourcePathResolver = resourcePathResolver;
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
        
        return packageDir;
	}

	public File getOldPackageDir() {
		if(oldVersion==null) {
			return null;
		} else {
			return resourcePathResolver.getPackageDirVersion(rulPackage, oldVersion).toFile();
		}
	}

}
