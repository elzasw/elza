package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.List;

public class ResultLookupVO {

    private String value;

    private String partTypeCode;

    private List<HighlightVO> highlights = new ArrayList<>();

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPartTypeCode() {
        return partTypeCode;
    }

    public void setPartTypeCode(String partTypeCode) {
        this.partTypeCode = partTypeCode;
    }

    public List<HighlightVO> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<HighlightVO> highlights) {
        this.highlights = highlights;
    }
}
