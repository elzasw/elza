package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaDao;

import java.util.ArrayList;
import java.util.List;

public class DaDaoVO {
    private Integer daoId;
    private DaAip aip;
    private DaDao.DaoType type;
    private String code;
    private String label;
    private List<DaDaoFileFolderVO> folders = new ArrayList<>();
    private List<DaDaoFileVO> files = new ArrayList<>();

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(Integer daoId) {
        this.daoId = daoId;
    }

    public DaAip getAip() {
        return aip;
    }

    public void setAip(DaAip aip) {
        this.aip = aip;
    }

    public DaDao.DaoType getType() {
        return type;
    }

    public void setType(DaDao.DaoType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<DaDaoFileFolderVO> getFolders() {
        return folders;
    }

    public void setFolders(List<DaDaoFileFolderVO> folders) {
        this.folders = folders;
    }

    public List<DaDaoFileVO> getFiles() {
        return files;
    }

    public void setFiles(List<DaDaoFileVO> files) {
        this.files = files;
    }
}
