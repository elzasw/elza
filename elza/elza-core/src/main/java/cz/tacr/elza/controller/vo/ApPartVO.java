package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.ap.ApStateVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemVO;
import cz.tacr.elza.domain.ApPart;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ApPartVO {

    /**
     * Identifikátor partu.
     */
    private Integer id;

    /**
     * Hodnota partu.
     */
    private String value;

    /**
     * Stav partu.
     */
    private ApStateVO state;

    /**
     * Identifikátor typu partu.
     */
    private Integer typeId;

    /**
     * Chyby v partu.
     */
    @Nullable
    private String errorDescription;

    /**
     * Identifikátor nadřazeného partu.
     */
    @Nullable
    private Integer partParentId;

    /**
     * Seznam hodnot atributů
     */
    private List<ApItemVO> items = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ApStateVO getState() {
        return state;
    }

    public void setState(ApStateVO state) {
        this.state = state;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    @Nullable
    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(@Nullable String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Nullable
    public Integer getPartParentId() {
        return partParentId;
    }

    public void setPartParentId(@Nullable Integer partParentId) {
        this.partParentId = partParentId;
    }

    public List<ApItemVO> getItems() {
        return items;
    }

    public void setItems(List<ApItemVO> items) {
        this.items = items;
    }
}
