package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;


/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrDataFileRef}
 *
 * @author Petr Compel <petr.compel@marbes.cz>
 * @since 17.6.2016
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_file_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataFileRef extends ArrData implements cz.tacr.elza.api.ArrDataFileRef<ArrFile> {

    public static final String FILE = "file";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrFile.class)
    @JoinColumn(name = "fileId", nullable = false)
    private ArrFile file;

    @Override
    public ArrFile getFile() {
        return file;
    }

    @Override
    public void setFile(final ArrFile file) {
        this.file = file;
    }

    @Override
    public String getFulltextValue() {
        return file.getName() + file.getFileName();
    }
}
