package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import cz.tacr.elza.api.ParRelationClassTypeRepeatabilityEnum;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Třída typu vztahu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 24. 10. 2016
 */
@Entity(name = "par_relation_class_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ParRelationClassType implements cz.tacr.elza.api.ParRelationClassType {

    public enum ClassType{
        VZNIK("B"),
        ZANIK("E"),
        VZTAH("R");

        private String classType;

        ClassType(final String classType) {
            this.classType = classType;
        }

        public String getClassType() {
            return classType;
        }
    }

    @Id
    @GeneratedValue
    private Integer relationClassTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_10)
    private ParRelationClassTypeRepeatabilityEnum repeatability;

    @Override
    public Integer getRelationClassTypeId() {
        return relationClassTypeId;
    }

    @Override
    public void setRelationClassTypeId(final Integer relationClassTypeId) {
        this.relationClassTypeId = relationClassTypeId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public ParRelationClassTypeRepeatabilityEnum getRepeatability() {
        return repeatability;
    }

    @Override
    public void setRepeatability(final ParRelationClassTypeRepeatabilityEnum repeatability) {
        this.repeatability = repeatability;
    }
}
