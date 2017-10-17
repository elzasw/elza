package cz.tacr.elza.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.SystemException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.annotation.AuthMethod;
import cz.tacr.elza.annotation.AuthParam;
import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.api.interfaces.IRegScope;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.UserService;

/**
 * Kontrola oprávnění přes AOP.
 *
 * @author Martin Šlapa
 * @since 17.04.2016
 */
@Aspect
@Component
public class Authorization {

    @Autowired
    private UserService userService;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Around("execution(* cz.tacr.elza..*.*(..)) && @annotation(cz.tacr.elza.annotation.AuthMethod)")
    public Object auth(final ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature methodSignature = (MethodSignature)pjp.getSignature();
        Method method = methodSignature.getMethod();
        AuthMethod declaredAnnotation = method.getDeclaredAnnotation(AuthMethod.class);

        if (userService.hasPermission(UsrPermission.Permission.ADMIN)) {
            return pjp.proceed();
        }

        boolean hasPermission = false;
        for (UsrPermission.Permission permission : declaredAnnotation.permission()) {

            Parameter[] parameters = method.getParameters();
            Object[] pjpArgs = pjp.getArgs();

            switch (permission.getType()) {
                case ALL:
                    if (userService.hasPermission(permission)) {
                        hasPermission = true;
                    }
                    break;
                case USER:
                case GROUP:
                case SCOPE:
                case FUND:
                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];
                        Object parameterValue = pjpArgs[i];
                        AuthParam[] authParams = parameter.getAnnotationsByType(AuthParam.class);
                        for (AuthParam authParam : authParams) {
                            Integer entityId = loadEntityId(parameterValue, authParam.type());
                            if (userService.hasPermission(permission, entityId)) {
                                hasPermission = true;
                            }
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Permission type not defined: " + permission.getType());
            }

            if (hasPermission) {
                return pjp.proceed();
            }
        }

        throw createAccessDeniedException(declaredAnnotation.permission());
    }

    /**
     * Načtení entity podle vstupního objektu.
     *
     * @param value vstupní objekt
     * @param type  typ vstupního parametru
     * @return  identfikátor entity
     */
    private Integer loadEntityId(final Object value, final AuthParam.Type type) {
        switch (type) {
            case FUND: {
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof IArrFund) {
                    return ((IArrFund) value).getFund().getFundId();
                }
            }
            case FUND_VERSION: {
                if (value instanceof Integer) {
                    return fundVersionRepository.getOneCheckExist((Integer) value).getFund().getFundId();
                } else if (value instanceof IArrFund) {
                    return ((IArrFund) value).getFund().getFundId();
                }
            }
            case SCOPE: {
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof IRegScope) {
                    return ((IRegScope) value).getRegScope().getScopeId();
                }
            }
            case PARTY: {
                if (value instanceof Integer) {
                    return partyRepository.getOneCheckExist((Integer) value).getRegScope().getScopeId();
                } else if (value instanceof IRegScope) {
                    return ((IRegScope) value).getRegScope().getScopeId();
                }
            }
            case REGISTRY: {
                if (value instanceof Integer) {
                    return recordRepository.getOneCheckExist((Integer) value).getRegScope().getScopeId();
                } else if (value instanceof IRegScope) {
                    return ((IRegScope) value).getRegScope().getScopeId();
                }
            }
            case USER: {
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof UsrUser) {
                    return ((UsrUser) value).getUserId();
                }
            }
            case GROUP: {
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof UsrGroup) {
                    return ((UsrGroup) value).getGroupId();
                }
            }
            default:
                throw new IllegalStateException(type + ":" + value.getClass().getName());
        }
    }

	public static AccessDeniedException createAccessDeniedException(Permission... deniedPermissions) {
		return new AccessDeniedException("Chybějící oprávnění: " + Arrays.toString(deniedPermissions), deniedPermissions);
	}
}
