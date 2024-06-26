package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "da_dao")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaDao {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer daoId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaAip.class)
    @JoinColumn(name = "aip_id", nullable = false)
    private DaAip aip;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = StringLength.LENGTH_ENUM, nullable = false)
    private DaoType type;

    @Column(name = "code", length = StringLength.LENGTH_250, nullable = false)
    private String code;

    @Column(name = "label", length = StringLength.LENGTH_250)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaLevelView.class)
    @JoinColumn(name = "level_view_id")
    private DaLevelView levelView;

    public enum DaoType {

        LOGICAL,
        REPRESENTATION,
        FILE,
        METAAMD,
        METADMDINHERENT,
        METADMDCONTEXTUAL
    }

    public Integer getDaoId() {
        return daoId;
    }

    public void setDaoId(Integer daoId) {
        this.daoId = daoId;
    }

    public DaAip getAip() {
        return aip;
    }

    public void setAip(DaAip aip) {
        this.aip = aip;
    }

    public DaChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(DaChange createChange) {
        this.createChange = createChange;
    }

    public DaChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(DaChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public DaoType getType() {
        return type;
    }

    public void setType(DaoType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DaLevelView getLevelView() {
        return levelView;
    }

    public void setLevelView(DaLevelView levelView) {
        this.levelView = levelView;
    }
}
