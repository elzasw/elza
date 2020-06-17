package cz.tacr.elza.controller.vo;

import javax.annotation.Nullable;

public class ArchiveEntityVO {

    private Integer id;

    private String name;

    private Integer aeTypeId;

    @Nullable
    private String description;

    private ResultLookupVO resultLookups;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAeTypeId() {
        return aeTypeId;
    }

    public void setAeTypeId(Integer aeTypeId) {
        this.aeTypeId = aeTypeId;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public ResultLookupVO getResultLookups() {
        return resultLookups;
    }

    public void setResultLookups(ResultLookupVO resultLookups) {
        this.resultLookups = resultLookups;
    }
}
