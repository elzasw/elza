package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.search.ItemIndexingInterceptor;
import org.apache.commons.lang.NotImplementedException;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;


/**
 * Implementace {@link cz.tacr.elza.api.ArrOutputItem}
 *
 * @author Martin Šlapa
 * @since 20.06.2016
 */
@Indexed(interceptor = ItemIndexingInterceptor.class)
@Entity(name = "arr_output_item")
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrOutputItem<T extends ArrItemData> extends ArrItem<T> implements cz.tacr.elza.api.ArrOutputItem<ArrOutputDefinition> {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutputDefinition.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    public ArrOutputItem() {
    }

    public ArrOutputItem(final T item) {
        super(item);
    }

    public ArrOutputItem(final Class<T> clazz) throws IllegalAccessException, InstantiationException {
        super(clazz.newInstance());
    }

    public ArrOutputDefinition getOutputDefinition() {
        return outputDefinition;
    }

    public void setOutputDefinition(final ArrOutputDefinition outputDefinition) {
        this.outputDefinition = outputDefinition;
    }

    @Override
    public String toString() {
        return "ArrOutputItem pk=" + getItemId();
    }

    @Override
    public Integer getNodeId() {
        return null; //throw new NotImplementedException();
    }

    @Override
    public Integer getFundId() {
        return null; //throw new NotImplementedException();
    }

    @Override
    public ArrNode getNode() {
        return null; //throw new NotImplementedException();
    }
}
