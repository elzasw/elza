package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu ArrFile.
 */
@Entity(name = "arr_data_file_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataFileRef extends ArrData {

    public static final String FIELD_FILE = "file";

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrFile.class)
    @JoinColumn(name = "fileId", nullable = false)
    private ArrFile file;

    @Column(name = "fileId", updatable = false, insertable = false)
    private Integer fileId;

	public ArrDataFileRef() {

	}

	protected ArrDataFileRef(ArrDataFileRef src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataFileRef src) {
        this.file = src.file;
        this.fileId = src.fileId;
    }

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

	@Override
	public ArrDataFileRef makeCopy() {
		return new ArrDataFileRef(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataFileRef src = (ArrDataFileRef)srcData;
        return fileId.equals(src.fileId);
    }

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataFileRef src = (ArrDataFileRef)srcData;
        copyValue(src);
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(file);
        Validate.notNull(fileId);
    }
}
