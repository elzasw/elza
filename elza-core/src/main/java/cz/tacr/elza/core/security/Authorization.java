package cz.tacr.elza.core.security;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.api.interfaces.IRegScope;
import cz.tacr.elza.domain.UsrGroup;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.exception.AccessDeniedException;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.UserRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.service.UserService;

/**
 * Kontrola oprávnění přes AOP.
 *
 */
@Aspect
@Component
public class Authorization {

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

		boolean hasPermission(AuthParam authParam, Object parameterValue);

	}

    @Autowired
    private UserService userService;

	@Autowired
	private UserRepository userRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RegRecordRepository recordRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Around("execution(* cz.tacr.elza..*.*(..)) && @annotation(cz.tacr.elza.core.security.AuthMethod)")
	public Object auth(final ProceedingJoinPoint pjp) throws Throwable {

		MethodInfo methodInfo = new MethodInfo(pjp);

		AuthMethod declaredAnnotation = methodInfo.getMethod().getDeclaredAnnotation(AuthMethod.class);

		UserDetail userDetail = userService.getLoggedUserDetail();

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

     * Načtení entity podle vstupního objektu.
     *
     * @param value vstupní objekt
     * @param type  typ vstupního parametru
     * @return  identfikátor entity
	private boolean hasPermission(MethodInfo methodInfo, MethodParamBasedAccess methodChecker) {
		Parameter[] params = methodInfo.getParameters();
		// permssions for fund
		for (int i = 0; i < params.length; i++) {
			Parameter parameter = params[i];
			Object parameterValue = methodInfo.getPjpArg(i);
			AuthParam[] authParams = parameter.getAnnotationsByType(AuthParam.class);
			for (AuthParam authParam : authParams) {
				if (methodChecker.hasPermission(authParam, parameterValue)) {
					return true;
				}
			}
		}
		return false;
	}
     */
	private boolean checkControlGroupPermission(MethodInfo methodInfo, UserDetail userDetail) {
		Integer userId = userDetail.getId();
		if (userId == null) {
			// if user is null -> it is admin
			return true;
		}

		return hasPermission(methodInfo, (authParam, parameterValue) -> {
			Integer groupId = loadGroupId(parameterValue, authParam.type());
			if (userDetail.hasPermission(UsrPermission.Permission.GROUP_CONTROL_ENTITITY, groupId)) {
				return true;
			}
			List<Integer> perms = userRepository.findPermissionAllowingGroupAccess(userId, groupId);
			if (CollectionUtils.isNotEmpty(perms)) {
				return true;
			}
			return false;
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
	 * @param permission
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
				return true;
			}
			List<Integer> perms = userRepository.findPermissionAllowingUserAccess(userId, entityId);
			if (CollectionUtils.isNotEmpty(perms)) {
				return true;
			}
			return false;
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
				return true;
			}
			return false;
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
				return true;
			}
			return false;
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
		switch (type) {
		case SCOPE:
			if (value instanceof Integer) {
				return (Integer) value;
			} else if (value instanceof IRegScope) {
				return ((IRegScope) value).getRegScope().getScopeId();
			}
			break;
		case PARTY:
			if (value instanceof Integer) {
				return partyRepository.getOneCheckExist((Integer) value).getRegScope().getScopeId();
			} else if (value instanceof IRegScope) {
				return ((IRegScope) value).getRegScope().getScopeId();
			}
			break;
		case REGISTRY:
			if (value instanceof Integer) {
				return recordRepository.getOneCheckExist((Integer) value).getRegScope().getScopeId();
			} else if (value instanceof IRegScope) {
				return ((IRegScope) value).getRegScope().getScopeId();
			}
			break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
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
			} else if (value instanceof IArrFund) {
				return ((IArrFund) value).getFund().getFundId();
			}
			break;
		case FUND_VERSION:
			if (value instanceof Integer) {
				return fundVersionRepository.getOneCheckExist((Integer) value).getFund().getFundId();
			} else if (value instanceof IArrFund) {
				return ((IArrFund) value).getFund().getFundId();
			}
			break;
		}
		throw new IllegalStateException(type + ":" + value.getClass().getName());
	}

	public static AccessDeniedException createAccessDeniedException(Permission... deniedPermissions) {
		return new AccessDeniedException("Chybějící oprávnění: " + Arrays.toString(deniedPermissions), deniedPermissions);
	}
}
