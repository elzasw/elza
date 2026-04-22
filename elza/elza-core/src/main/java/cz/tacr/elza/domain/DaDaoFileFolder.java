package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

@Entity(name = "da_dao_file_folder")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class DaDaoFileFolder {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer daoFileFolderId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private DaChange createChange;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaChange.class)
    @JoinColumn(name = "delete_change_id")
    private DaChange deleteChange;

    @Column(name = "label", length = StringLength.LENGTH_250, nullable = false)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDao.class)
    @JoinColumn(name = "representation_dao_id", nullable = false)
    private DaDao representationDao;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DaDaoFileFolder.class)
    @JoinColumn(name = "parent_file_folder_id")
    private DaDaoFileFolder parentFileFolder;

    public Integer getDaoFileFolderId() {
        return daoFileFolderId;
    }

    public void setDaoFileFolderId(Integer daoFileFolderId) {
        this.daoFileFolderId = daoFileFolderId;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DaDao getRepresentationDao() {
        return representationDao;
    }

    public void setRepresentationDao(DaDao representationDao) {
        this.representationDao = representationDao;
    }

    public DaDaoFileFolder getParentFileFolder() {
        return parentFileFolder;
    }

    public void setParentFileFolder(DaDaoFileFolder parentFileFolder) {
        this.parentFileFolder = parentFileFolder;
    }
}
