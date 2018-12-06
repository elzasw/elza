package cz.tacr.elza.domain;


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "ap_name")
@Inheritance(strategy = InheritanceType.JOINED)
public class ApName {

    public static final String FIELD_NAME_ID = "nameId";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_COMPLEMENT = "complement";
    public static final String FIELD_PREFERRED_NAME = "preferredName";
    public static final String FIELD_LANGUAGE = "language";
    public static final String FIELD_ACCESS_POINT_ID = "accessPointId";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer nameId;

    @Column(length = StringLength.LENGTH_1000)
    private String name;

    @Column(length = StringLength.LENGTH_1000)
    private String complement;

    @Column(length = StringLength.LENGTH_2000)
    private String fullName;

    @Column(nullable = false)
    private boolean preferredName;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SysLanguage.class)
    @JoinColumn(name = "languageId")
    private SysLanguage language;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer languageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = FIELD_ACCESS_POINT_ID, nullable = false)
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApState state;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String errorDescription;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ApChange createChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID, nullable = true)
    private ApChange deleteChange;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Column(nullable = false)
    private Integer objectId;

    public ApName(){}

    public ApName(ApName other) {
        this.name = other.name;
        this.complement = other.complement;
        this.preferredName = other.preferredName;
        this.fullName = other.fullName;
        this.language = other.language;
        this.state = other.state;
        this.errorDescription = other.errorDescription;
        this.languageId = other.languageId;
        this.accessPoint = other.accessPoint;
        this.accessPointId = other.accessPointId;
        this.createChange = other.createChange;
        this.createChangeId = other.createChangeId;
        this.deleteChange = other.deleteChange;
        this.deleteChangeId = other.deleteChangeId;
        this.objectId = other.objectId;
    }

    public Integer getNameId() {
        return nameId;
    }

    public void setNameId(Integer nameId) {
        this.nameId = nameId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public boolean isPreferredName() {
        return preferredName;
    }

    public void setPreferredName(boolean preferredName) {
        this.preferredName = preferredName;
    }

    public SysLanguage getLanguage() {
        return language;
    }

    public void setLanguage(SysLanguage language) {
        this.language = language;
        this.languageId = language != null ? language.getLanguageId() : null;
    }

    public Integer getLanguageId() {
        return languageId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public ApState getState() {
        return state;
    }

    public void setState(final ApState state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(final Integer objectId) {
        this.objectId = objectId;
    }

    @Override
    public String toString() {
        return "ApName pk=" + nameId;
    }
}
