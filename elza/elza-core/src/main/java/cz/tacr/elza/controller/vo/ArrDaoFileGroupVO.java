package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.ListUtils;

import cz.tacr.elza.domain.ArrDaoFileGroup;

/**
 * Value objekt {@link ArrDaoFileGroup}
  *
 */
public class ArrDaoFileGroupVO {

    private Integer id;
    private String label;
    private String code;

    private long fileCount = 0;
    private List<ArrDaoFileVO> fileList = new ArrayList<>();

    public ArrDaoFileGroupVO() {
    }


    public ArrDaoFileGroupVO(ArrDaoFileGroup daoFileGroup) {
        id = daoFileGroup.getDaoId();
        label = daoFileGroup.getLabel();
        code = daoFileGroup.getCode();
    }

    /**
     * Zařazení nového daoFile do seznamu na vo, zároveň nastaví položku fileCount na novou délku seznamu
     *
     * @param file daoFile
     */
    public void addFile(ArrDaoFileVO file) {
        fileList.add(file);
        fileCount++;
    }

    /**
     * Nastavení seznami daoFile do seznamu na vo, zároveň nastaví položku fileCount na novou délku seznamu
     *
     * @param files seznam daoFile
     */
    public void setFiles(Collection<ArrDaoFileVO> files) {
        fileList.addAll(files);
        fileCount = fileList.size();
    }

    public List<ArrDaoFileVO> getFileList() {
        return ListUtils.unmodifiableList(fileList);
    }

    public long getFileCount() {
        return fileCount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public static ArrDaoFileGroupVO newInstance(ArrDaoFileGroup daoFileGroup) {
        ArrDaoFileGroupVO groupVo = new ArrDaoFileGroupVO(daoFileGroup);
        return groupVo;
    }
}
