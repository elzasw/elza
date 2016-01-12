package cz.tacr.elza;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.descitems.ArrDescItemGroupVO;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.repository.FindingAidVersionRepository;


/**
 * Nastavení strategií pravidel.
 *
 * @author Martin Šlapa
 * @since 10.12.2015
 */
@Component
@ConfigurationProperties(prefix = "elza")
public class ElzaRules {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    private Group defaultGroup = new Group("DEFAULT", "Bez zařazení");

    /**
     * Seznam strategii podle pravidel.
     */
    private Map<String, Map<String, List<String>>> rules;

    @Valid
    private Map<String, TypesGroupConf> typeGroups;

    public Map<String, Map<String, List<String>>> getRules() {
        return rules;
    }

    public void setRules(final Map<String, Map<String, List<String>>> rules) {
        this.rules = rules;
    }

    public Map<String, TypesGroupConf> getTypeGroups() {
        return typeGroups;
    }

    public void setTypeGroups(final Map<String, TypesGroupConf> typeGroups) {
        this.typeGroups = typeGroups;
    }

    public List<String> getTypeGroupCodes() {
        List<String> list = new ArrayList<>();
        if (typeGroups != null) {
            list.addAll(typeGroups.keySet());
        }
        list.add(defaultGroup.getCode());
        return list;
    }

    public Group getGroupByType(final String typeCode) {

        if (typeGroups != null) {
            for (Map.Entry<String, TypesGroupConf> entry : typeGroups.entrySet()) {
                List<TypeInfo> typeInfos = entry.getValue().getTypes();
                if (typeInfos != null) {
                    for (TypeInfo typeInfo : typeInfos) {
                        if (typeInfo.getCode().equals(typeCode)) {
                            return new Group(entry.getKey(), entry.getValue().getName());
                        }
                    }
                }
            }
        }

        return defaultGroup;
    }

    public List<String> getTypeCodesByGroupCode(final String groupCode) {
        if (typeGroups == null) {
            return new ArrayList<>();
        }
        TypesGroupConf typesGroupConf = typeGroups.get(groupCode);
        if (typesGroupConf == null) {
            return new ArrayList<>();
        }
        return typesGroupConf.getTypes().stream().map(s -> s.getCode()).collect(Collectors.toList());
    }

    public Integer getTypeWidthByCode(final String code) {
        if (typeGroups != null) {
            for (TypesGroupConf typesGroupConf : typeGroups.values()) {
                for (TypeInfo typeInfo : typesGroupConf.getTypes()) {
                    if (typeInfo.getCode().equals(code)) {
                        return typeInfo.getWidth();
                    }
                }
            }
        }
        return 1;
    }

    /**
     * Vrací seznam strategií.
     *
     * @param versionId identifikátor verze AP
     * @return seznam strategií
     */
    public Set<String> getStrategies(final Integer versionId) {
        Assert.notNull(versionId);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);

        Assert.notNull(version, "Verzev s ID:" + versionId + " neexistuje");

        String ruleCode = version.getRuleSet().getCode();

        if (rules == null) {
            logger.warn("Nejsou nastavené strategie");
            return new HashSet<>();
        }

        Map<String, List<String>> ruleMap = rules.get(ruleCode);

        if (ruleMap == null) {
            logger.warn("Nejsou nastavené strategie pro kód pravidel: " + ruleCode);
            return new HashSet<>();
        }

        List<String> strategies = ruleMap.get("version_" + versionId);

        if (strategies == null) {
            strategies = ruleMap.get("default");
            if (strategies == null) {
                throw new IllegalStateException("Nejsou nastavené výchozí (default) strategie pro kód pravidel: " + ruleCode);
            }
        }

        return new HashSet<>(strategies);
    }

    public static class Group {

        private String code;

        private String name;

        public Group(final String code, final String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class TypesGroupConf {

        private String name;

        private List<TypeInfo> types;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public List<TypeInfo> getTypes() {
            return types;
        }

        public void setTypes(final List<TypeInfo> types) {
            this.types = types;
        }
    }

    public static class TypeInfo {

        private String code;

        private Integer width;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(final Integer width) {
            this.width = width;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TypeInfo typeInfo = (TypeInfo) o;

            return new EqualsBuilder()
                    .append(code, typeInfo.code)
                    .append(width, typeInfo.width)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(code)
                    .append(width)
                    .toHashCode();
        }
    }

}
