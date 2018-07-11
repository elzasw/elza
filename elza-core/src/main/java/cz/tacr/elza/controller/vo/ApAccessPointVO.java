package cz.tacr.elza.controller.vo;

import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * VO rejstříkového záznamu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 21.12.2015
 */
public class ApAccessPointVO extends AbstractApAccessPoint {
    /**
     * Id hesla.
     */
    private Integer id;
    /**
     * Typ rejstříku.
     */
    private Integer apTypeId;

    /**
     * Id osoby.
     */
    private Integer partyId;

    /**
     * Id třídy rejstříku.
     */
    private Integer scopeId;

    /**
     * Externí zdroj hesel.
     */
    private ApExternalSystemVO externalSystem;

    /**
     * Rejstříkové heslo.
     */
    private String record;

    /**
     * Podrobná charakteristika rejstříkového hesla.
     */
    private String characteristics;
    /**
     * Poznámka k heslu v rejstříku.
     */
    private String note;
    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     */
    private String externalId;

    /**
     * Verze záznamu.
     */
    private Integer version;

    /**
     * Seznam variantních rejstříkových hesel.
     */
    private List<ApAccessPointNameVO> variantRecords = Collections.EMPTY_LIST;

    /** Cesta od toho rejstříku až ke kořeni. První záznam je přímý nadřízený, poslední je kořen. */
    private List<RecordParent> parents;

    /**
     * Názvy typů rejstříku od typu až po kořenový typ.
     */
    private List<RecordParent> typesToRoot;

    /**
     * Lze přidat podřízený záznam.
     */
    private boolean addRecord;

    private String uuid;

    private Date lastUpdate;

    private boolean invalid;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    public void setApTypeId(final Integer apTypeId) {
        this.apTypeId = apTypeId;
    }

    public ApExternalSystemVO getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(final ApExternalSystemVO externalSystem) {
        this.externalSystem = externalSystem;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(final String record) {
        this.record = record;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public List<ApAccessPointNameVO> getVariantRecords() {
        return variantRecords;
    }

    public void setVariantRecords(final List<ApAccessPointNameVO> variantRecords) {
        this.variantRecords = variantRecords;
    }

    public boolean isAddRecord() {
        return addRecord;
    }

    public void setAddRecord(final boolean addRecord) {
        this.addRecord = addRecord;
    }

    public List<RecordParent> getParents() {
        return parents;
    }

    public void setParents(final List<RecordParent> parents) {
        this.parents = parents;
    }

    public List<RecordParent> getTypesToRoot() {
        return typesToRoot;
    }

    public void setTypesToRoot(final List<RecordParent> typesToRoot) {
        this.typesToRoot = typesToRoot;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean getInvalid() {
        return invalid;
    }

	public boolean isInvalid() {
		return invalid;
	}

	public void setInvalid(final boolean invalid) {
		this.invalid = invalid;
	}

    public static class RecordParent{
        private Integer id;
        private String name;

        public RecordParent() {
        }

        public RecordParent(final Integer id, final String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }
}
