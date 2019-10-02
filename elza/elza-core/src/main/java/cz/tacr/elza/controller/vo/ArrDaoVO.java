package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrDao;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Value objekt {@link ArrDao}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
public class ArrDaoVO {

    private Integer id;

    private Boolean valid;

    private String code;

    private String label;

    private String url;

    private Boolean existInArrDaoRequest;

    private List<ArrDaoFileVO> fileList = new ArrayList<>();
    private List<ArrDaoFileGroupVO> fileGroupList = new ArrayList<>();

    private long fileCount;
    private long fileGroupCount;

    private ArrDaoLinkVO daoLink; // null pokud vazba neexistuje


    /**
     * Zařazení nového daoFile do seznamu na vo, zároveň nastaví položku fileCount na novou délku seznamu
     * @param file daoFile
     */
    public void addFile(ArrDaoFileVO file) {
        fileList.add(file);
        fileCount = fileList.size();
    }

    /**
     * Zařazení seznamu daoFile do seznamu na vo, zároveň nastaví položku fileCount na novou délku seznamu
     * @param files seznam daoFile
     */
    public void addAllFile(Collection<ArrDaoFileVO> files) {
        fileList.addAll(files);
        fileCount = fileList.size();
    }

    /**
     * Zařazení nového daoFileGroup do seznamu na vo, zároveň nastaví položku fileGroupCount na novou délku seznamu
     * @param fileGroup daoFileGroup
     */
    public void addFileGroup(ArrDaoFileGroupVO fileGroup) {
        fileGroupList.add(fileGroup);
        fileGroupCount = fileList.size();
    }

    /**
     * Zařazení seznamu daoFileGroup do seznamu na vo, zároveň nastaví položku fileGroupCount na novou délku seznamu
     * @param daoFileGroups seznam daoFileGroup
     */
    public void addAllFileGroup(Collection<ArrDaoFileGroupVO> daoFileGroups) {
        fileGroupList.addAll(daoFileGroups);
        fileGroupCount = fileList.size();
    }

    public ArrDaoLinkVO getDaoLink() {
        return daoLink;
    }

    public void setDaoLink(ArrDaoLinkVO arrDaoLinkVO) {
        this.daoLink = arrDaoLinkVO;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(final Boolean valid) {
        this.valid = valid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public List<ArrDaoFileVO> getFileList() {
        return ListUtils.unmodifiableList(fileList);
    }

    public List<ArrDaoFileGroupVO> getFileGroupList() {
        return ListUtils.unmodifiableList(fileGroupList);
    }

    public long getFileCount() {
        return fileCount;
    }

    public void setFileCount(long fileCount) {
            this.fileCount = fileCount;
    }

    public long getFileGroupCount() {
        return fileGroupCount;
    }

    public void setFileGroupCount(long fileGroupCount) {
        this.fileGroupCount = fileGroupCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public Boolean getExistInArrDaoRequest() {
        return existInArrDaoRequest;
    }

    public void setExistInArrDaoRequest(Boolean existInArrDaoRequest) {
        this.existInArrDaoRequest = existInArrDaoRequest;
    }
}
