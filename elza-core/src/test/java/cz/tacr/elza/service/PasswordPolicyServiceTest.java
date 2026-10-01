package cz.tacr.elza.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrPolicy;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.UserCode;
import cz.tacr.elza.repository.PolicyRepository;

/**
 * {@link PasswordPolicyService}: password strength validation and expiry evaluation.
 */
class PasswordPolicyServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-30T12:00:00+02:00");

    private PasswordPolicyService service;
    private UsrPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new UsrPolicy();
        policy.setPolicyId(UsrPolicy.POLICY_ID);
        PolicyRepository policyRepository = mock(PolicyRepository.class);
        when(policyRepository.getOneCheckExist(UsrPolicy.POLICY_ID)).thenReturn(policy);
        service = new PasswordPolicyService();
        ReflectionTestUtils.setField(service, "policyRepository", policyRepository);
    }

    private void setPolicy(final Integer expiryDays, final Integer minLength, final Integer minCharGroups) {
        policy.setPasswordExpiryDays(expiryDays);
        policy.setPasswordMinLength(minLength);
        policy.setPasswordMinCharGroups(minCharGroups);
    }

    private static UsrAuthentication createAuthentication(final UsrAuthentication.AuthType authType,
                                                          final OffsetDateTime validFrom) {
        UsrAuthentication authentication = new UsrAuthentication();
        authentication.setAuthenticationId(1);
        authentication.setAuthType(authType);
        authentication.setValidFrom(validFrom);
        return authentication;
    }

    @ParameterizedTest
    @CsvSource({
            "a, 1",
            "abc, 1",
            "aB, 2",
            "aB1, 3",
            "aB1!, 4",
            "ěŠ, 2",
            "'a b', 2",
            "1234, 1",
    })
    void countCharGroups(final String password, final int expected) {
        assertThat(PasswordPolicyService.countCharGroups(password)).isEqualTo(expected);
    }

    @Test
    void emptyPasswordIsRejectedEvenWithoutPolicy() {
        setPolicy(null, null, null);
        assertThatThrownBy(() -> service.validate(""))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getErrorCode()).isEqualTo(BaseCode.PROPERTY_NOT_EXIST));
        assertThatThrownBy(() -> service.validate(null)).isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {
            // minLength, minCharGroups, password, valid
            "null, null, a, true",
            "0, 0, a, true",
            "-5, -1, a, true",
            "8, null, abcdefg, false",
            "8, null, abcdefgh, true",
            "3, null, ěšč, true",
            "null, 3, abcDEF, false",
            "null, 3, abcDEF1, true",
            "null, 4, abcDEF1, false",
            "null, 4, abcDEF1-, true",
            "null, 9, abcDEF1-, true",
            "8, 3, aB1, false",
            "8, 3, aaaaBBBB, false",
            "8, 3, aaaaBBB1, true",
    }, nullValues = "null")
    void validate(final Integer minLength, final Integer minCharGroups, final String password, final boolean valid) {
        setPolicy(null, minLength, minCharGroups);
        if (valid) {
            assertThatCode(() -> service.validate(password)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> service.validate(password))
                    .isInstanceOfSatisfying(BusinessException.class,
                                            e -> assertThat(e.getErrorCode()).isEqualTo(UserCode.PASSWORD_POLICY_VIOLATION));
        }
    }

    @Test
    void minLengthViolationCarriesLimit() {
        setPolicy(null, 8, 3);
        assertThatThrownBy(() -> service.validate("aB1"))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getProperties()).containsEntry("minLength", 8));
    }

    @Test
    void minCharGroupsViolationCarriesLimit() {
        setPolicy(null, null, 3);
        assertThatThrownBy(() -> service.validate("abcdef"))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getProperties()).containsEntry("minCharGroups", 3));
    }

    @Test
    void noExpiryWithoutPolicy() {
        setPolicy(null, null, null);
        UsrAuthentication authentication = createAuthentication(UsrAuthentication.AuthType.PASSWORD,
                                                                NOW.minusYears(10));
        assertThat(service.needsChange(authentication, NOW)).isFalse();

        setPolicy(0, null, null);
        assertThat(service.needsChange(authentication, NOW)).isFalse();
    }

    @Test
    void expiredPassword() {
        setPolicy(90, null, null);
        assertThat(service.needsChange(createAuthentication(UsrAuthentication.AuthType.PASSWORD,
                                                            NOW.minusDays(91)), NOW)).isTrue();
        assertThat(service.needsChange(createAuthentication(UsrAuthentication.AuthType.PASSWORD,
                                                            NOW.minusDays(89)), NOW)).isFalse();
    }

    @Test
    void neverExpireOverridesPolicy() {
        setPolicy(90, null, null);
        UsrAuthentication authentication = createAuthentication(UsrAuthentication.AuthType.PASSWORD,
                                                                NOW.minusYears(10));
        authentication.setNeverExpire(true);
        assertThat(service.needsChange(authentication, NOW)).isFalse();
    }

    @Test
    void changeRequiredWithoutPolicy() {
        setPolicy(null, null, null);
        UsrAuthentication authentication = createAuthentication(UsrAuthentication.AuthType.PASSWORD, NOW);
        authentication.setChangeRequired(true);
        authentication.setNeverExpire(true);
        assertThat(service.needsChange(authentication, NOW)).isTrue();
    }

    @Test
    void saml2NeverNeedsChange() {
        setPolicy(90, null, null);
        UsrAuthentication authentication = createAuthentication(UsrAuthentication.AuthType.SAML2,
                                                                NOW.minusYears(10));
        authentication.setChangeRequired(true);
        assertThat(service.needsChange(authentication, NOW)).isFalse();
    }

    @Test
    void syntheticDefaultUserNeverNeedsChange() {
        setPolicy(90, null, null);
        UsrAuthentication authentication = createAuthentication(UsrAuthentication.AuthType.PASSWORD, null);
        authentication.setAuthenticationId(null);
        assertThat(service.needsChange(authentication, NOW)).isFalse();
    }

    @Test
    void tooManyCharGroupsInPolicyIsRejected() {
        assertThatThrownBy(() -> service.updatePolicy(null, null, 5))
                .isInstanceOfSatisfying(BusinessException.class,
                                        e -> assertThat(e.getErrorCode()).isEqualTo(BaseCode.PROPERTY_IS_INVALID));
    }
}
