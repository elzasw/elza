package cz.tacr.elza.daoimport.service.vo;

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

    private List<DaoFile> files = new LinkedList();

    public void addFile(DaoFile file){
        if (file != null) {
            files.add(file);
        }
    }

    public List<DaoFile> getFiles() {
        return files;
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

    public void setMetadata(Map<Integer, String> metadata) {
        this.metadata = metadata;
    }

    public String getDaoName() {
        return daoName;
    }

    public void setDaoName(String daoName) {
        this.daoName = daoName;
    }
}
