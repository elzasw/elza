package cz.tacr.elza.daoimport.service.vo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImportDao {

    private UUID communityId;
    private UUID collectionId;
    private String daoId;
    private String daoName;
    Map<Integer, String> metadata = new HashMap<>();

    private List<ImportBundle> bundles = new ArrayList<>();
    private Map<String, ImportBundle> bundleMap = new HashMap<>();

    public void addFile(String bundleName, DaoFile file){
        if (file != null) {
        	ImportBundle importBundle = bundleMap.get(bundleName);
        	if(importBundle==null) {
        		importBundle = new ImportBundle(bundleName);
        		bundles.add(importBundle);
        		bundleMap.put(bundleName, importBundle);
        	}
        	
        	importBundle.add(file);
        }
    }

	public void addFile(String bundleName, Path destPath) {
		if (destPath == null) {
			return;
		}
		DaoFile file = new DaoFile();
		file.setFile(destPath);
		addFile(bundleName, file);
	}

	public List<ImportBundle> getBundles() {
        return bundles;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public String getDaoId() {
        return daoId;
    }

    public void setDaoId(String daoId) {
        this.daoId = daoId;
    }

    public Map<Integer, String> getMetadata() {
        return metadata;
    }

    public void addMetadata(Integer metadataId, String value) {
        metadata.put(metadataId, value);
    }

    public String getDaoName() {
        return daoName;
    }

    public void setDaoName(String daoName) {
        this.daoName = daoName;
    }

}
