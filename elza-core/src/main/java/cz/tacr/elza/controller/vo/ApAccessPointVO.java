package cz.tacr.elza.controller.vo;

import cz.tacr.elza.controller.vo.ap.ApFormVO;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;


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
    private Integer typeId;

    /**
     * Id osoby.
     */
    // TODO: validate if needed, client should read if it's party AP by cached AP types
    @Deprecated
    private Integer partyId;

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

    /**
     * Externí identifikátory rejstříkového hesla, například interpi.
     */
    private Collection<ApExternalIdVO> externalIds = Collections.emptyList();

    /**
     * Seznam jmen přístupového bodu.
     */
    private Collection<ApAccessPointNameVO> names = Collections.emptyList();

    private String uuid;

    private boolean invalid;

    /**
     * Strukturované data formuláře pro AP. Vyplněné pouze v případě, že se jedná o strukturovaný typ.
     */
    @Nullable
    private ApFormVO form;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
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

    public Collection<ApExternalIdVO> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(final Collection<ApExternalIdVO> externalIds) {
        this.externalIds = externalIds;
    }

    @Deprecated
    public Integer getPartyId() {
        return partyId;
    }

    @Deprecated
    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public Collection<ApAccessPointNameVO> getNames() {
        return names;
    }

    public void setNames(final Collection<ApAccessPointNameVO> names) {
        this.names = names;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
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

    @Nullable
    public ApFormVO getForm() {
        return form;
    }

    public void setForm(@Nullable final ApFormVO form) {
        this.form = form;
    }
}
