package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Hodnota atributu archivního popisu typu ArrFile.
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Entity(name = "arr_data_file_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataFileRef extends ArrData {

    public static final String FILE = "file";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrFile.class)
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
        return file.getName() + file.getFileName();
    }

    @Override
    public ArrData copy() {
        ArrDataFileRef data = new ArrDataFileRef();
        data.setDataType(this.getDataType());
        data.setFile(this.getFile());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setFile(((ArrDataFileRef) data).getFile());
    }
}
