package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.ap.item.ApItemVO;

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
    private Integer parentPartId;

    /**
     * Identifikátor části
     */
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
}
