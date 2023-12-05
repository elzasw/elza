package cz.tacr.elza.security;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;
import cz.tacr.elza.security.oauth2.JwtUserDetailProvider;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import cz.tacr.elza.repository.ItemTypeRepository;
//import cz.tacr.elza.security.oauth2.JwtUserDetailProvider;
import cz.tacr.elza.security.oauth2.OAuth2Properties;
import cz.tacr.elza.security.ssoheader.SsoHeaderAuthenticationFilter;
import cz.tacr.elza.security.ssoheader.SsoHeaderAuthenticationProvider;
import cz.tacr.elza.security.ssoheader.SsoHeaderProperties;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;
import net.minidev.json.JSONObject;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Authentization configuration for API
 *
 * @since 11.04.2016
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
public class ApplicationSecurity {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSecurity.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    private ApiAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private ApiAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private ApiAuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ApiLogoutSuccessHandler apiLogoutSuccessHandler;

    @Autowired
    private Optional<SsoHeaderProperties> optionalSsoHeaderProperties;

    @Autowired
    private Optional<OAuth2Properties> optionalOAuth2Props;

    private SessionRegistry sessionRegistry = null;

    @Bean
    public SessionRegistry sessionRegistry() {
        if (sessionRegistry == null) {
            sessionRegistry = new SessionRegistryImpl();
        }
        return sessionRegistry;
    }


    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean("applicationAuthenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        List<AuthenticationProvider> ap = new ArrayList<>();

        ap.add(new PasswordAutheticationProvider(userService));
        if (optionalSsoHeaderProperties.isPresent()) {
            ap.add(new SsoHeaderAuthenticationProvider(userService));
        }
        if (optionalOAuth2Props.isPresent()) {
            JwtDecoder jwtDecoder = applicationContext.getBean(JwtDecoder.class);
            AccessPointService apService = applicationContext.getBean(AccessPointService.class);

            ap.add(new JwtUserDetailProvider(jwtDecoder, txManager, userService, apService,
                    itemTypeRepository, optionalOAuth2Props.get()));
        }
        return new ProviderManager(ap);
    }

    private static class RestOperationsResourceRetriever implements ResourceRetriever {
        private static final MediaType APPLICATION_JWK_SET_JSON = new MediaType("application", "json");
        private final RestOperations restOperations;

        RestOperationsResourceRetriever(RestOperations restOperations) {
            Assert.notNull(restOperations, "restOperations cannot be null");
            this.restOperations = restOperations;
        }

        @Override
        public com.nimbusds.jose.util.Resource retrieveResource(URL url) throws IOException {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, APPLICATION_JWK_SET_JSON));

            ResponseEntity<String> response;
            try {
                RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, url.toURI());
                response = this.restOperations.exchange(request, String.class);
            } catch (Exception ex) {
                throw new IOException(ex);
            }

            if (response.getStatusCodeValue() != 200) {
                throw new IOException(response.toString());
            }

            return new com.nimbusds.jose.util.Resource(response.getBody(), "UTF-8");
        }
    }

    private static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid JWK Set URL \"" + url + "\" : " + ex.getMessage(), ex);
        }
    }

    private byte[] getKeySpec(String keyValue) {
        keyValue = keyValue.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        return Base64.getMimeDecoder().decode(keyValue);
    }

    @ConditionalOnProperty(prefix = "elza.security.o-auth2", name = "key-url")
    @Bean
    public JwtDecoder getjwtDecoder() {
        URL tokenKeyUrl = toURL(optionalOAuth2Props.get().getKeyUrl());

        // read public key
        RestOperations restOperations = new RestTemplate();
        RestOperationsResourceRetriever rorr = new RestOperationsResourceRetriever(restOperations);
        try {
            Resource tokenResource = rorr.retrieveResource(tokenKeyUrl);
            Map<String, Object> jsonKey = JSONObjectUtils.parse(tokenResource.getContent());
            String key = (String) jsonKey.get("value");

            String jwsAlgorithm = "RS256";

            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(getKeySpec(key)));
            JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
                    .signatureAlgorithm(SignatureAlgorithm.from(jwsAlgorithm)).build();
            return jwtDecoder;
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse token", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid key specification", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid key algoritm", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read token", e);
        }
    }


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
        configureOAuth2(http);
        return http.build();
    }



    private void configureOAuth2(HttpSecurity http) throws Exception {
        if (!optionalOAuth2Props.isPresent()) {
            return;
        }

        http
                // enable resource server
                .oauth2ResourceServer()
                // enable JWT processing
                .jwt()
                // set own authentication manager
                //  - allows to set JwtUserDetailProvider as a AutheticationProvider for JWT
                .authenticationManager(authenticationManagerBean());

        log.info("OAuth2 auto-user mapping filter was configured");
    }

    private void configureSsoHeaderFilter(HttpSecurity http) throws Exception {
        if (optionalSsoHeaderProperties.isPresent()) {
            SsoHeaderAuthenticationFilter filter = new SsoHeaderAuthenticationFilter(optionalSsoHeaderProperties.get());
            filter.setAuthenticationManager(authenticationManagerBean());
            filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
            filter.setAuthenticationFailureHandler(authenticationFailureHandler);
            http.addFilterBefore(filter, AbstractPreAuthenticatedProcessingFilter.class);
            log.info("SSO header authentication filter was configured");
        }
    }

}
