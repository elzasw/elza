package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import cz.tacr.elza.packageimport.xml.SettingIndexSearch;

import org.apache.commons.collections4.CollectionUtils;
import org.drools.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UISettings.SettingsType;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.Setting;
import cz.tacr.elza.packageimport.xml.SettingFavoriteItemSpecs;
import cz.tacr.elza.packageimport.xml.SettingFundIssues;
import cz.tacr.elza.packageimport.xml.SettingFundViews;
import cz.tacr.elza.packageimport.xml.SettingGridView;
import cz.tacr.elza.packageimport.xml.SettingItemTypes;
import cz.tacr.elza.packageimport.xml.SettingPartsOrder;
import cz.tacr.elza.packageimport.xml.SettingRecord;
import cz.tacr.elza.packageimport.xml.SettingStructTypeSettings;
import cz.tacr.elza.packageimport.xml.SettingStructureTypes;
import cz.tacr.elza.packageimport.xml.SettingTypeGroups;
import cz.tacr.elza.repository.SettingsRepository;

/**
 * Serviska pro nastavení.
 *
 * @since 19.07.2016
 */
@Component
public class SettingsService {

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private StaticDataService staticDataService;

	static interface SettingConvertor {

		/**
		 * Convert UISettings to XML/JSON representation
		 * @param uiSettings
		 * @return Return null if convertor cannot convert selected settings.
		 *     Return valid object if settings was successfully converted.
		 */
		Setting convert(UISettings uiSettings);

		/**
		 * Type safe version of convert
		 * @param settings
		 * @param cls
		 * @return
		 */
		<T extends Setting> T convert(UISettings settings, Class<T> cls);

		<T extends Setting> boolean canConvertTo(String settingsType, Class<T> cls);

        EntityType getEntityType();

	}

	abstract class SettingConvertorBase implements SettingConvertor {
	    final UISettings.SettingsType settingsType;

	    protected SettingConvertorBase(final UISettings.SettingsType settingsType) {
	        this.settingsType = settingsType;

	    }

        @Override
        public EntityType getEntityType() {
            return settingsType.getEntityType();
        }

        @Override
        public <T extends Setting> T convert(UISettings uiSettings, Class<T> cls) {
            Setting result = convert(uiSettings);
            return cls.cast(result);
        }

	}

	class SettingConvertorSimple <R extends Setting>  extends SettingConvertorBase {

	    final Function<UISettings, R> mappingFunction;

	    final Class<R> targetClass;

	    SettingConvertorSimple(UISettings.SettingsType settingType,
	                           final Function<UISettings, R> mappingFunction,
	                           final Class<R> targetClass) {
	        super(settingType);
	        this.mappingFunction = mappingFunction;
	        this.targetClass = targetClass;
	    }

        @Override
        public R convert(UISettings uiSettings) {
	        try {
                return mappingFunction.apply(uiSettings);
            } catch (Throwable t) {
	            return null;
            }
        }

        @Override
        public <T extends Setting> boolean canConvertTo(String settingsType, Class<T> cls) {
            if(!this.settingsType.toString().equals(settingsType)) {
                return false;
            }
            return cls.isAssignableFrom(targetClass);
        }
	}

	class StructTypeSettingsConvertor extends SettingConvertorBase {

	    StructTypeSettingsConvertor() {
	        super(UISettings.SettingsType.STRUCT_TYPE_);
	    }

        @Override
        public SettingStructTypeSettings convert(UISettings uiSettings) {
            // prepare CODE
            String code = uiSettings.getSettingsType().substring(this.settingsType.toString().length());
            if(StringUtils.isEmpty(code)) {
                throw new SystemException("UISettings without code", BaseCode.DB_INTEGRITY_PROBLEM).set("settingsId", uiSettings.getSettingsId());
            }
            return SettingStructTypeSettings.newInstance(uiSettings, code);
        }

        @Override
        public <T extends Setting> boolean canConvertTo(String settingsType, Class<T> cls) {
            if(!settingsType.startsWith(this.settingsType.toString())) {
                return false;
            }
            return cls.isAssignableFrom(SettingStructTypeSettings.class);
        }

	}

    List<SettingConvertor> settingsConvertors = new ArrayList<>();

	public SettingsService() {
		// initialize setting convertors
	    settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.FUND_ISSUES,
	            SettingFundIssues::newInstance,
	            SettingFundIssues.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.FUND_VIEW,
                SettingFundViews::newInstance,
                SettingFundViews.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.TYPE_GROUPS,
                SettingTypeGroups::newInstance,
                SettingTypeGroups.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.RECORD,
                SettingRecord::newInstance,
                SettingRecord.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.STRUCTURE_TYPES,
                SettingStructureTypes::newInstance,
                SettingStructureTypes.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.GRID_VIEW,
                SettingGridView::newInstance,
                SettingGridView.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.FAVORITE_ITEM_SPECS,
                c -> SettingFavoriteItemSpecs.newInstance(c, staticDataService),
                SettingFavoriteItemSpecs.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.PARTS_ORDER,
                SettingPartsOrder::newInstance,
                SettingPartsOrder.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.ITEM_TYPES,
                SettingItemTypes::newInstance,
                SettingItemTypes.class ));
        settingsConvertors.add(new SettingConvertorSimple<>(UISettings.SettingsType.INDEX_SEARCH,
                SettingIndexSearch::newInstance,
                SettingIndexSearch.class ));
        settingsConvertors.add(new StructTypeSettingsConvertor());

		// default convertor
		// settingsConvertors.add(uiSettings -> SettingBase.newInstance(uiSettings));
	}

	public Setting convertSetting(final UISettings uiSettings) {
		for(SettingConvertor settingConvertor: settingsConvertors)
		{
			Setting setting = settingConvertor.convert(uiSettings);
			if(setting!=null) {
				return setting;
			}
		}

		throw new SystemException("Failed to convert UISettings", BaseCode.DB_INTEGRITY_PROBLEM)
			.set("settings_id", uiSettings.getSettingsId())
			.set("settings_type", uiSettings.getSettingsType());
	}

    /**
     * Načte nastavení podle uživatele.
     *
     * @param user uživatel
     * @return seznam nastavení
     */
    public List<UISettings> getSettings(int userId) {
        List<UISettings> settingsList = settingsRepository.findByUserId(userId);
        return settingsList;
    }

    /**
     * Nastaví parametry pro uživatele.
     *
     * @param user         uživatel
     * @param settingsList seznam nastavení
     */
    public void setSettings(@NotNull final UsrUser user,
                            @NotNull final List<UISettings> settingsList) {
        List<UISettings> settingsListDB = settingsRepository.findByUser(user);

        Map<Integer, UISettings> settingsMap = settingsListDB.stream()
                .collect(Collectors.toMap(UISettings::getSettingsId, Function.identity()));

        List<UISettings> settingsListAdd = new ArrayList<>();
        List<UISettings> settingsListDelete;
        List<UISettings> settingsListUpdate = new ArrayList<>();

        for (UISettings settings : settingsList) {
            if (settings.getSettingsId() == null) {
                settings.setUser(user);
                settingsListAdd.add(settings);
            } else {
                UISettings settingsDB = settingsMap.get(settings.getSettingsId());
                if (settingsDB == null) {
                    throw new ObjectNotFoundException("Entita nastavení s id=" + settings.getSettingsId() + " neexistuje v DB", BaseCode.ID_NOT_EXIST);
                }
                settings.setUser(user);
                settings.setEntityType(settingsDB.getEntityType());
                settings.setEntityId(settingsDB.getEntityId());
                settings.setSettingsType(settingsDB.getSettingsType());
                settingsListUpdate.add(settings);
            }
        }

        settingsListDelete = new ArrayList<>(settingsListUpdate);
        settingsListDelete.removeAll(settingsListUpdate);

        settingsRepository.deleteAll(settingsListDelete);
        settingsRepository.saveAll(settingsListAdd);
        settingsRepository.saveAll(settingsListUpdate);
    }

    /**
     * Vrací seznam nastavení, které se vážou na typ nastavení a typ entity.
     *
     * @param settingsType typ nastavneí
     * @param entityType   typ entity
     * @return seznam nastavení
     */
    public List<UISettings> getGlobalSettings(final String settingsType,
                                              final UISettings.EntityType entityType) {
        return settingsRepository.findByUserAndSettingsTypeAndEntityType(null, settingsType, entityType);
    }

    public List<UISettings> getGlobalSettings(String settingsType, EntityType entityType, Integer entityId) {
        return settingsRepository.findByUserAndSettingsTypeAndEntityTypeAndEntityId(null, settingsType, entityType, entityId);
    }

    /**
     * Načtení seznamu kódů atributů - implicitní atributy pro zobrazení tabulky hromadných akcí, seznam je seřazený podle
     * pořadí, které jedefinováno u atributů.
     * @return seznam kódů
     */
    public List<SettingGridView.ItemType> getGridView() {

        // načtený globální oblíbených
        List<UISettings> gridViews = getGlobalSettings(UISettings.SettingsType.GRID_VIEW.toString(), null);

        for (UISettings gridView : gridViews) {
            SettingGridView view = SettingGridView.newInstance(gridView);
            if (CollectionUtils.isNotEmpty(view.getItemTypes())) {
                return view.getItemTypes();
            }
        }

        return null;
    }

    /**
     * Read settings of given type
     * @param settingsType
     * @param entityId
     * @param cls
     * @return
     */
    public <T extends Setting> T readSettings(String settingsType, Integer entityId, Class<T> cls) {
        // Get factory
        SettingConvertor convertor = getConvertor(settingsType, cls);
        EntityType entityType = convertor.getEntityType();

        UISettings settings = null;
        // try to read settings for specific entity
        if(entityId!=null) {
            settings = readSettingsForEntity(settingsType, entityType, entityId);
        }
        // if not found -> try to read global settings
        if(settings==null) {
            settings = readSettingsForEntity(settingsType, entityType, null);
        }
        // Convert settings
        if(settings!=null) {
            return convertor.convert(settings, cls);
        }
        return null;
    }

    /**
     * Read settings of given entity
     * @param settingsType
     * @param entityId
     * @param cls
     * @return
     */
    public UISettings readSettingsForEntity(String settingsType, EntityType entityType, Integer entityId) {
        List<UISettings> settingsList = settingsRepository.findByUserAndSettingsTypeAndEntityTypeAndEntityId(null,
                                                                                                             settingsType,
                                                                                                             entityType, entityId);
        if(settingsList.size()>0) {
            // Use first settings in case of multiple settings
            // We have no priority for selecting later settings
            return settingsList.get(0);
        }
        return null;
    }

    private SettingConvertor getConvertor(String settingsType, Class cls) {
        for(SettingConvertor sc: settingsConvertors) {
            if(sc.canConvertTo(settingsType, cls)) {
                return sc;
            }
        }
        return null;
    }

    public List<UISettings> getGlobalSettings(SettingsType settingsType) {
        return getGlobalSettings(settingsType.toString(), settingsType.getEntityType());
    }
}
