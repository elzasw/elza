package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.DaChange;
import java.util.List;

public class DaDaoFileFolderVO {
    private String uuid;
    private Integer daoFileFolderId;
    private DaChange createChange;
    private DaChange deleteChange;
    private String label;
    private DaDaoVO representationDao;
    private List<DaDaoFileFolderVO> childFolders;
    private List<DaDaoFileVO> childFiles;
    private DaDaoFileFolderVO parentFolder;

    public DaDaoFileFolderVO getParentFolderLogical() {
        return parentFolderLogical;
    }

    public void setParentFolderLogical(DaDaoFileFolderVO parentFolderLogical) {
        this.parentFolderLogical = parentFolderLogical;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private DaDaoFileFolderVO parentFolderLogical;

    public Integer getDaoFileFolderId() {
        return daoFileFolderId;
    }

    public void setDaoFileFolderId(Integer daoFileFolderId) {
        this.daoFileFolderId = daoFileFolderId;
    }

    public DaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(DaChange createChange) {
        this.createChange = createChange;
    }

    public DaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(DaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DaDaoVO getRepresentationDao() {
        return representationDao;
    }

    public void setRepresentationDao(DaDaoVO representationDao) {
        this.representationDao = representationDao;
    }

    public List<DaDaoFileFolderVO> getChildFolders() {
        return childFolders;
    }

    public void setChildFolders(List<DaDaoFileFolderVO> childFolders) {
        this.childFolders = childFolders;
    }

    public List<DaDaoFileVO> getChildFiles() {
        return childFiles;
    }

    public void setChildFiles(List<DaDaoFileVO> childFiles) {
        this.childFiles = childFiles;
    }


    public DaDaoFileFolderVO getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(DaDaoFileFolderVO parentFolder) {
        this.parentFolder = parentFolder;
    }
}
