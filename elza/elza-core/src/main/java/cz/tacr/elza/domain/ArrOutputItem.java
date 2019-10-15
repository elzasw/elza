package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Atribut pro výstupy.
 *
 * @author Martin Šlapa
 * @since 20.06.2016
 */
@Entity(name = "arr_output_item")
@Table
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public class ArrOutputItem extends ArrItem {

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrOutput.class)
    @JoinColumn(name = "outputId", nullable = false)
    private ArrOutput output;

    public ArrOutputItem() {
    }

    public ArrOutputItem(ArrOutputItem srcItem) {
        super(srcItem);
        this.output = srcItem.output;
    }

    @Override
    public ArrOutput getOutput() {
        return output;
    }

    public void setOutput(final ArrOutput output) {
        this.output = output;
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

    @Override
    public ArrItem makeCopy() {
        return new ArrOutputItem(this);
    }
}
