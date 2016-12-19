package cz.tacr.elza.service;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviska pro nastavení.
 *
 * @author Martin Šlapa
 * @since 19.07.2016
 */
@Component
public class SettingsService {

    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private UserService userService;

    /**
     * Načte nastavení podle uživatele.
     *
     * @param user uživatel
     * @return seznam nastavení
     */
    public List<UISettings> getSettings(@NotNull final UsrUser user) {
        List<UISettings> settingsList = settingsRepository.findByUser(user);
        return settingsList;
    }

    /**
     * Nastaví parametry pro uživatele.
     *
     * @param user         uživatel
     * @param settingsList seznam nastavení
     */
    public void setSettings(@NotNull final UsrUser user,
                            @NotNull List<UISettings> settingsList) {
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
                    throw new IllegalArgumentException("Entita s id=" + settings.getSettingsId() + " neexistuje v DB");
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

        settingsRepository.delete(settingsListDelete);
        settingsRepository.save(settingsListAdd);
        settingsRepository.save(settingsListUpdate);
    }

    /**
     * Vrací seznam nastavení, které se vážou na typ nastavení a typ entity.
     *
     * @param settingsType typ nastavneí
     * @param entityType   typ entity
     * @return seznam nastavení
     */
    public List<UISettings> getGlobalSettings(final UISettings.SettingsType settingsType,
                                              final UISettings.EntityType entityType) {
        return settingsRepository.findByUserAndSettingsTypeAndEntityType(userService.getLoggedUser(), settingsType, entityType);
    }
}
