package cz.tacr.elza.service;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Serviska pro uživatele.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Value("${elza.security.salt:kdFss=+4Df_%}")
    private String SALT;

    private ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);

    public UsrUser findByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }

    public String encodePassword(final String username, final String password) {
        return encoder.encodePassword(password, username + SALT);
    }

    /**
     * Vrací přihlášeného uživatele - DO.
     *
     * @return přihlášený uživatel (null pokud je přihlášený admin nebo je to akce bez přihlášení)
     */
    public UsrUser getLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName());
    }

    /**
     * Vrací detail přihlášeního uživatele - VO.
     *
     * @return detail přihlášeného uživatele (null pokud není nikdo přihlášený)
     */
    public UserDetail getLoggedUserDetail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return (UserDetail) auth.getDetails();
    }
}
