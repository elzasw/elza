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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.Resource;
import com.nimbusds.jose.util.ResourceRetriever;

import cz.tacr.elza.security.kerberos.KerberosPassAuthProvider;
import cz.tacr.elza.security.kerberos.KerberosProperties;
import cz.tacr.elza.security.kerberos.KerberosTokenAuthProvider;
import cz.tacr.elza.security.ldap.ActiveDirectoryUserDetailProvider;
import cz.tacr.elza.security.ldap.LdapProperties;
import cz.tacr.elza.security.oauth2.JwtUserDetailProvider;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.security.oauth2.OAuth2Properties;
import cz.tacr.elza.security.ssoheader.SsoHeaderAuthenticationFilter;
import cz.tacr.elza.security.ssoheader.SsoHeaderAuthenticationProvider;
import cz.tacr.elza.security.ssoheader.SsoHeaderProperties;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentization configuration for API
 *
 * @since 11.04.2016
 */
@Configuration
@EnableWebSecurity
@Order(SecurityProperties.BASIC_AUTH_ORDER - 2)
public class ApplicationSecurity {

    private static final Logger log = LoggerFactory.getLogger(ApplicationSecurity.class);
    
    public static final String AUTHENTICATE_SSO = "/authenticate/sso";

    /**
     * These patterns need to be allowed to access without authorization
     * to make it possible to navigate from the browser address bar for unauthorized users
     * @see cz.tacr.elza.web.controller.ElzaWebController (elza-web)
     */
    public static final String[] PERMIT_ALL_PATTERNS = {"/", "/res/**", "/static/**", 
    		"/fund/**", "/node/**", "/entity/**", "/admin/**", "/h2-console/**" };

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

    @Autowired
    private Optional<LdapProperties> optionalLdapProps;

    @Autowired
    private Optional<KerberosProperties> optionalKerberosProps;
    
    @Autowired
    private SiemAuditLogger siemAuditLogger;

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
    	log.debug("Configuring authentication manager.");
    	
        List<AuthenticationProvider> ap = new ArrayList<>();

        if (optionalSsoHeaderProperties.isPresent()) {
            ap.add(new SsoHeaderAuthenticationProvider(userService, txManager, siemAuditLogger));
        }
        if (optionalOAuth2Props.isPresent()) {
        	log.debug("Adding JWT based provider.");
            JwtDecoder jwtDecoder = applicationContext.getBean(JwtDecoder.class);
            AccessPointService apService = applicationContext.getBean(AccessPointService.class);

            ap.add(new JwtUserDetailProvider(jwtDecoder, txManager, userService, apService,
                    itemTypeRepository, optionalOAuth2Props.get(), siemAuditLogger));
        }
        if(optionalLdapProps.isPresent()) {
			if (optionalLdapProps.get().getAdDomain() != null) {
				log.debug("Adding ActiveDirectory provider, domain: {}, server: {}.",
						optionalLdapProps.get().getAdDomain(), optionalLdapProps.get().getAdServer());
				// adding active directory domain
				var adProvider = new ActiveDirectoryUserDetailProvider(optionalLdapProps.get(), txManager, userService, siemAuditLogger);
				
				ap.add(adProvider);
			}
        }
        if(optionalKerberosProps.isPresent()) {
        	var kerberosProps = optionalKerberosProps.get();
			// adding default Kerberos provider
			log.debug("Adding Kerberos token authentication provider (SSO).");			
			ap.add(new KerberosTokenAuthProvider(sunJaasKerberosTicketValidator(), userService, txManager, siemAuditLogger));
						
			if(kerberosProps.isAuthenticateWithPassword()) {
				log.debug("Adding Kerberos password authentication provider.");
		        // Configure native JRE Kerberos client
		        SunJaasKerberosClient client = new SunJaasKerberosClient();
		        client.setDebug(optionalKerberosProps.get().isKerberosClientDebug());
		        ap.add(new KerberosPassAuthProvider(client, userService, txManager, siemAuditLogger));				
			}
        }
        ap.add(new PasswordAutheticationProvider(userService, txManager, siemAuditLogger));

        // Wrap the chain so a single provider failing (e.g. Kerberos validation for a
        // local-password user) is logged according to the overall outcome instead of
        // flooding the log with an ERROR stack trace on every successful login.
        return new DeferredFailureAuthenticationManager(new ProviderManager(ap));
    }

    // ?
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

            if (response.getStatusCode().value() != 200) {
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
    public HttpFirewall strictHttpFirewall() {
    	// This allows to accept some Czech characters as header values when using SSO header
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // allow all header values
        firewall.setAllowedHeaderValues(v -> true);
        return firewall;
    }

    /**
     * Dedicated, highest-priority security chain for the Actuator endpoints.
     *
     * The management endpoints are published on a loopback-only port (see the
     * actuator defaults in {@code ElzaWebApp}) and serve the local CSC reporting
     * client, so they are accessed without authentication. The primary chain
     * below requires authentication for every request that is not explicitly
     * permitted; scoping this chain to {@code /actuator/**} and ordering it first
     * keeps the actuator endpoints reachable while leaving the rest of the
     * application protected.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(AntPathRequestMatcher.antMatcher("/actuator/**"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
        	.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))

    		// by https://www.baeldung.com/spring-security-migrate-5-to-6
        	/**
        	 * Dotaz na autorizaci /login je vychozi endpoint a nevyžaduje zvláštní povolení k přístupu,
        	 * tento dotaz je zpracován výchozím Spring controller.
        	 */
    		.authorizeHttpRequests(auth -> auth
    				.requestMatchers(PERMIT_ALL_PATTERNS).permitAll()
    				// Explicitly require auth for SSO
    			    .requestMatchers(AntPathRequestMatcher.antMatcher(AUTHENTICATE_SSO)).authenticated() 
    				.anyRequest().authenticated())
    		.httpBasic(Customizer.withDefaults())
    		// .requestCache(cache -> cache.requestCache(requestCache()))

        	.sessionManagement(session -> session
                .maximumSessions(10)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry()))

        	.exceptionHandling(ex -> ex.authenticationEntryPoint(getAuthenticationEntryPoint()) ) 

            .formLogin(formLogin -> formLogin
           		.successHandler(authenticationSuccessHandler)
        		.failureHandler(authenticationFailureHandler))

        	.logout(logout -> logout.permitAll().logoutSuccessHandler(apiLogoutSuccessHandler));

        configureSsoHeaderFilter(http);
        configureOAuth2(http);
        configureKerberos(http);
        return http.build();
    }
    
    /**
     * Method will return the authentication entry point
     * @return
     */
    private AuthenticationEntryPoint getAuthenticationEntryPoint() {
    	if (!isKerberosEnabled()) {
    		// We do not any extra logic is SSO is not configured
    		return authenticationEntryPoint;
    	}
    	
    	// Create a map of specific matchers to specific entry points
    	LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        
        // Add SPNEGO for the SSO endpoint
        if (isKerberosEnabled()) {
            log.debug("Mapping SpnegoEntryPoint to {}", AUTHENTICATE_SSO);
            entryPoints.put(new AntPathRequestMatcher(AUTHENTICATE_SSO), spnegoEntryPoint());
        }

        // Create the delegator. The second argument is the DEFAULT entry point 
        // if no matchers above are hit.
        DelegatingAuthenticationEntryPoint delegator = 
                new DelegatingAuthenticationEntryPoint(entryPoints);
        
        // This is default entry point
        delegator.setDefaultEntryPoint(authenticationEntryPoint); 
        
        return delegator;
    }

	private void configureOAuth2(HttpSecurity http) throws Exception {
        if (!optionalOAuth2Props.isPresent()) {
            return;
        }

        http
            // enable resource server & JWT processing
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(getjwtDecoder())))

            // set own authentication manager
            //  - allows to set JwtUserDetailProvider as a AutheticationProvider for JWT
            .authenticationManager(authenticationManagerBean());

        log.info("OAuth2 auto-user mapping filter was configured");
    }

    private void configureSsoHeaderFilter(HttpSecurity http) throws Exception {
        if (!optionalSsoHeaderProperties.isPresent()) {
        	return;
        }

        SsoHeaderAuthenticationFilter filter = new SsoHeaderAuthenticationFilter(optionalSsoHeaderProperties.get());
        filter.setAuthenticationManager(authenticationManagerBean());
        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(authenticationFailureHandler);

        http
        	.addFilterBefore(filter, AbstractPreAuthenticatedProcessingFilter.class);

        log.info("SSO header authentication filter was configured");
    }

	private void configureKerberos(HttpSecurity http) throws Exception {
        if (!optionalKerberosProps.isPresent()) {
            return;
        }
        log.debug("Configuring Kerberos filter.");

        /*ProviderManager providerManager = new ProviderManager(kerberosAuthenticationProvider(), kerberosServiceAuthenticationProvider());*/
        SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
                
        filter.setAuthenticationManager(authenticationManagerBean());
        filter.setSuccessHandler( kerberosSuccessHandler() );
        filter.setFailureHandler(authenticationFailureHandler);
        
        http
           //	.authenticationProvider(kerberosServiceAuthenticationProvider())
        	.addFilterBefore(filter, BasicAuthenticationFilter.class)
        ;

        log.info("Kerberos authentication filter was configured.");
	}
		
	/**
	 * Success handler specifically for Kerberos browser-based SSO.
	 */
	private AuthenticationSuccessHandler kerberosSuccessHandler() {
	    SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler() {
	        @Override
	        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
	                                            Authentication authentication) throws IOException, ServletException {
	            
	            SavedRequest savedRequest = (SavedRequest) request.getSession()
	                    .getAttribute("SPRING_SECURITY_SAVED_REQUEST");

	            // If the user was trying to go to the SSO URL itself, ignore it and go to root
	            if (savedRequest != null && savedRequest.getRedirectUrl().contains(AUTHENTICATE_SSO)) {
	                log.debug("Detected redirect loop to SSO URL, clearing saved request.");
	                request.getSession().removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
	            }
	            
	            super.onAuthenticationSuccess(request, response, authentication);
	        }
	    };
	    handler.setDefaultTargetUrl("/"); // This is where they go if no saved request exists
	    return handler;
	}	
	// Kerberos authentication
	// Requires three beans
	// - KerberosAuthenticationProvider
	// - SunJaasKerberosTicketValidator
	
    @Bean
    @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
    	var kerberosPros = optionalKerberosProps.get();
    	
    	log.debug("Creating SunJaasKerberosTicketValidator for principal: {}, keytab: {}.",
    			kerberosPros.getServicePrincipal(), kerberosPros.getKeytabLocation());
    	
    	/**
    	 * Basic implementation of ticket validator in Kerberos
    	 */
        SunJaasKerberosTicketValidator ticketValidator = new SunJaasKerberosTicketValidator();
        ticketValidator.setServicePrincipal(kerberosPros.getServicePrincipal());
        // Should be domain set here?
        //ticketValidator.setRealmName(kerberosPros.getRealmName());
        
        // 
        ticketValidator.setKeyTabLocation(new FileSystemResource(kerberosPros.getKeytabLocation()));    
        ticketValidator.setDebug(kerberosPros.isTicketValidatorDebug());
        return ticketValidator;
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
    SpnegoEntryPoint spnegoEntryPoint() {
    	var kerberosPros = optionalKerberosProps.get();
		
		log.debug("Creating SpnegoEntryPoint for principal: {}, keytab: {}.",
				kerberosPros.getServicePrincipal(), kerberosPros.getKeytabLocation());
    	return new SpnegoEntryPoint();
    }
    
    /**
     * Return true if Kerberos is enabled
     * @return
     */
    public boolean isKerberosEnabled() { 
    	return optionalKerberosProps.isPresent() && optionalKerberosProps.get().getServicePrincipal() != null; 
    }
    
    /*
    private RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        if (optionalKerberosProps.isPresent() && optionalKerberosProps.get().getServicePrincipal() != null) {
        	// Prevent saving the SSO trigger URL so we don't redirect back to it
        	requestCache.setRequestMatcher(new NegatedRequestMatcher(
                new AntPathRequestMatcher(AUTHENTICATE_SSO)
        			));
        }
        return requestCache;
    }*/   
}
