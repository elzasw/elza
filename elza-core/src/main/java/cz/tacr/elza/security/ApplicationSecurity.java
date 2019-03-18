package cz.tacr.elza.security;

import cz.tacr.elza.service.LevelTreeCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.service.UserService;

/**
 * Autentikační třída pro API.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Component
//@EnableWebMvcSecurity
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
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

    @Autowired
    private ApiLogoutSuccessHandler apiLogoutSuccessHandler;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Value("${elza.security.defaultUsername:admin}")
    private String defaultUsername;

    @Value("${elza.security.defaultPassword:0bde6ccb27aaa200002df6017ee3ddee70dacf5e9a4f99627af3447b73fde09b}")
    private String defaultPassword;

    @Value("${elza.security.allowDefaultUser:true}")
    private Boolean allowDefaultUser;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationSecurity.class);

    private SessionRegistry sessionRegistry = null;

    @Bean
    public SessionRegistry sessionRegistry() {
        if (sessionRegistry == null) {
            sessionRegistry = new SessionRegistryImpl();
        }
        return sessionRegistry;
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder builder) throws Exception {
        builder.authenticationProvider(new AuthenticationProvider() {
            @Override
            public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = authentication.getCredentials().toString();
                String encodePassword = userService.encodePassword(username, password);

                UsrUser user = userService.findByUsername(username);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, encodePassword, null);
                if (user != null) {
                    if (!user.getPassword().equals(encodePassword)) {
                        throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
                    }

                    if (!user.getActive()) {
                        throw new LockedException("User is not active");
                    }

                    auth.setDetails(new UserDetail(user, userService.calcUserPermission(user), levelTreeCacheService));
                } else if (allowDefaultUser && username.equals(defaultUsername) && encodePassword.equalsIgnoreCase(defaultPassword)) {
                    auth.setDetails(new UserDetail(defaultUsername, levelTreeCacheService));
                } else {
                    // TODO: smazat po vytvoření správy uživatelů
                    logger.warn(username + ":" + encodePassword);
                    throw new UsernameNotFoundException("Neplatné uživatelské jméno nebo heslo");
                }
                return auth;
            }

            @Override
            public boolean supports(final Class<?> authentication) {
                return authentication.equals(UsernamePasswordAuthenticationToken.class);
            }
        });

    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http.headers().frameOptions().sameOrigin();
        http.authorizeRequests().antMatchers("/api/**").authenticated();
        http.authorizeRequests()
                .antMatchers("/services").permitAll()
                .antMatchers("/services/**").authenticated()
                .and().httpBasic();
        http.csrf().disable();
        http.sessionManagement()
                .maximumSessions(10)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry());
        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
                .and()
                .logout().permitAll().logoutSuccessHandler(apiLogoutSuccessHandler);
        http.formLogin().successHandler(authenticationSuccessHandler);
        http.formLogin().failureHandler(authenticationFailureHandler);
    }

}
