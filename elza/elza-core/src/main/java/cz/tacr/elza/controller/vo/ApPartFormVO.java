package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ApPartFormVO {

    /**
     * Kód typu části
     */
    private String partTypeCode;

    /**
     * Seznam všech hodnot atributů
     */
    private List<ApItemVO> items = new ArrayList<>();

    /**
     * Identifikátor nadřízené části
     */
    @Nullable
    private Integer parentPartId;

    /**
     * Identifikátor nadřízené části z revize
     */
    @Nullable
    private Integer revParentPartId;

    /**
     * Identifikátor části
     */
    @Nullable
    private Integer partId;

    public String getPartTypeCode() {
        return partTypeCode;
    }

    public void setPartTypeCode(String partTypeCode) {
        this.partTypeCode = partTypeCode;
    }

    public List<ApItemVO> getItems() {
        return items;
    }

    public void setItems(List<ApItemVO> items) {
        this.items = items;
    }

    public Integer getParentPartId() {
        return parentPartId;
    }

    public void setParentPartId(Integer parentPartId) {
        this.parentPartId = parentPartId;
    }

    public Integer getPartId() {
        return partId;
    }

    public void setPartId(Integer partId) {
        this.partId = partId;
    }

    @Nullable
    public Integer getRevParentPartId() {
        return revParentPartId;
    }

    public void setRevParentPartId(@Nullable Integer revParentPartId) {
        this.revParentPartId = revParentPartId;
    }
}
