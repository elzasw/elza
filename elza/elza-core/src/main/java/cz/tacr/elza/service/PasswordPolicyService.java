package cz.tacr.elza.service;

import java.time.OffsetDateTime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.controller.vo.PasswordPolicyVO;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPolicy;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.Level;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.PolicyRepository;

/**
 * Password policy: password strength and expiry.
 *
 * All callers go through this service, so resolving the policy per user group
 * would change only {@link #getPolicy()}.
 */
@Service
public class PasswordPolicyService {

    /**
     * Character groups: lowercase letters, uppercase letters, digits, other characters.
     */
    public static final int CHAR_GROUP_COUNT = 4;

    @Autowired
    private PolicyRepository policyRepository;

    public UsrPolicy getPolicy() {
        return policyRepository.getOneCheckExist(UsrPolicy.POLICY_ID);
    }

    /**
     * Updates the password policy. Null or a value <= 0 turns the rule off.
     */
    @AuthMethod(permission = {UsrPermission.Permission.USR_PERM})
    public UsrPolicy updatePolicy(final Integer expiryDays, final Integer minLength, final Integer minCharGroups) {
        if (minCharGroups != null && minCharGroups > CHAR_GROUP_COUNT) {
            throw new BusinessException("Počet skupin znaků musí být 1–" + CHAR_GROUP_COUNT, BaseCode.PROPERTY_IS_INVALID)
                    .set("property", "minCharGroups");
        }
        UsrPolicy policy = getPolicy();
        policy.setPasswordExpiryDays(expiryDays);
        policy.setPasswordMinLength(minLength);
        policy.setPasswordMinCharGroups(minCharGroups);
        return policyRepository.save(policy);
    }

    /**
     * Checks a new password against the policy.
     *
     * @param rawPassword new password (plaintext)
     * @throws BusinessException the password is empty or violates the policy
     */
    public void validate(final String rawPassword) {
        if (StringUtils.isEmpty(rawPassword)) {
            throw new BusinessException("Je nutné zadat nové heslo", BaseCode.PROPERTY_NOT_EXIST)
                    .set("property", "newPassword");
        }
        UsrPolicy policy = getPolicy();
        int minLength = positive(policy.getPasswordMinLength());
        if (rawPassword.codePointCount(0, rawPassword.length()) < minLength) {
            throw new BusinessException("Heslo musí mít alespoň " + minLength + " znaků.",
                    UserCode.PASSWORD_POLICY_VIOLATION)
                    .set("rule", "minLength")
                    .set("minLength", minLength)
                    .level(Level.WARNING);
        }
        int minCharGroups = Math.min(positive(policy.getPasswordMinCharGroups()), CHAR_GROUP_COUNT);
        if (countCharGroups(rawPassword) < minCharGroups) {
            throw new BusinessException("Heslo musí obsahovat znaky alespoň ze " + minCharGroups
                    + " skupin: malá písmena, velká písmena, číslice, ostatní znaky.",
                    UserCode.PASSWORD_POLICY_VIOLATION)
                    .set("rule", "minCharGroups")
                    .set("minCharGroups", minCharGroups)
                    .level(Level.WARNING);
        }
    }

    /**
     * Whether the password must be changed: the administrator required it, or it expired.
     *
     * Only persisted PASSWORD authentications are evaluated; SAML2 and the configured
     * default user (no ID) never need a change.
     *
     * @param authentication authentication of the user
     * @param now            current time
     */
    public boolean needsChange(final UsrAuthentication authentication, final OffsetDateTime now) {
        if (authentication.getAuthType() != UsrAuthentication.AuthType.PASSWORD
                || authentication.getAuthenticationId() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(authentication.getChangeRequired())) {
            return true;
        }
        if (Boolean.TRUE.equals(authentication.getNeverExpire())) {
            return false;
        }
        int expiryDays = positive(getPolicy().getPasswordExpiryDays());
        return expiryDays > 0 && authentication.getValidFrom().plusDays(expiryDays).isBefore(now);
    }

    /**
     * Counts the character groups present in the password.
     */
    static int countCharGroups(final String password) {
        boolean lower = false, upper = false, digit = false, other = false;
        for (int cp : password.codePoints().toArray()) {
            if (Character.isLowerCase(cp)) {
                lower = true;
            } else if (Character.isUpperCase(cp)) {
                upper = true;
            } else if (Character.isDigit(cp)) {
                digit = true;
            } else {
                other = true;
            }
        }
        return (lower ? 1 : 0) + (upper ? 1 : 0) + (digit ? 1 : 0) + (other ? 1 : 0);
    }

    public static PasswordPolicyVO toVO(final UsrPolicy policy) {
        PasswordPolicyVO result = new PasswordPolicyVO();
        result.setExpiryDays(policy.getPasswordExpiryDays());
        result.setMinLength(policy.getPasswordMinLength());
        result.setMinCharGroups(policy.getPasswordMinCharGroups());
        return result;
    }

    private static int positive(final Integer value) {
        return value == null || value < 0 ? 0 : value;
    }
}
