package cz.tacr.elza.security;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import cz.tacr.elza.security.ssoheader.SsoHeaderAuthenticationFilter;
import cz.tacr.elza.security.ssoheader.SsoHeaderAuthenticationProvider;
import cz.tacr.elza.security.ssoheader.SsoHeaderProperties;
import cz.tacr.elza.service.UserService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Autentikační třída pro API.
 *
 * @author Martin Šlapa
 * @since 11.04.2016
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
public class ApplicationSecurity {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSecurity.class);

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
    private Optional<SsoHeaderProperties> optionalSsoHeaderProperties;

    private SessionRegistry sessionRegistry = null;

    @Bean
    public SessionRegistry sessionRegistry() {
        if (sessionRegistry == null) {
            sessionRegistry = new SessionRegistryImpl();
        }
        return sessionRegistry;
    }

//    @Override TODO pasek
//    protected void configure(final AuthenticationManagerBuilder builder) throws Exception {
//        builder.authenticationProvider(new PasswordAutheticationProvider(userService));
//        if (optionalSsoHeaderProperties.isPresent()) {
//        	builder.authenticationProvider(new SsoHeaderAuthenticationProvider(userService));
//        }
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

//    @Bean("applicationAuthenticationManager")
//    @Override
//    public AuthenticationManager authenticationManagerBean() throws Exception {
//    	return super.authenticationManagerBean();
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.headers().frameOptions().sameOrigin();
        http.authorizeRequests().requestMatchers(new AntPathRequestMatcher("/api/auth/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated();
        http.authorizeRequests().requestMatchers(
        new AntPathRequestMatcher("/services")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/services/**")).authenticated()
                .and().httpBasic().authenticationEntryPoint(authenticationEntryPoint);
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

        configureSsoHeaderFilter(http);
        return http.build();
    }

	private void configureSsoHeaderFilter(HttpSecurity http) throws Exception {
		if (optionalSsoHeaderProperties.isPresent()) {
			SsoHeaderAuthenticationFilter filter = new SsoHeaderAuthenticationFilter(optionalSsoHeaderProperties.get());
			//filter.setAuthenticationManager(authenticationManagerBean()); TODO pasek
			filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
			filter.setAuthenticationFailureHandler(authenticationFailureHandler);
			http.addFilterBefore(filter, AbstractPreAuthenticatedProcessingFilter.class);
			log.info("SSO header authentication filter was configured");
		}
	}
}
