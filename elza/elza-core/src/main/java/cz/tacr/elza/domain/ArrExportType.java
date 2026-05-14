package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Publication / export type definition.
 *
 * Configures one target system for the public publication API: its filter,
 * retention policy and which Elza permissions allow publishing into it.
 * Persisted into {@code arr_export_type}.
 */
@Entity(name = "arr_export_type")
public class ArrExportType {

	public static final String FIELD_NAME = "name";
    public static final String FIELD_CODE = "code";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer exportTypeId;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulExportFilter.class)
    @JoinColumn(name = "exportFilterId", nullable = true)
    private RulExportFilter exportFilter;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer exportFilterId;

    /** Number of most recent exports retained on disk; 0 = unlimited. */
    @Column(nullable = false)
    private Integer retentionCount;

    /** Users holding the FUND_EXPORT permission may publish to this type. */
    @Column(nullable = false)
    private Boolean allowPermExport;

    /** Users holding the FUND_PUBLISH permission may publish to this type. */
    @Column(nullable = false)
    private Boolean allowPermPublication;

    public Integer getExportTypeId() {
        return exportTypeId;
    }

    public void setExportTypeId(Integer exportTypeId) {
        this.exportTypeId = exportTypeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public RulExportFilter getExportFilter() {
        return exportFilter;
    }

    public void setExportFilter(RulExportFilter exportFilter) {
        this.exportFilter = exportFilter;
        this.exportFilterId = exportFilter != null ? exportFilter.getExportFilterId() : null;
    }

    public Integer getExportFilterId() {
        return exportFilterId;
    }

    public Integer getRetentionCount() {
        return retentionCount;
    }

    public void setRetentionCount(Integer retentionCount) {
        this.retentionCount = retentionCount;
    }

    public Boolean getAllowPermExport() {
        return allowPermExport;
    }

    public void setAllowPermExport(Boolean allowPermExport) {
        this.allowPermExport = allowPermExport;
    }

    public Boolean getAllowPermPublication() {
        return allowPermPublication;
    }

    public void setAllowPermPublication(Boolean allowPermPublication) {
        this.allowPermPublication = allowPermPublication;
    }
}
