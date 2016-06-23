package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.search.DescItemIndexingInterceptor;
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
@Indexed(interceptor = DescItemIndexingInterceptor.class)
@Entity(name = "arr_output_item")
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrOutputItem extends ArrItem implements cz.tacr.elza.api.ArrOutputItem<ArrOutputDefinition> {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name = "outputDefinitionId", nullable = false)
    private ArrOutputDefinition outputDefinition;

    public ArrOutputItem(final ArrItemData item) {
        super(item);
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
        throw new NotImplementedException();
    }

    @Override
    public Integer getFundId() {
        throw new NotImplementedException();
    }

    @Override
    public ArrNode getNode() {
        throw new NotImplementedException();
    }
}
