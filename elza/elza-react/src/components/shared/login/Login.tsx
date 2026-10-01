import {
    Button,
    CompoundButton,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Divider,
    Field,
    Input,
    MessageBar,
    MessageBarBody,
    Spinner,
    makeStyles,
    mergeClasses,
    tokens,
} from '@fluentui/react-components';
import { PersonKeyRegular } from '@fluentui/react-icons';
import { checkUserLogged, login } from 'actions/global/login';
import { FormEvent, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { AppState } from 'typings/store';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';
import { isAutoSsoLoginSuppressed } from 'utils/loginMethod';

// Id převzatých hlášek jsou z legacy katalogu beze změny.
const messages = defineMessages({
    signingIn: { id: 'login.state.signingIn', defaultMessage: 'Přihlašování…' },
    errorUnknown: { id: 'login.error.unknown', defaultMessage: 'Neznámá chyba přihlášení' },
    formTitle: { id: 'login.form.title', defaultMessage: 'Přihlášení uživatele' },
    ssoKerberos: {
        id: 'login.action.ssoKerberos',
        defaultMessage: 'Přihlásit se pomocí Windows autentizace (Kerberos)',
    },
    ssoKerberosTitle: {
        id: 'login.action.ssoKerberosTitle',
        defaultMessage: 'Windows autentizace',
    },
    defaultUserEnabled: {
        id: 'login.defaultUserEnabled',
        defaultMessage:
            'Je povolen výchozí uživatel. Vytvořte si vlastního uživatele s oprávněním administrátora a výchozího uživatele vypněte.',
    },
    ssoErrorUserNotFound: {
        id: 'login.error.ssoUserNotFound',
        defaultMessage:
            'Windows autentizace proběhla úspěšně, ale uživatel „{username}“ v aplikaci ELZA neexistuje. Požádejte administrátora o založení uživatele.',
    },
    ssoErrorUserInactive: {
        id: 'login.error.ssoUserInactive',
        defaultMessage:
            'Windows autentizace proběhla úspěšně, ale váš uživatel v aplikaci ELZA není aktivní. Obraťte se na administrátora.',
    },
    ssoErrorFailed: {
        id: 'login.error.ssoFailed',
        defaultMessage: 'Přihlášení pomocí Windows autentizace se nezdařilo. Obraťte se na administrátora.',
    },
    username: { id: 'login.field.username', defaultMessage: 'Uživatelské jméno' },
    password: { id: 'login.field.password', defaultMessage: 'Heslo' },
    login: { id: 'login.action.login', defaultMessage: 'Přihlásit' },
});

interface WindowEx extends Window {
    defaultUserEnabled?: boolean;
    serverContextPath?: string;
    ssoKerberosUrl?: string;
    /** Whether the dialog starts the Windows sign-in on its own instead of waiting for the button */
    autoSsoLogin?: boolean;
    /** Reason of the failed Windows sign-in, set by the server after a redirect from the SSO endpoint */
    ssoError?: SsoError | null;
}

interface SsoError {
    code: 'USER_NOT_FOUND' | 'USER_INACTIVE' | 'FAILED';
    username?: string | null;
}

const windowEx = window as WindowEx;
const isDefaultUserEnabled = !!windowEx.defaultUserEnabled;

/**
 * Query parameter that asks for the username/password form on a deployment which would
 * otherwise sign the user in through Kerberos without asking. Read once - in-app
 * navigation drops the parameter, but the request it expressed still holds.
 */
const PASSWORD_LOGIN_PARAM = 'login';
const PASSWORD_LOGIN_PARAM_VALUE = 'form';

/**
 * Automatic Kerberos sign-in, set by the server from `elza.security.autoSsoLogin`. While off,
 * Kerberos is reachable only through the button and the password form is always offered, so a
 * deployment whose SSO endpoint returns no session cannot leave the user waiting on it.
 */
const IS_AUTO_SSO_LOGIN_ALLOWED = !!windowEx.autoSsoLogin;

const isPasswordLoginRequested =
    new URLSearchParams(window.location.search).get(PASSWORD_LOGIN_PARAM) === PASSWORD_LOGIN_PARAM_VALUE;

const getDefaultCredentials = () =>
    isDefaultUserEnabled ? { username: 'admin', password: 'admin' } : { username: '', password: '' };

/** An unreachable KDC leaves the browser sitting on the handshake with nothing to time out. */
const SSO_HANDSHAKE_TIMEOUT_MS = 8000;

/** 'finished' means the handshake ran and did not produce a session, so the form is the way in. */
type SsoStatus = 'idle' | 'running' | 'finished';

type SsoOutcome =
    | { status: 'authenticated' }
    | { status: 'noTicket' }
    | { status: 'rejected'; error: SsoError }
    | { status: 'unsupported' };

/**
 * Runs the Kerberos handshake in place. The browser answers the endpoint's Negotiate challenge
 * from the ticket it already holds, so nothing navigates and the dialog keeps its state.
 * Deployments without the JSON endpoint report it as unsupported and the caller falls back to
 * the redirect.
 */
const runSsoHandshake = async (ssoUrl: string): Promise<SsoOutcome> => {
    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => controller.abort(), SSO_HANDSHAKE_TIMEOUT_MS);

    try {
        const response = await fetch(`${ssoUrl}/json`, {
            credentials: 'same-origin',
            signal: controller.signal,
        });

        if (response.status === 404) {
            return { status: 'unsupported' };
        }
        if (response.ok) {
            return { status: 'authenticated' };
        }

        // A refusal carries the reason; a challenge the browser could not answer carries nothing.
        const ssoError = (await response.json().catch(() => null)) as SsoError | null;
        return ssoError ? { status: 'rejected', error: ssoError } : { status: 'noTicket' };
    } catch {
        // Aborted or offline - the password form is the only way forward, and a redirect would
        // only hand the user the same failure on a page of its own.
        return { status: 'noTicket' };
    } finally {
        window.clearTimeout(timeoutId);
    }
};

const useStyles = makeStyles({
    surface: {
        maxWidth: '620px',
    },
    // Holds the spinner over the content it replaces, so the dialog keeps its size.
    contentArea: {
        position: 'relative',
    },
    content: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
    fade: {
        transitionProperty: 'opacity',
        transitionDuration: tokens.durationNormal,
        transitionTimingFunction: tokens.curveEasyEase,
    },
    faded: {
        opacity: 0,
        pointerEvents: 'none',
    },
    spinner: {
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    columns: {
        display: 'flex',
        alignItems: 'stretch',
        columnGap: tokens.spacingHorizontalL,
    },
    column: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'stretch',
        flexGrow: 1,
        flexBasis: 0,
        minWidth: 0,
        rowGap: tokens.spacingVerticalM,
    },
    divider: {
        flexGrow: 0,
    },
    ssoIcon: {
        fontSize: '28px',
        height: '28px',
        width: '28px',
    },
    ssoButton: {
        height: 'auto',
        justifyContent: 'flex-start',
        paddingTop: tokens.spacingVerticalS,
        paddingBottom: tokens.spacingVerticalS,
        textAlign: 'left',
        whiteSpace: 'normal',
    },
    // Aligns the button with the first field of the password form next to it.
    ssoButtonAligned: {
        marginTop: tokens.spacingVerticalXXL,
    },
});

export const Login = () => {
    const styles = useStyles();
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const logged = useAppSelector((state: AppState) => state.login.logged);
    const userDetailFetched = useAppSelector((state: AppState) => state.userDetail.fetched);

    const [credentials, setCredentials] = useState(getDefaultCredentials);
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const [ssoError, setSsoError] = useState(() => windowEx.ssoError ?? null);
    const [ssoStatus, setSsoStatus] = useState<SsoStatus>('idle');

    const ssoKerberosUrl = windowEx.ssoKerberosUrl;
    const isSsoAvailable = !!ssoKerberosUrl;
    // Kerberos runs on its own unless the user asked for the form, has just logged out and would
    // be signed straight back in, or has just come back from a failed Kerberos sign-in -
    // restarting it would only loop through the same failure.
    const isAutoSsoLoginEnabled =
        IS_AUTO_SSO_LOGIN_ALLOWED &&
        isSsoAvailable &&
        !isPasswordLoginRequested &&
        !isAutoSsoLoginSuppressed() &&
        !ssoError;
    // Waiting for the user detail keeps the dialog from flashing on a page reload.
    const isUserLoggedOut = !logged && userDetailFetched;
    const isAutoSsoLoginPending = isUserLoggedOut && isAutoSsoLoginEnabled && ssoStatus !== 'finished';

    const startSsoLogin = async () => {
        if (!ssoKerberosUrl) {
            return;
        }
        const ssoUrl = `${windowEx.serverContextPath ?? ''}${ssoKerberosUrl}`;

        setSsoStatus('running');
        setSsoError(null);
        const outcome = await runSsoHandshake(ssoUrl);

        // Backend without the JSON endpoint - sign in the old way, which leaves the page.
        if (outcome.status === 'unsupported') {
            window.location.href = ssoUrl;
            return;
        }

        // Stays 'running' on success: the dialog has to keep the spinner until the user detail
        // arrives and closes it, otherwise the form shows through in between.
        if (outcome.status === 'authenticated') {
            dispatch(checkUserLogged());
            return;
        }

        if (outcome.status === 'rejected') {
            setSsoError(outcome.error);
        }
        setSsoStatus('finished');
    };

    useEffect(() => {
        dispatch(checkUserLogged());
    }, [dispatch]);

    useEffect(() => {
        // show the SSO error only once, not again after a later logout
        windowEx.ssoError = null;
    }, []);

    useEffect(() => {
        if (isAutoSsoLoginPending) {
            void startSsoLogin();
        }
    }, [isAutoSsoLoginPending]);

    useEffect(() => {
        // The component stays mounted across a sign-out, so a previous attempt would otherwise
        // leave the dialog on its spinner when it opens again.
        if (!logged) {
            setSsoStatus('idle');
        }
    }, [logged]);

    const formatSsoError = (value: SsoError) => {
        switch (value.code) {
            case 'USER_NOT_FOUND':
                return intl.formatMessage(messages.ssoErrorUserNotFound, { username: value.username ?? '' });
            case 'USER_INACTIVE':
                return intl.formatMessage(messages.ssoErrorUserInactive);
            default:
                return intl.formatMessage(messages.ssoErrorFailed);
        }
    };

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setSubmitting(true);
        setSsoError(null);
        try {
            await dispatch(login(credentials.username, credentials.password));
            setCredentials(getDefaultCredentials());
            setError(null);
        } catch (loginError) {
            const message = (loginError as { data?: { message?: string } })?.data?.message;
            setError(message ?? intl.formatMessage(messages.errorUnknown));
        } finally {
            setSubmitting(false);
        }
    };

    const isAwaitingLogin = submitting || ssoStatus === 'running' || isAutoSsoLoginPending;

    return (
        <Dialog open={isUserLoggedOut} modalType="alert">
            <DialogSurface className={styles.surface}>
                <form onSubmit={handleSubmit}>
                    <DialogBody>
                        <DialogTitle>
                            <FormattedMessage {...messages.formTitle} />
                        </DialogTitle>
                        <DialogContent className={styles.contentArea}>
                            <div className={mergeClasses(styles.content, styles.fade, isAwaitingLogin && styles.faded)}>
                                {isDefaultUserEnabled && (
                                    <MessageBar intent="warning">
                                        <MessageBarBody>
                                            <FormattedMessage {...messages.defaultUserEnabled} />
                                        </MessageBarBody>
                                    </MessageBar>
                                )}
                                {ssoError && (
                                    <MessageBar intent="error">
                                        <MessageBarBody>{formatSsoError(ssoError)}</MessageBarBody>
                                    </MessageBar>
                                )}
                                <div className={styles.columns}>
                                    {isSsoAvailable && !isAutoSsoLoginPending && (
                                        <>
                                            <div className={styles.column}>
                                                <CompoundButton
                                                    className={mergeClasses(styles.ssoButton, styles.ssoButtonAligned)}
                                                    type="button"
                                                    appearance="primary"
                                                    disabled={isAwaitingLogin}
                                                    icon={{ className: styles.ssoIcon, children: <PersonKeyRegular /> }}
                                                    secondaryContent={intl.formatMessage(messages.ssoKerberos)}
                                                    onClick={startSsoLogin}
                                                >
                                                    <FormattedMessage {...messages.ssoKerberosTitle} />
                                                </CompoundButton>
                                            </div>
                                            <Divider vertical className={styles.divider} />
                                        </>
                                    )}
                                    <div className={styles.column}>
                                        {error && (
                                            <MessageBar intent="error">
                                                <MessageBarBody>{error}</MessageBarBody>
                                            </MessageBar>
                                        )}
                                        <Field label={intl.formatMessage(messages.username)} required>
                                            <Input
                                                autoFocus
                                                disabled={submitting}
                                                value={credentials.username}
                                                onChange={(_event, data) =>
                                                    setCredentials((current) => ({
                                                        ...current,
                                                        username: data.value,
                                                    }))
                                                }
                                            />
                                        </Field>
                                        <Field label={intl.formatMessage(messages.password)} required>
                                            <Input
                                                type="password"
                                                disabled={submitting}
                                                value={credentials.password}
                                                onChange={(_event, data) =>
                                                    setCredentials((current) => ({
                                                        ...current,
                                                        password: data.value,
                                                    }))
                                                }
                                            />
                                        </Field>
                                    </div>
                                </div>
                            </div>
                            <div
                                className={mergeClasses(styles.spinner, styles.fade, !isAwaitingLogin && styles.faded)}
                                aria-live="polite"
                            >
                                <Spinner label={intl.formatMessage(messages.signingIn)} />
                            </div>
                        </DialogContent>
                        <DialogActions className={mergeClasses(styles.fade, isAwaitingLogin && styles.faded)}>
                            <Button type="submit" appearance="primary" disabled={isAwaitingLogin}>
                                <FormattedMessage {...messages.login} />
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </form>
            </DialogSurface>
        </Dialog>
    );
};

export default Login;
