package cz.tacr.elza.interpi.service.vo;

import cz.tacr.elza.api.enums.InterpiClass;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;

/**
 * Objekt mapování použitý při importu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 4. 1. 2017
 */
public class MappingVO {

    private String interpiRelationType;

    private String interpiRoleType;

    private InterpiClass interpiClass;

    private String interpiId;

    private ParRelationType parRelationType;

    private ParRelationRoleType parRelationRoleType;

    private boolean importRelation;

    public String getInterpiRelationType() {
        return interpiRelationType;
    }

    public void setInterpiRelationType(final String interpiRelationType) {
        this.interpiRelationType = interpiRelationType;
    }

    public String getInterpiRoleType() {
        return interpiRoleType;
    }

    public void setInterpiRoleType(final String interpiRoleType) {
        this.interpiRoleType = interpiRoleType;
    }

    public InterpiClass getInterpiClass() {
        return interpiClass;
    }

    public void setInterpiClass(final InterpiClass interpiClass) {
        this.interpiClass = interpiClass;
    }

    public String getInterpiId() {
        return interpiId;
    }

    public void setInterpiId(final String interpiId) {
        this.interpiId = interpiId;
    }

    public ParRelationType getParRelationType() {
        return parRelationType;
    }

    public void setParRelationType(final ParRelationType parRelationType) {
        this.parRelationType = parRelationType;
    }

    public ParRelationRoleType getParRelationRoleType() {
        return parRelationRoleType;
    }

    public void setParRelationRoleType(final ParRelationRoleType parRelationRoleType) {
        this.parRelationRoleType = parRelationRoleType;
    }

    public boolean isImportRelation() {
        return importRelation;
    }

    public void setImportRelation(final boolean importRelation) {
        this.importRelation = importRelation;
    }
}
