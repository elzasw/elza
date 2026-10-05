import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Field,
    Input,
    MessageBar,
    MessageBarBody,
    Text,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { EyeOffRegular, EyeRegular } from '@fluentui/react-icons';
import { isAxiosError } from 'axios';
import { FormEvent, ReactNode, useState } from 'react';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { login } from 'actions/global/login';
import { Api } from 'api';
import { ExceptionTitle } from 'components/exception/ExceptionTitle';
import { type ExceptionPayload } from 'components/exception/exceptionKey';
import { globalMessages } from 'components/shared/lang/messages';
import { useAppThunkDispatch } from 'utils/hooks';

const messages = defineMessages({
    title: { id: 'setup.wizard.title', defaultMessage: 'Úvodní nastavení' },
    intro: {
        id: 'setup.wizard.intro',
        defaultMessage: 'Aplikace zatím nemá žádného uživatele. Vytvořte prvního administrátora.',
    },
    username: { id: 'setup.wizard.username', defaultMessage: 'Uživatelské jméno administrátora' },
    // same texts as the login and password forms
    password: { id: 'login.field.password', defaultMessage: 'Heslo' },
    passwordAgain: { id: 'admin.user.passwordAgain', defaultMessage: 'Opakovat heslo' },
    passwordsDiffer: { id: 'admin.user.validation.passNotEqual', defaultMessage: 'Zadaná hesla nejsou stejná' },
    create: { id: 'setup.wizard.create', defaultMessage: 'Vytvořit administrátora' },
    // same text as the button of the login form, which it opens
    login: { id: 'login.action.login', defaultMessage: 'Přihlásit' },
    createFailed: { id: 'setup.wizard.createFailed', defaultMessage: 'Administrátora se nepodařilo vytvořit.' },
    showPassword: { id: 'setup.wizard.showPassword', defaultMessage: 'Zobrazit heslo' },
    hidePassword: { id: 'setup.wizard.hidePassword', defaultMessage: 'Skrýt heslo' },
});

const useStyles = makeStyles({
    surface: {
        maxWidth: '520px',
    },
    content: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
});

interface FormValues {
    username: string;
    password: string;
    passwordAgain: string;
}

const EMPTY_VALUES: FormValues = { username: '', password: '', passwordAgain: '' };

interface Props {
    /** Logs in as the default user instead; offered only while the default user is enabled. */
    onUseDefaultUser?: () => void;
    /** The administrator was created but the automatic login failed. */
    onLoginFailed: () => void;
}

export type SetupWizardProps = Props;

/**
 * First-run setup: creates the first administrator while the application has no user,
 * then logs in as that administrator.
 */
export function SetupWizard({ onUseDefaultUser, onLoginFailed }: Props) {
    const styles = useStyles();
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const [values, setValues] = useState<FormValues>(EMPTY_VALUES);
    const [submitted, setSubmitted] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<ReactNode | null>(null);
    const [isPasswordShown, setIsPasswordShown] = useState(false);

    const isFilled = (name: keyof FormValues) => values[name].trim() !== '';
    const passwordsDiffer = values.password !== values.passwordAgain;
    const isValid = isFilled('username') && isFilled('password') && !passwordsDiffer;

    const requiredError = (name: keyof FormValues) =>
        submitted && !isFilled(name) ? intl.formatMessage(globalMessages.validationRequired) : undefined;
    const passwordAgainError =
        requiredError('passwordAgain') ??
        (submitted && passwordsDiffer ? intl.formatMessage(messages.passwordsDiffer) : undefined);

    const setValue = (name: keyof FormValues) => (_event: unknown, data: { value: string }) =>
        setValues(current => ({ ...current, [name]: data.value }));

    // one switch for both fields, so the typed password can be compared with its repetition
    const passwordToggle = (
        <Button
            appearance="transparent"
            size="small"
            disabled={submitting}
            icon={isPasswordShown ? <EyeOffRegular /> : <EyeRegular />}
            aria-label={intl.formatMessage(isPasswordShown ? messages.hidePassword : messages.showPassword)}
            title={intl.formatMessage(isPasswordShown ? messages.hidePassword : messages.showPassword)}
            aria-pressed={isPasswordShown}
            onClick={() => setIsPasswordShown(shown => !shown)}
        />
    );

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setSubmitted(true);
        if (!isValid) {
            return;
        }
        setSubmitting(true);
        setError(null);
        try {
            await Api.setup.setupCreateSetupAdmin(
                { username: values.username, password: values.password },
                // the error is shown in the dialog, not as a toast
                { overrideErrorHandler: true },
            );
        } catch (createError) {
            const data = isAxiosError<ExceptionPayload>(createError) ? createError.response?.data : undefined;
            setError(data ? <ExceptionTitle data={data} /> : intl.formatMessage(messages.createFailed));
            setSubmitting(false);
            return;
        }
        try {
            await dispatch(login(values.username.trim(), values.password));
        } catch {
            onLoginFailed();
        }
    };

    return (
        <Dialog open modalType="alert">
            <DialogSurface className={styles.surface}>
                <form onSubmit={handleSubmit} noValidate>
                    <DialogBody>
                        <DialogTitle>
                            <FormattedMessage {...messages.title} />
                        </DialogTitle>
                        <DialogContent className={styles.content}>
                            <Text>
                                <FormattedMessage {...messages.intro} />
                            </Text>
                            {error && (
                                <MessageBar intent="error">
                                    <MessageBarBody>{error}</MessageBarBody>
                                </MessageBar>
                            )}
                            <Field
                                label={intl.formatMessage(messages.username)}
                                required
                                validationMessage={requiredError('username')}
                            >
                                <Input
                                    autoFocus
                                    autoComplete="off"
                                    disabled={submitting}
                                    value={values.username}
                                    onChange={setValue('username')}
                                />
                            </Field>
                            <Field
                                label={intl.formatMessage(messages.password)}
                                required
                                validationMessage={requiredError('password')}
                            >
                                <Input
                                    type={isPasswordShown ? 'text' : 'password'}
                                    autoComplete="new-password"
                                    disabled={submitting}
                                    value={values.password}
                                    onChange={setValue('password')}
                                    contentAfter={passwordToggle}
                                />
                            </Field>
                            <Field
                                label={intl.formatMessage(messages.passwordAgain)}
                                required
                                validationMessage={passwordAgainError}
                            >
                                <Input
                                    type={isPasswordShown ? 'text' : 'password'}
                                    autoComplete="new-password"
                                    disabled={submitting}
                                    value={values.passwordAgain}
                                    onChange={setValue('passwordAgain')}
                                    contentAfter={passwordToggle}
                                />
                            </Field>
                        </DialogContent>
                        {onUseDefaultUser && (
                            <DialogActions position="start">
                                <Button type="button" appearance="primary" disabled={submitting} onClick={onUseDefaultUser}>
                                    <FormattedMessage {...messages.login} />
                                </Button>
                            </DialogActions>
                        )}
                        <DialogActions position="end">
                            <Button type="submit" appearance="primary" disabled={submitting}>
                                <FormattedMessage {...messages.create} />
                            </Button>
                        </DialogActions>
                    </DialogBody>
                </form>
            </DialogSurface>
        </Dialog>
    );
}
