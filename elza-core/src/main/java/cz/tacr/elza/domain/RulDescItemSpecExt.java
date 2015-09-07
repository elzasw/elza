package cz.tacr.elza.domain;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.req.ax.IdObject;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */

public class RulDescItemSpecExt extends RulDescItemSpec implements cz.tacr.elza.api.RulDescItemSpecExt<RulDescItemType, RulDescItemConstraint, RegRegisterType> {

    private List<RulDescItemConstraint> rulDescItemConstraintList = new LinkedList<>();

    @Override
    public List<RulDescItemConstraint> getRulDescItemConstraintList() {
        return this.rulDescItemConstraintList;
    }

    public void setRulDescItemConstraintList(List<RulDescItemConstraint> rulDescItemConstraintList) {
        this.rulDescItemConstraintList = rulDescItemConstraintList;
    }
}
