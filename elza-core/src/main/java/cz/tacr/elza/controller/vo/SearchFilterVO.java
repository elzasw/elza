package cz.tacr.elza.controller.vo;

import java.util.List;

public class SearchFilterVO {

    private String search;

    private Area area;

    private Boolean onlyMainPart;

    private List<Integer> aeTypeIds = null;

    private List<AeState> aeStates = null;

    private String code;

    private String user;

    private String creation;

    private String extinction;

    private List<RelationFilterVO> relFilters = null;

    private List<ExtensionFilterVO> extFilters = null;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public Boolean getOnlyMainPart() {
        return onlyMainPart;
    }

    public void setOnlyMainPart(Boolean onlyMainPart) {
        this.onlyMainPart = onlyMainPart;
    }

    public List<Integer> getAeTypeIds() {
        return aeTypeIds;
    }

    public void setAeTypeIds(List<Integer> aeTypeIds) {
        this.aeTypeIds = aeTypeIds;
    }

    public List<AeState> getAeStates() {
        return aeStates;
    }

    public void setAeStates(List<AeState> aeStates) {
        this.aeStates = aeStates;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCreation() {
        return creation;
    }

    public void setCreation(String creation) {
        this.creation = creation;
    }

    public String getExtinction() {
        return extinction;
    }

    public void setExtinction(String extinction) {
        this.extinction = extinction;
    }

    public List<RelationFilterVO> getRelFilters() {
        return relFilters;
    }

    public void setRelFilters(List<RelationFilterVO> relFilters) {
        this.relFilters = relFilters;
    }

    public List<ExtensionFilterVO> getExtFilters() {
        return extFilters;
    }

    public void setExtFilters(List<ExtensionFilterVO> extFilters) {
        this.extFilters = extFilters;
    }
}
