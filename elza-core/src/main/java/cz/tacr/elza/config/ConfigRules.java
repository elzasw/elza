package cz.tacr.elza.config;


import com.google.common.eventbus.Subscribe;
import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.packageimport.PackageService;
import cz.tacr.elza.packageimport.xml.SettingTypeGroups;
import cz.tacr.elza.repository.SettingsRepository;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Nastavení strategií pravidel.
 *
 * @author Martin Šlapa
 * @since 10.12.2015
 */
@Component
@EventBusListener
public class ConfigRules {

    public static final String FA_PREFIX = "fa-";
    public static final String DEFAULT = "default";

    private Group defaultGroup = new Group("DEFAULT", null);

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private PackageService packageService;

    private Map<String, Map<String, Map<String, TypesGroupConf>>> typeGroups;

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.RULE)) {
            typeGroups = null;
        }
    }

    public synchronized Map<String, Map<String, Map<String, TypesGroupConf>>> getTypeGroups() {
        if (typeGroups == null) {
            List<UISettings> uiSettingsList = settingsRepository.findByUserAndSettingsTypeAndEntityType(null, UISettings.SettingsType.TYPE_GROUPS, UISettings.EntityType.RULE);
            typeGroups = new HashMap<>();
            if (uiSettingsList.size() > 0) {
                uiSettingsList.forEach(uiSettings -> {
                    SettingTypeGroups setting = (SettingTypeGroups) packageService.convertSetting(uiSettings);
                    Map<String, Map<String, TypesGroupConf>> fundGroups = typeGroups.get(setting.getCode());
                    if (fundGroups == null) {
                        fundGroups = new HashMap<>();
                        typeGroups.put(setting.getCode(), fundGroups);
                    }
                    List<SettingTypeGroups.Item> items = setting.getItems();
                    for (SettingTypeGroups.Item item : items) {
                        List<SettingTypeGroups.Group> groups = item.getGroups();
                        Map<String, TypesGroupConf> typesGroupConfMap = convertGroups(groups);
                        if (item instanceof SettingTypeGroups.Default) {
                            fundGroups.put(DEFAULT, typesGroupConfMap);
                        } else if (item instanceof SettingTypeGroups.Fund) {
                            Integer fundId = ((SettingTypeGroups.Fund) item).getFundId();
                            fundGroups.put(FA_PREFIX + fundId, typesGroupConfMap);
                        } else {
                            throw new IllegalStateException("Nedefinovaný stav pro třídu:" + item.getClass().getSimpleName());
                        }
                    }
                });
            }
        }
        return typeGroups;
    }

    private Map<String, TypesGroupConf> convertGroups(final List<SettingTypeGroups.Group> groups) {
        Map<String, TypesGroupConf> typesGroupConfMap = null;
        if (CollectionUtils.isEmpty(groups)) {
            return typesGroupConfMap;
        }
        typesGroupConfMap = new HashMap<>();
        for (SettingTypeGroups.Group group : groups) {
            TypesGroupConf typesGroupConf = new TypesGroupConf();
            typesGroupConf.setName(group.getName());
            typesGroupConf.setTypes(convertTypes(group.getTypes()));
            typesGroupConfMap.put(group.getCode(), typesGroupConf);
        }
        return typesGroupConfMap;
    }

    private List<TypeInfo> convertTypes(final List<SettingTypeGroups.Type> types) {
        if (CollectionUtils.isEmpty(types)) {
            return null;
        }
        List<TypeInfo> result = new ArrayList<>();
        for (SettingTypeGroups.Type type : types) {
            TypeInfo typeInfo = new TypeInfo();
            typeInfo.setCode(type.getCode());
            typeInfo.setWidth(type.getWidth());
            result.add(typeInfo);
        }
        return result;
    }

    public List<String> getTypeGroupCodes(final String code, final Integer fundId) {
        Map<String, Map<String, Map<String, TypesGroupConf>>> typeGroups = getTypeGroups();
        List<String> list = new ArrayList<>();
        list.add(defaultGroup.getCode());

        if (typeGroups == null) {
            return list;
        }

        Map<String, Map<String, TypesGroupConf>> fundGroups = typeGroups.get(code);
        if (fundGroups == null) {
            return list;
        }

        Map<String, TypesGroupConf> groups = fundGroups.get(FA_PREFIX + fundId);

        if (groups == null) {
            groups = fundGroups.get(DEFAULT);

            if (groups == null) {
                return list;
            }
        }

        list.addAll(groups.keySet());
        return list;
    }

    public Group getGroupByType(final String code, final Integer fundId, final String typeCode) {
        Map<String, Map<String, Map<String, TypesGroupConf>>> typeGroups = getTypeGroups();
        if (typeGroups != null) {
            Map<String, Map<String, TypesGroupConf>> fundGroups = typeGroups.get(code);
            if (fundGroups != null) {
                Map<String, TypesGroupConf> groups = fundGroups.get(FA_PREFIX + fundId);
                if (groups == null) {
                    groups = fundGroups.get(DEFAULT);
                }
                if (groups != null) {
                    for (Map.Entry<String, TypesGroupConf> entry : groups.entrySet()) {
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
            }
        }

        return defaultGroup;
    }

    public List<String> getTypeCodesByGroupCode(final String code, final Integer fundId, final String groupCode) {
        Map<String, Map<String, Map<String, TypesGroupConf>>> typeGroups = getTypeGroups();
        if (typeGroups == null) {
            return new ArrayList<>();
        }

        if (typeGroups != null) {
            Map<String, Map<String, TypesGroupConf>> fundGroups = typeGroups.get(code);
            if (fundGroups != null) {
                Map<String, TypesGroupConf> groups = fundGroups.get(FA_PREFIX + fundId);
                if (groups == null) {
                    groups = fundGroups.get(DEFAULT);
                }
                if (groups != null) {
                    TypesGroupConf typesGroupConf = groups.get(groupCode);
                    if (typesGroupConf == null) {
                        return new ArrayList<>();
                    }
                    return typesGroupConf.getTypes().stream().map(s -> s.getCode()).collect(Collectors.toList());
                }
            }
        }

        return new ArrayList<>();
    }

    public Integer getTypeWidthByCode(final String code, final Integer fundId, final String typeCode) {
        Map<String, Map<String, Map<String, TypesGroupConf>>> typeGroups = getTypeGroups();
        if (typeGroups != null) {
            Map<String, Map<String, TypesGroupConf>> fundGroups = typeGroups.get(code);
            if (fundGroups != null) {

                Map<String, TypesGroupConf> groups = fundGroups.get(FA_PREFIX + fundId);

                if (groups == null) {
                    groups = fundGroups.get(DEFAULT);
                }

                if (groups != null) {
                    for (TypesGroupConf groupConf : groups.values()) {
                        for (TypeInfo typeInfo : groupConf.getTypes()) {
                            if (typeInfo.getCode().equals(typeCode)) {
                                return typeInfo.getWidth();
                            }
                        }
                    }
                }
            }
        }
        return 1;
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
        private List<TypeInfo> types;
        private String name;

        public List<TypeInfo> getTypes() {
            return types;
        }

        public void setTypes(final List<TypeInfo> types) {
            this.types = types;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
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
