package cz.tacr.elza.security;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Autentikační třída pro API.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Component
@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class ApplicationSecurity extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserService userService;

    @Autowired
    private ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private ApiAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private ApiAuthenticationSuccessHandler authenticationSuccessHandler;

    @Value("${elza.security.defaultUsername:admin}")
    private String defaultUsername;

    @Value("${elza.security.defaultPassword:0bde6ccb27aaa200002df6017ee3ddee70dacf5e9a4f99627af3447b73fde09b}")
    private String defaultPassword;

    @Value("${elza.security.allowDefaultUser:true}")
    private Boolean allowDefaultUser;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSecurity.class);

    @Override
    protected void configure(AuthenticationManagerBuilder builder) throws Exception {
        builder.authenticationProvider(new AuthenticationProvider() {
            @Override
            public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = authentication.getCredentials().toString();
                String encodePassword = userService.encodePassword(username, password);

                List<GrantedAuthority> grantedAuths = new ArrayList<>();
                UsrUser user = userService.findByUsername(username);
                if (user != null) {
                    if (!user.getPassword().equals(encodePassword)) {
                        throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
                    }

                    if (!user.getActive()) {
                        throw new LockedException("User is not active");
                    }

                    grantedAuths.add(new SimpleGrantedAuthority("USER"));
                    Authentication auth = new UsernamePasswordAuthenticationToken(username, encodePassword, grantedAuths);
                    return auth;
                } else if (allowDefaultUser && username.equals(defaultUsername) && encodePassword.equalsIgnoreCase(defaultPassword)) {
                    grantedAuths.add(new SimpleGrantedAuthority("ADMIN"));
                    Authentication auth = new UsernamePasswordAuthenticationToken(username, encodePassword, grantedAuths);
                    return auth;
                } else {
                    // TODO: smazat po vytvoření správy uživatelů
                    logger.warn(username + ":" + encodePassword);
                    throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
                }
            }

            @Override
            public boolean supports(final Class<?> authentication) {
                return authentication.equals(UsernamePasswordAuthenticationToken.class);
            }
        });

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/api/**").authenticated();
        http.csrf().disable();
        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
                .and()
                .logout().permitAll();
        http.formLogin().successHandler(authenticationSuccessHandler);
        http.formLogin().failureHandler(authenticationFailureHandler);
    }

}
