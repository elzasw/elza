package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_text")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataText extends ArrData implements cz.tacr.elza.api.ArrDataText{

    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String value;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        RulDescItemSpec descItemSpec = getDescItem().getDescItemSpec();
        if (descItemSpec == null) {
            return value;
        }

        return descItemSpec.getName() + ": " + value;
    }
}
