package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.UISettings.EntityType;
import cz.tacr.elza.domain.UsrUser;

/**
 * Repozitory pro {@link UISettings}
 *
 * @since 19.07.2016
 */
@Repository
public interface SettingsRepository extends JpaRepository<UISettings, Integer>, Packaging<UISettings> {

    List<UISettings> findByUser(UsrUser user);

    List<UISettings> findByUserId(int userId);

    /*
    List<UISettings> findByUserAndSettingsType(UsrUser user,
                                               Collection<UISettings.SettingsType> settingsTypes);
                                               */

    List<UISettings> findByUserAndSettingsTypeAndEntityType(UsrUser user,
                                                            String settingsType,
                                                            UISettings.EntityType entityType);

    List<UISettings> findByUserAndEntityTypeAndEntityId(UsrUser user,
                                                        UISettings.EntityType entityType,
                                                        Integer entityId);

    List<UISettings> findByUserAndSettingsTypeAndEntityTypeAndEntityId(UsrUser user,
                                                                       String settingsType,
                                                                       EntityType entityType, 
                                                                       Integer entityId);

    //List<UISettings> findBySettingsType(UISettings.SettingsType settingsType);
}
