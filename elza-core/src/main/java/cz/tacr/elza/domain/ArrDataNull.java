package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.NotImplementedException;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Entity;
import javax.persistence.Table;


/**
 * Atributu archivního popisu bez hodnoty - napr. enum
 *
 * @author Martin Šlapa
 * @since 18.11.2015
 */
@Entity(name = "arr_data_null")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataNull extends ArrData {

    @Override
    public String getFulltextValue() {
        return null;
    }
}
