package cz.tacr.elza.core.security;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import cz.tacr.elza.api.interfaces.ArrFundGetter;
import cz.tacr.elza.api.interfaces.IApAccessPoint;
import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.api.interfaces.IWfIssueList;
import cz.tacr.elza.core.security.AuthParam.Type;
import cz.tacr.elza.core.security.Authorization.MethodParamBasedAccess.PermissionResult;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.repository.WfIssueListRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;

/**
 * Kontrola oprávnění přes AOP.
 *
 */
@Aspect
@Component
public class Authorization {

    static private Logger logger = LoggerFactory.getLogger(Authorization.class);

	/**
	 * Brief method info
	 *
	 *
	 */
	class MethodInfo {
		MethodSignature methodSignature;
		Method method;
		Parameter[] parameters;
		Object[] pjpArgs;

		public MethodInfo(ProceedingJoinPoint pjp) {
			this.methodSignature = (MethodSignature) pjp.getSignature();
			this.method = methodSignature.getMethod();

			this.parameters = method.getParameters();
			this.pjpArgs = pjp.getArgs();
		}

		Method getMethod() {
			return method;
		}

		Parameter[] getParameters() {
			return parameters;
		}

		public Object getPjpArg(int i) {
			return pjpArgs[i];
		}
	}

	/**
	 * Interface to check access based on annotaded method parameter
	 *
	 */
	interface MethodParamBasedAccess {

		enum PermissionResult {
			/**
			 * Used when permission cannot be checked with this parameter
			 */
			NOT_CHECKED,
			GRANT_ACCESS,
			DENY_ACCESS
		}

		/**
		 * Check if permision can be applied
		 * @param authParam
		 * @param parameterValue
		 * @return
		 */
		PermissionResult checkPermission(AuthParam authParam, Object parameterValue);

	}

    @Autowired
    private UserService userService;

	@Autowired
	private UserRepository userRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
	private ApStateRepository stateRepository;

    @Autowired
    private WfIssueListRepository issueListRepository;

    @Around("execution(* cz.tacr.elza..*.*(..)) && @annotation(cz.tacr.elza.core.security.AuthMethod)")
	public Object auth(final ProceedingJoinPoint pjp) throws Throwable {

		MethodInfo methodInfo = new MethodInfo(pjp);

		AuthMethod declaredAnnotation = methodInfo.getMethod().getDeclaredAnnotation(AuthMethod.class);

		UserDetail userDetail = userService.getLoggedUserDetail();
        if (userDetail == null) {
            // user is not logged
            logger.error("User is not logged, no security context");
            throw createAccessDeniedException(declaredAnnotation.permission());
        }

		for (UsrPermission.Permission permission : declaredAnnotation.permission()) {

			boolean hasPermission = false;

			if(permission==UsrPermission.Permission.USER_CONTROL_ENTITITY) {
				hasPermission = checkControlEntityPermission(methodInfo, userDetail);
			} else if (permission == UsrPermission.Permission.GROUP_CONTROL_ENTITITY) {
				hasPermission = checkControlGroupPermission(methodInfo, userDetail);
			} else {
				// type based permission checker
				switch (permission.getType()) {
				case ALL:
					if (userDetail.hasPermission(permission)) {
						hasPermission = true;
					}
					break;
				case SCOPE:
					// permissions for scope
					hasPermission = checkScopePermission(permission, methodInfo, userDetail);
					break;
				case FUND:
					hasPermission = checkFundPermission(permission, methodInfo, userDetail);
					break;
				case NODE:
					hasPermission = checkNodePermission(permission, methodInfo, userDetail);
					break;
				case ISSUE_LIST:
					hasPermission = checkIssueListPermission(permission, methodInfo, userDetail);
					break;
				default:
					throw new IllegalStateException("Permission type not defined: " + permission.getType());
				}
			}

			if (hasPermission) {
				return pjp.proceed();
			}
		}

		throw createAccessDeniedException(declaredAnnotation.permission());
	}

    /**
     * Iterate all method parameters and try to get access
     *
     * @param methodInfo
     * @param methodChecker
     * @return
     */
    private boolean hasPermission(MethodInfo methodInfo, MethodParamBasedAccess methodChecker) {
        Parameter[] params = methodInfo.getParameters();

        // number of applied parameters
        int appliedParams = 0;

        // permssions for fund
        for (int i = 0; i < params.length; i++) {
            Parameter parameter = params[i];
            Object parameterValue = methodInfo.getPjpArg(i);
            AuthParam[] authParams = parameter.getAnnotationsByType(AuthParam.class);
            for (AuthParam authParam : authParams) {
                switch(methodChecker.checkPermission(authParam, parameterValue)) {
                case GRANT_ACCESS:
                    return true;
                case DENY_ACCESS:
                	appliedParams++;
                	break;
				case NOT_CHECKED:
					break;
				default:
					break;
                }
            }
        }

        // check if permissions where checked with at least one parameter
        if(appliedParams==0) {
        	throw new SystemException("Failed to check permissions, incorrect configuration, method: "
        			+ methodInfo.getMethod().toString());
        }
        return false;
    }

    private boolean checkControlGroupPermission(MethodInfo methodInfo, UserDetail userDetail) {
		Integer userId = userDetail.getId();
		if (userId == null) {
			// if user is null -> it is admin
			return true;
		}

		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			Integer groupId = loadGroupId(parameterValue, authParam.type());
			if (userDetail.hasPermission(UsrPermission.Permission.GROUP_CONTROL_ENTITITY, groupId)) {
				return PermissionResult.GRANT_ACCESS;
			}
			List<Integer> perms = userRepository.findPermissionAllowingGroupAccess(userId, groupId);
			if (CollectionUtils.isNotEmpty(perms)) {
				return PermissionResult.GRANT_ACCESS;
			}
			return PermissionResult.DENY_ACCESS;
		});
	}

	private Integer loadGroupId(Object value, Type type) {
		switch (type) {
		case GROUP:
			if (value instanceof Integer) {
				return (Integer) value;
			} else if (value instanceof UsrGroup) {
				return ((UsrGroup) value).getGroupId();
			}
			break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	/**
	 * Check if logged user can manage entity in parameter
	 *
	 * @param methodInfo
	 * @param userDetail
	 * @return
	 */
	private boolean checkControlEntityPermission(MethodInfo methodInfo, UserDetail userDetail) {
		Integer userId = userDetail.getId();
		if (userId == null) {
			// if user is null -> it is admin
			return true;
		}
		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			Integer entityId = loadUserId(parameterValue, authParam.type());
			if (userDetail.hasPermission(UsrPermission.Permission.USER_CONTROL_ENTITITY, entityId)) {
				return PermissionResult.GRANT_ACCESS;
			}
			List<Integer> perms = userRepository.findPermissionAllowingUserAccess(userId, entityId);
			if (CollectionUtils.isNotEmpty(perms)) {
				return PermissionResult.GRANT_ACCESS;
			}
			return PermissionResult.DENY_ACCESS;
		});
	}

	private int loadUserId(Object value, Type type) {
		switch (type) {
		case USER:
			if (value instanceof Integer) {
				return (Integer) value;
			} else if (value instanceof UsrUser) {
				return ((UsrUser) value).getUserId();
			}
			break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	private boolean checkFundPermission(Permission permission, MethodInfo methodInfo, UserDetail userDetail) {
		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			Integer entityId = loadFundId(parameterValue, authParam.type());
			if (userDetail.hasPermission(permission, entityId)) {
				return PermissionResult.GRANT_ACCESS;
			}
			return PermissionResult.DENY_ACCESS;
		});
	}

	private boolean checkNodePermission(final Permission permission, final MethodInfo methodInfo, final UserDetail userDetail) {
		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			if (Type.NODE == authParam.type()) {
				Integer entityId = loadNodeId(parameterValue, authParam.type());
				if (userDetail.hasPermission(permission, entityId)) {
					return PermissionResult.GRANT_ACCESS;
				}
				return PermissionResult.DENY_ACCESS;
			} else {
				return PermissionResult.DENY_ACCESS;
			}
		});
	}

	/**
	 * Check permissions for scope
	 *
	 * @param permission
	 * @param methodInfo
	 * @param userDetail
	 * @return
	 */
	private boolean checkScopePermission(Permission permission, MethodInfo methodInfo, UserDetail userDetail) {
		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			Integer entityId = loadScopeId(parameterValue, authParam.type());
			if (userDetail.hasPermission(permission, entityId)) {
				return PermissionResult.GRANT_ACCESS;
			}
			return PermissionResult.DENY_ACCESS;
		});
	}

	private boolean checkIssueListPermission(Permission permission, MethodInfo methodInfo, UserDetail userDetail) {
		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			Integer entityId = loadIssueListId(parameterValue, authParam.type());
			if (userDetail.hasPermission(permission, entityId)) {
				return PermissionResult.GRANT_ACCESS;
			}
			return PermissionResult.DENY_ACCESS;
		});
	}

	/**
	 * Prapare scope id
	 *
	 * @param value
	 * @param type
	 * @return
	 */
	private Integer loadScopeId(final Object value, final AuthParam.Type type) {
		if (value instanceof IApScope) {
			return ((IApScope) value).getScopeId();
		}
		if (value instanceof IApAccessPoint) {
			return getScopeIdAccessPointId(((IApAccessPoint) value).getAccessPointId());
		}
		switch (type) {
		case SCOPE:
			if (value instanceof Integer) {
				return (Integer) value;
			}
			break;
		case AP:
			if (value instanceof Integer) {
				ApAccessPoint accessPoint = accessPointRepository.getOneCheckExist((Integer) value);
				return getScopeIdAccessPointId(accessPoint.getAccessPointId());
			}
			break;
		case AP_STATE:
			if (value instanceof Integer) {
				return stateRepository.getOneCheckExist((Integer) value).getScopeId();
			}
			break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	private Integer getScopeIdAccessPointId(Integer accessPointId) {
		ApState apState = stateRepository.findLastByAccessPointId(accessPointId);
		if (apState == null) {
			throw new ObjectNotFoundException("Stav pro přístupový bod neexistuje", BaseCode.INVALID_STATE)
					.set("accessPointId", accessPointId);
		}
		return apState.getScopeId();
	}

	/**
	 * Load fund id
	 *
	 * @param value
	 *            vstupní objekt
	 * @param type
	 *            typ vstupního parametru
	 * @return identfikátor entity
	 */
	private Integer loadFundId(final Object value, final AuthParam.Type type) {
		switch (type) {
		case FUND:
			if (value instanceof Integer) {
				return (Integer) value;
            } else if (value instanceof ArrFundGetter) {
                return ((ArrFundGetter) value).getFund().getFundId();
			}
			break;
		case FUND_VERSION:
			if (value instanceof Integer) {
                return fundVersionRepository.getOneCheckExist((Integer) value).getFundId();
            } else if (value instanceof ArrFundGetter) {
                return ((ArrFundGetter) value).getFund().getFundId();
			}
			break;
		case ISSUE_LIST:
			if (value instanceof Integer) {
				return issueListRepository.findFundIdByIssueListId((Integer) value);
            } else if (value instanceof ArrFundGetter) {
                return ((ArrFundGetter) value).getFund().getFundId();
			}
			break;
		case ISSUE:
			if (value instanceof Integer) {
				return issueListRepository.findFundIdByIssueId((Integer) value);
            } else if (value instanceof ArrFundGetter) {
                return ((ArrFundGetter) value).getFund().getFundId();
			}
			break;
		case COMMENT:
			if (value instanceof Integer) {
				return issueListRepository.findFundIdByCommentId((Integer) value);
			} else if (value instanceof IWfIssueList) {
				return issueListRepository.findFundIdByIssueListId(((IWfIssueList) value).getIssueListId());
			}
			break;
		case NODE:
			return null;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	private Integer loadNodeId(final Object value, final AuthParam.Type type) {
		switch (type) {
			case NODE:
				if (value instanceof Integer) {
					return (Integer) value;
				} else if (value instanceof ArrNode) {
					return ((ArrNode) value).getNodeId();
				}
				break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	/**
	 * Load issueList id
	 *
	 * @param value vstupní objekt
	 * @param type typ vstupního parametru
	 * @return identfikátor entity
	 */
	private Integer loadIssueListId(final Object value, final AuthParam.Type type) {
		switch (type) {
		case ISSUE_LIST:
			if (value instanceof Integer) {
				return (Integer) value;
			} else if (value instanceof IWfIssueList) {
				return ((IWfIssueList) value).getIssueListId();
			}
			break;
		case ISSUE:
			if (value instanceof Integer) {
				return issueListRepository.findIdByIssueId((Integer) value);
			} else if (value instanceof IWfIssueList) {
				return ((IWfIssueList) value).getIssueListId();
			}
			break;
		case COMMENT:
			if (value instanceof Integer) {
				return issueListRepository.findIdByCommentId((Integer) value);
			} else if (value instanceof IWfIssueList) {
				return ((IWfIssueList) value).getIssueListId();
			}
			break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	public static AccessDeniedException createAccessDeniedException(Permission... deniedPermissions) {
		return new AccessDeniedException("Chybějící oprávnění: " + Arrays.toString(deniedPermissions), deniedPermissions);
	}

	/**
	 * Creates runnable with current security context.
	 *
	 * @param runnable original runnable
	 */
	public static Runnable createRunnableWithCurrentSecurity(Runnable runnable) {
	    SecurityContext ctx = SecurityContextHolder.getContext();

	    // create copy of security context, see SEC-2025
        Authentication auth = ctx.getAuthentication();
        ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);

	    return new DelegatingSecurityContextRunnable(runnable, ctx);
	}

    public static <T> Callable<T> createCallableWithCurrentSecurity(Callable<T> param) {
        SecurityContext ctx = SecurityContextHolder.getContext();

        // create copy of security context, see SEC-2025
        Authentication auth = ctx.getAuthentication();
        ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);

        return DelegatingSecurityContextCallable.create(param, ctx);
    }
}
