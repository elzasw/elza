package cz.tacr.elza.controller.vo;

import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.controller.factory.ApFactory;
import cz.tacr.elza.drools.model.PartType;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import cz.tacr.elza.service.cache.CachedPart;

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

    static public ArchiveEntityVO valueOf(CachedAccessPoint entity) {
        ArchiveEntityVO ae = new ArchiveEntityVO();
        ae.id = entity.getAccessPointId();
        ae.aeTypeId = entity.getApState().getApTypeId(); 
        List<CachedPart> parts = entity.getParts();
        for (CachedPart part : parts) {
            if (part.getPartId().equals(entity.getPreferredPartId())) {
                ae.name = ApFactory.findDisplayIndexValue(part);
            } else {
                if (part.getPartTypeCode().equals(PartType.PT_BODY.name())) {
                    ae.description = ApFactory.findDisplayIndexValue(part);
                }
            }
        }
        return ae;
    }
}
