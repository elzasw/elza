package cz.tacr.elza.controller.vo;

import java.util.Collection;

import cz.tacr.elza.domain.ApAccessPoint;


/**
 * VO rejstříkového záznamu.
 */
public class ApAccessPointVO extends AbstractApAccessPoint {
    
    /**
     * Id hesla.
     */
    private Integer id;
    
    private String uuid;
    
    /**
     * Typ rejstříku.
     */
    private Integer typeId;

    /**
     * Id třídy rejstříku.
     */
    private Integer scopeId;

    /**
     * Rejstříkové heslo.
     */
    private String record;

    /**
     * Podrobná charakteristika rejstříkového hesla.
     */
    private String characteristics;
    
    private boolean invalid;
    
    /**
     * Id osoby.
     */
    // TODO: client should read if it's party AP by cached AP types and find party by AP if needed
    @Deprecated
    private Integer partyId;
    
    /**
     * Externí identifikátory rejstříkového hesla, například interpi.
     */
    private Collection<ApExternalIdVO> externalIds;

    /**
     * Seznam jmen přístupového bodu.
     */
    private Collection<ApAccessPointNameVO> names;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(Integer scopeId) {
        this.scopeId = scopeId;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    @Deprecated
    public Integer getPartyId() {
        return partyId;
    }

    @Deprecated
    public void setPartyId(Integer partyId) {
        this.partyId = partyId;
    }

    public Collection<ApExternalIdVO> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(Collection<ApExternalIdVO> externalIds) {
        this.externalIds = externalIds;
    }

    public Collection<ApAccessPointNameVO> getNames() {
        return names;
    }

    public void setNames(Collection<ApAccessPointNameVO> names) {
        this.names = names;
    }

    /**
     * Create new instance from domain object
     * 
     * @param ap
     * @return
     */
    public static ApAccessPointVO newInstance(ApAccessPoint ap) {
        ApAccessPointVO apvo = new ApAccessPointVO();
        apvo.setId(ap.getAccessPointId());
        apvo.setInvalid(ap.getDeleteChange() != null);
        apvo.setScopeId(ap.getScopeId());
        apvo.setTypeId(ap.getApTypeId());
        apvo.setUuid(ap.getUuid());
        return apvo;
    }
}
