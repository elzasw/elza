package cz.tacr.elza.controller.vo;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;


/**
 * VO pro pravidla s typy výstupů.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
public class RulRuleSetVO {

    private Integer id;

    private String code;

    private String name;

    /** Kódy atributů pro zobrazení v gridu hromadných úprav */
    private List<GridView> gridViews;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public List<GridView> getGridViews() {
        return gridViews;
    }

    public void setGridViews(final List<GridView> gridViews) {
        this.gridViews = gridViews;
    }

    public static class GridView {

        /**
         * Kód atributu.
         */
        private String code;

        /**
         * Zobrazit ve výchozím zobrazení?
         */
        private Boolean showDefault;

        /**
         * Výchozí šířka.
         */
        private Integer width;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public Boolean getShowDefault() {
            return showDefault;
        }

        public void setShowDefault(final Boolean showDefault) {
            this.showDefault = showDefault;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(final Integer width) {
            this.width = width;
        }

    }
}
