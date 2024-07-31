package cz.tacr.elza.controller.vo;

import java.util.List;

public class ExplorerTreeNode {
    private String uuid;
    private Integer daoId;
    private String label;
    private List<ExplorerTreeNode> childFolders;
    private List<ExplorerTreeNodeFile> childFiles;
    private ExplorerTreeNode parentFolder;
    private ExplorerTreeNode parentFolderLogical;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(Integer daoId) {
        this.daoId = daoId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<ExplorerTreeNode> getChildFolders() {
        return childFolders;
    }

    public void setChildFolders(List<ExplorerTreeNode> childFolders) {
        this.childFolders = childFolders;
    }

    public List<ExplorerTreeNodeFile> getChildFiles() {
        return childFiles;
    }

    public void setChildFiles(List<ExplorerTreeNodeFile> childFiles) {
        this.childFiles = childFiles;
    }

    public ExplorerTreeNode getParentFolder() {
        return parentFolder;
    }

    public void setParentFolder(ExplorerTreeNode parentFolder) {
        this.parentFolder = parentFolder;
    }

    public ExplorerTreeNode getParentFolderLogical() {
        return parentFolderLogical;
    }

    public void setParentFolderLogical(ExplorerTreeNode parentFolderLogical) {
        this.parentFolderLogical = parentFolderLogical;
    }
}
