package cz.tacr.elza.service;

import java.time.OffsetDateTime;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ApChangeRepository;
import cz.tacr.elza.security.UserDetail;

/**
 * Datová třída pro přístupové body.
 *
 * @since 24.07.2018
 */
@Service
public class AccessPointDataService {

    private final EntityManager em;
    private final UserService userService;
    private final ApChangeRepository apChangeRepository;

    @Autowired
    public AccessPointDataService(final EntityManager em,
                                  final UserService userService,
                                  final ApChangeRepository apChangeRepository) {
        this.em = em;
        this.userService = userService;
        this.apChangeRepository = apChangeRepository;
    }

    /**
     * Sestavení celého jména z jména a doplňku.
     *
     * @param name       jméno
     * @param complement doplněk
     * @return celé jméno
     */
    @Nullable
    public static String generateFullName(@Nullable final String name, @Nullable final String complement) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name.trim());
        if (StringUtils.isNotEmpty(complement)) {
            sb.append(" (").append(complement.trim()).append(')');
        }
        return sb.toString();
    }

    /**
     * Vytvoření změny daného typu.
     *
     * @param type typ změny
     * @return změna
     */
    public ApChange createChange(@Nullable final ApChange.Type type) {
        return createChange(type, null);
    }

    /**
     * Vytvoření změny s externím systémem.
     *
     * @param type           typ změny
     * @param externalSystem externí systém
     * @return změna
     */
    public ApChange createChange(@Nullable final ApChange.Type type, @Nullable ApExternalSystem externalSystem) {
        ApChange change = new ApChange();
        UserDetail userDetail = userService.getLoggedUserDetail();
        change.setChangeDate(OffsetDateTime.now());

        if (userDetail != null && userDetail.getId() != null) {
            UsrUser user = em.getReference(UsrUser.class, userDetail.getId());
            change.setUser(user);
        }

        change.setType(type);
        change.setExternalSystem(externalSystem);

        return apChangeRepository.save(change);
    }
}
