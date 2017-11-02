package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu ArrFile.
 */
@Entity(name = "arr_data_file_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataFileRef extends ArrData {

    public static final String FILE = "file";

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrFile.class)
    @JoinColumn(name = "fileId", nullable = false)
    private ArrFile file;

    @Column(name = "fileId", updatable = false, insertable = false)
    private Integer fileId;

    public ArrFile getFile() {
        return file;
    }

    public void setFile(final ArrFile file) {
        this.file = file;
        this.fileId = file == null ? null : file.getFileId();
    }

    public Integer getFileId() {
        return fileId;
    }

    @Override
    public String getFulltextValue() {
        return file.getName();
    }
}
