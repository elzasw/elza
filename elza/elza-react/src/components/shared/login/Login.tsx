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
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { PersonKeyRegular } from '@fluentui/react-icons';
import { checkUserLogged, login } from 'actions/global/login';
import { FormEvent, useEffect, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { AppState } from 'typings/store';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
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
    /** Reason of the failed Windows sign-in, set by the server after a redirect from the SSO endpoint */
    ssoError?: SsoError | null;
}

interface SsoError {
    code: 'USER_NOT_FOUND' | 'USER_INACTIVE' | 'FAILED';
    username?: string | null;
}

const windowEx = window as WindowEx;
const isDefaultUserEnabled = !!windowEx.defaultUserEnabled;

const getDefaultCredentials = () =>
    isDefaultUserEnabled ? { username: 'admin', password: 'admin' } : { username: '', password: '' };

const useStyles = makeStyles({
    surface: {
        maxWidth: '620px',
    },
    content: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
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
        marginTop: tokens.spacingVerticalXXL,
        justifyContent: 'flex-start',
        paddingTop: tokens.spacingVerticalS,
        paddingBottom: tokens.spacingVerticalS,
        textAlign: 'left',
        whiteSpace: 'normal',
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

    useEffect(() => {
        dispatch(checkUserLogged());
    }, [dispatch]);

    useEffect(() => {
        // show the SSO error only once, not again after a later logout
        windowEx.ssoError = null;
    }, []);

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

    // Login dialog is shown only when user is not logged in and data about user have been fetched
    // to prevent flicker on page reload
    const isLoginDialogVisible = !logged && userDetailFetched;
    const ssoKerberosUrl = windowEx.ssoKerberosUrl;

    return (
        <Dialog open={isLoginDialogVisible} modalType="alert">
            <DialogSurface className={styles.surface}>
                <form onSubmit={handleSubmit}>
                    <DialogBody>
                        <DialogTitle>
                            <FormattedMessage {...messages.formTitle} />
                        </DialogTitle>
                        <DialogContent className={styles.content}>
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
                                {ssoKerberosUrl && (
                                    <>
                                        <div className={styles.column}>
                                            <CompoundButton
                                                className={styles.ssoButton}
                                                type="button"
                                                appearance="primary"
                                                icon={{ className: styles.ssoIcon, children: <PersonKeyRegular /> }}
                                                secondaryContent={intl.formatMessage(messages.ssoKerberos)}
                                                onClick={() => {
                                                    window.location.href = `${windowEx.serverContextPath ?? ''}${ssoKerberosUrl}`;
                                                }}
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
                                                setCredentials(current => ({ ...current, username: data.value }))
                                            }
                                        />
                                    </Field>
                                    <Field label={intl.formatMessage(messages.password)} required>
                                        <Input
                                            type="password"
                                            disabled={submitting}
                                            value={credentials.password}
                                            onChange={(_event, data) =>
                                                setCredentials(current => ({ ...current, password: data.value }))
                                            }
                                        />
                                    </Field>
                                </div>
                            </div>
                        </DialogContent>
                        <DialogActions>
                            <Button type="submit" appearance="primary" disabled={submitting}>
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
