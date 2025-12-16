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

import cz.tacr.elza.security.kerberos.KerberosProperties;
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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.client.config.SunJaasKrb5LoginConfig;
import org.springframework.security.kerberos.client.ldap.KerberosLdapContextSource;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.session.HttpSessionEventPublisher;
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

    /**
     * These patterns need to be allowed to access without authorization
     * to make it possible to navigate from the browser address bar for unauthorized users
     * @see cz.tacr.elza.web.controller.ElzaWebController (elza-web)
     */
    public static final String[] PERMIT_ALL_PATTERNS = {"/", "/res/**", "/static/**", "/fund/**", "/node/**", "/entity/**", "/admin/**", "/h2-console/**"};

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
            ap.add(new SsoHeaderAuthenticationProvider(userService, txManager));
        }
        if (optionalOAuth2Props.isPresent()) {
        	log.debug("Adding JWT based provider.");
            JwtDecoder jwtDecoder = applicationContext.getBean(JwtDecoder.class);
            AccessPointService apService = applicationContext.getBean(AccessPointService.class);

            ap.add(new JwtUserDetailProvider(jwtDecoder, txManager, userService, apService,
                    itemTypeRepository, optionalOAuth2Props.get()));
        }
        if(optionalLdapProps.isPresent()) {
			if (optionalLdapProps.get().getAdDomain() != null) {
				log.debug("Adding ActiveDirectory provider, domain: {}, server: {}.",
						optionalLdapProps.get().getAdDomain(), optionalLdapProps.get().getAdServer());
				// adding active directory domain
				var adProvider = new ActiveDirectoryUserDetailProvider(optionalLdapProps.get(), txManager, userService);
				
				ap.add(adProvider);
			}
        }
        if(optionalKerberosProps.isPresent()) {
			// adding default Kerberos provider
			log.debug("Adding generic Kerberos Authentication provider.");
			ap.add(kerberosAuthenticationProvider());
			
			log.debug("Adding generic Kerberos Service Authentication provider.");
			ap.add(kerberosServiceAuthenticationProvider());
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
    @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
	public KerberosLdapContextSource kerberosLdapContextSource() throws Exception {
		log.debug("Creating KerberosLdapContextSource for principal: {}", optionalKerberosProps.get().getServicePrincipal());
		
		SunJaasKrb5LoginConfig loginConfig = new SunJaasKrb5LoginConfig();
		loginConfig.setKeyTabLocation(new FileSystemResource(optionalKerberosProps.get().getKeytabLocation()));
		loginConfig.setServicePrincipal(optionalKerberosProps.get().getServicePrincipal());
		loginConfig.setDebug(true);
		loginConfig.setIsInitiator(true);
		loginConfig.afterPropertiesSet();

		KerberosLdapContextSource contextSource = new KerberosLdapContextSource(optionalLdapProps.get().getAdServer());
		contextSource.setLoginConfig(loginConfig);
		return contextSource;
	}

	@Bean
	@ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
	public LdapUserDetailsService ldapUserDetailsService() throws Exception {
		log.debug("Creating LdapUserDetailsService.");
		
		FilterBasedLdapUserSearch userSearch =
				new FilterBasedLdapUserSearch(optionalKerberosProps.get().getLdapSearchBase() , 
						optionalKerberosProps.get().getLdapSearchFilter(), kerberosLdapContextSource());
		LdapUserDetailsService service =
				new LdapUserDetailsService(userSearch, new NullLdapAuthoritiesPopulator());
		service.setUserDetailsMapper(new LdapUserDetailsMapper());
		return service;
	}
	
	@Bean
    public HttpFirewall strictHttpFirewall() {
    	// This allows to accept some Czech characters as header values when using SSO header
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // allow all header values
        firewall.setAllowedHeaderValues(v -> true);
        return firewall;
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
    				.anyRequest().authenticated())
    		.httpBasic(Customizer.withDefaults())

        	.sessionManagement(session -> session
                .maximumSessions(10)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry()))

        	.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))

            .formLogin(formLogin -> formLogin
           		.successHandler(authenticationSuccessHandler)
        		.failureHandler(authenticationFailureHandler))

        	.logout(logout -> logout.permitAll().logoutSuccessHandler(apiLogoutSuccessHandler));

        configureSsoHeaderFilter(http);
        configureOAuth2(http);
        configureKerberos(http);
        return http.build();
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

	private void configureKerberos(HttpSecurity http) {
        if (!optionalKerberosProps.isPresent()) {
            return;
        }
        log.debug("Configuring Kerberos filter.");

        /*ProviderManager providerManager = new ProviderManager(kerberosAuthenticationProvider(), kerberosServiceAuthenticationProvider());
        SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter = new SpnegoAuthenticationProcessingFilter();
        spnegoAuthenticationProcessingFilter.setAuthenticationManager(providerManager);*/
        
        //http
        //	.authenticationProvider(kerberosServiceAuthenticationProvider())
        	//.addFilterBefore(spnegoAuthenticationProcessingFilter, BasicAuthenticationFilter.class)
        //	;

        log.info("Kerberos authentication filter was configured.");
	}
	
    @Bean
    @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        // Configure native JRE Kerberos client
        SunJaasKerberosClient client = new SunJaasKerberosClient();
        client.setDebug(optionalKerberosProps.get().isKerberosClientDebug());
        
        // Prepare authentication provider based on default Kerberos client
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        provider.setKerberosClient(client);
        provider.setUserDetailsService(userDetailsService());
        return provider;
    }

    @Bean
    @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator());
        provider.setUserDetailsService(userDetailsService());
        return provider;
    }

    @Bean
    @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
    	var kerberosPros = optionalKerberosProps.get();
    	
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

    // @Bean
    // @ConditionalOnProperty(prefix = "elza.security.kerberos", name = "service-principal")
    public UserDetailsService userDetailsService() {
    	return new UserDetailsService() {

			@Override
			public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				return userService.loadUserByUsername(username);
			}
    		
    	};
    	//return userService;
    }
}
