package cz.tacr.elza.security.oauth2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import cz.tacr.elza.controller.vo.ApPartFormVO;
import cz.tacr.elza.controller.vo.ap.item.ApItemStringVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrPermission.PermissionType;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.oauth2.OAuth2Properties.PermProperties;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.UserService;
import liquibase.util.StringUtils;

public class JwtUserDetailProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtUserDetailProvider.class);

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter = new JwtAuthenticationConverter();

    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    private final AccessPointService apService;

    private final PlatformTransactionManager txManager;
    private final ItemTypeRepository itemTypeRepository;

    private final OAuth2Properties oAuth2Properties;

    /**
     * Cache pro podrobnosti uživatele.
     */
    private final LoadingCache<String, UserDetail> userDetailCache;

    private final int USER_DETAIL_CACHE_LIFETIME = 5; // TimeUnit.MINUTES

    private final String CLAIM_SUB = "sub";
    private final String CLAIM_NAME = "name";
    private final String CLAIM_AUTHORITIES = "authorities";

    private final String JWT_USERS = "JWT_USERS";
    private final String PERSON_INDIVIDUAL = "PERSON_INDIVIDUAL";

    private final String NM_MAIN = "NM_MAIN";
    private final String NM_SUP_PRIV = "NM_SUP_PRIV";
    
    public JwtUserDetailProvider(final JwtDecoder jwtDecoder,
                                 final PlatformTransactionManager txManager,
                                 final UserService userService,
                                 final AccessPointService apService,
                                 final ItemTypeRepository itemTypeRepository,
                                 final OAuth2Properties oAuth2Properties) {
        this.jwtDecoder = jwtDecoder;
        this.txManager = txManager;
        this.userService = userService;
        this.apService = apService;
        this.itemTypeRepository = itemTypeRepository;
        this.oAuth2Properties = oAuth2Properties;
        this.userDetailCache = CacheBuilder.newBuilder()
                .expireAfterWrite(USER_DETAIL_CACHE_LIFETIME, TimeUnit.MINUTES)
                .build(new CacheLoader<String, UserDetail>() {
                    @Override
                    public UserDetail load(final String name) {
                        return null;
                    }
                });
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;

        Jwt jwt;
        try {
            jwt = this.jwtDecoder.decode(bearer.getToken());
        } catch (BadJwtException failed) {
            throw new InvalidBearerTokenException(failed.getMessage(), failed);
        } catch (JwtException failed) {
            throw new AuthenticationServiceException(failed.getMessage(), failed);
        }

        AbstractAuthenticationToken token = this.jwtAuthenticationConverter.convert(jwt);

        Object details = prepareDetails(jwt);
        token.setDetails(details);

        // Object details = bearer.getDetails();
        // token.setDetails(bearer.getDetails());

        return token;
    }

    synchronized private Object prepareDetails(Jwt jwt) {
    	String sub = jwt.getClaimAsString(CLAIM_SUB);
    	UserDetail result = userDetailCache.getIfPresent(sub);
    	if (result == null) {
    		result = createUserDetail(jwt);
    		userDetailCache.put(sub, result);
    	}
    	return result;
    }

    private UserDetail createUserDetail(Jwt jwt) {
        final String sub = jwt.getClaimAsString(CLAIM_SUB);
        final String name = jwt.getClaimAsString(CLAIM_NAME);
        final List<String> authorities = jwt.getClaimAsStringList(CLAIM_AUTHORITIES);

        log.debug("Preparing details fot JWT, headers: {}, claims: {}, sub: {}, name: {}, authorities: {}",
                jwt.getHeaders(), jwt.getClaims(),
                sub, name, authorities);

        return new TransactionTemplate(txManager).execute(r -> {
        	UsrUser user = userService.findByUsername(sub);
            // prepare temporary credentials
            SecurityContext prevSecCtx = SecurityContextHolder.getContext();
            SecurityContext secCtx = userService.createSecurityContextSystem();
            SecurityContextHolder.setContext(secCtx);

            if (user == null) {
                user = createJWTUser(sub, name);
            }

            // prepare permissions and synchronize permissions
            updatePermissions(authorities, user);

            log.debug("Permissions for user '{}' are synchronized.", sub);

            // return back previous context
            SecurityContextHolder.setContext(prevSecCtx);

            return userService.createUserDetail(user);
        });
    }

    private UsrUser createJWTUser(String sub, String name) {
        ApScope userScope = apService.getApScope(JWT_USERS);
        ApType type = apService.getType(PERSON_INDIVIDUAL);

        ApPartFormVO pf = createPrefName(name, sub);
        // create person
        ApState ap = apService.createAccessPoint(userScope, type, pf);
        UsrUser user = userService.createUser(sub, null, ap.getAccessPointId());

        log.debug("Created new user from JWT: {}", sub);
        return user;
    }

    private ApPartFormVO createPrefName(String name, String sub) {

        RulItemType itemTypeNmMain = itemTypeRepository.findOneByCode(NM_MAIN);
        RulItemType itemTypeNmInternal = itemTypeRepository.findOneByCode(NM_SUP_PRIV);

        ApPartFormVO pf = new ApPartFormVO();
        pf.setPartTypeCode(StaticDataProvider.DEFAULT_PART_TYPE);
        ApItemStringVO nmMainVo = new ApItemStringVO();
        nmMainVo.setTypeId(itemTypeNmMain.getItemTypeId());
        nmMainVo.setValue(name);
        pf.getItems().add(nmMainVo);
        ApItemStringVO nmPrivVo = new ApItemStringVO();
        nmPrivVo.setTypeId(itemTypeNmInternal.getItemTypeId());
        nmPrivVo.setValue(sub);
        pf.getItems().add(nmPrivVo);

        return pf;
    }

    private void updatePermissions(List<String> authorities, UsrUser user) {
        List<UsrPermission> perms;
        if (CollectionUtils.isEmpty(authorities)) {
            perms = Collections.emptyList();
        } else {
            perms = new ArrayList<>();
            for (String authority : authorities) {
                preparePermissions(authority, perms);
            }
        }

        userService.changeUserPermission(user, perms);
    }

    private void preparePermissions(String authority, List<UsrPermission> perms) {
        List<PermProperties> permProps = this.oAuth2Properties.getPermissions();
        if (CollectionUtils.isEmpty(permProps)) {
            return;
        }
        for (PermProperties permProp : permProps) {
            if (Objects.equals(authority, permProp.getAuthority())) {
                // we have match
                preparePermissions(permProp, perms);
            }

        }

    }

    private void preparePermissions(PermProperties permProp, List<UsrPermission> perms) {
        if (CollectionUtils.isEmpty(permProp.getPermissions())) {
            return;
        }

        ApScope scope = null;

        if (StringUtils.isNotEmpty(permProp.getScope())) {
            // read scope
            scope = apService.getApScope(permProp.getScope());
        }

        for (String perm : permProp.getPermissions()) {
            Permission permission = Permission.valueOf(perm);

            UsrPermission permObj = new UsrPermission();
            permObj.setPermission(permission);

            if (permission.getType() == PermissionType.SCOPE) {
                permObj.setScope(scope);
            }

            addPermission(permObj, perms);
        }

    }

    private void addPermission(UsrPermission permObj, List<UsrPermission> perms) {
        // check if not duplicated
        for (UsrPermission currPerm : perms) {
            if (permObj.getPermission().equals(currPerm.getPermission())) {
                return;
            }
        }
        perms.add(permObj);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
