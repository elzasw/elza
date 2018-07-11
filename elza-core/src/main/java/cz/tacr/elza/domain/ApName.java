package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "ap_name")
@Inheritance(strategy = InheritanceType.JOINED)
public class ApName {

    public static final String NAME_ID = "nameId";
    public static final String NAME = "name";
    public static final String COMPLEMENT = "complement";
    public static final String PREFERRED_NAME = "preferredName";
    public static final String LANGUAGE = "language";
    public static final String ACCESS_POINT_ID = "accessPointId";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer nameId;

    @Column(length = StringLength.LENGTH_1000)
    private String name;

    @Column(length = StringLength.LENGTH_1000)
    private String complement;

    @Column(nullable = false)
    private boolean preferredName;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = SysLanguage.class)
    @JoinColumn(name = "languageId")
    private SysLanguage language;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer languageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = ACCESS_POINT_ID, nullable = false)
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ApChange createChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = DELETE_CHANGE_ID, nullable = true)
    private ApChange deleteChange;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    public ApName(){}

    public ApName(ApName other) {
        this.name = other.name;
        this.complement = other.complement;
        this.preferredName = other.preferredName;
        this.language = other.language;
        this.languageId = other.languageId;
        this.accessPoint = other.accessPoint;
        this.accessPointId = other.accessPointId;
        this.createChange = other.createChange;
        this.createChangeId = other.createChangeId;
        this.deleteChange = other.deleteChange;
        this.deleteChangeId = other.deleteChangeId;
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

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    @Transient
    public String getFullName() {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name);
        if (StringUtils.isNotEmpty(complement)) {
            sb.append(" (").append(complement).append(')');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "ApName pk=" + nameId;
    }
}
