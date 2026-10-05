import { FormEvent, useEffect, useState } from 'react';
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
    Text,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { defineMessages, useIntl } from 'react-intl';
import { Api } from 'api';
import { PasswordPolicy } from 'elza-api';
import { globalMessages } from 'components/shared/lang';
import { addToastrSuccess } from 'components/shared/toastr/ToastrActions.jsx';
import { useAppThunkDispatch } from 'utils/hooks';
import { CHAR_GROUP_COUNT } from './passwordPolicy';

const messages = defineMessages({
    title: { id: 'admin.passwordPolicy.title', defaultMessage: 'Pravidla pro hesla' },
    info: {
        id: 'admin.passwordPolicy.info',
        defaultMessage: 'Prázdná hodnota nebo 0 znamená bez omezení.',
    },
    expiryDays: { id: 'admin.passwordPolicy.expiryDays', defaultMessage: 'Platnost hesla (dní)' },
    expiryDaysHint: {
        id: 'admin.passwordPolicy.expiryDaysHint',
        defaultMessage:
            'Po zapnutí platnosti budou muset změnit heslo všichni uživatelé, kteří jej nezměnili déle, než je nastavená doba.',
    },
    minLength: { id: 'admin.passwordPolicy.minLength', defaultMessage: 'Minimální délka hesla' },
    minCharGroups: {
        id: 'admin.passwordPolicy.minCharGroups',
        defaultMessage: 'Minimální počet skupin znaků (0–{max})',
    },
    minCharGroupsHint: {
        id: 'admin.passwordPolicy.minCharGroupsHint',
        defaultMessage: 'Skupiny: malá písmena, velká písmena, číslice, ostatní znaky.',
    },
    invalidNumber: { id: 'admin.passwordPolicy.invalidNumber', defaultMessage: 'Zadejte celé číslo od 0 do {max}.' },
    saved: { id: 'admin.passwordPolicy.saved', defaultMessage: 'Pravidla pro hesla byla uložena' },
});

const useStyles = makeStyles({
    content: {
        display: 'flex',
        flexDirection: 'column',
        rowGap: tokens.spacingVerticalM,
    },
});

type FieldName = 'expiryDays' | 'minLength' | 'minCharGroups';

type FormValues = Record<FieldName, string>;

const MAX_VALUE: Record<FieldName, number> = {
    expiryDays: 36500,
    minLength: 250,
    minCharGroups: CHAR_GROUP_COUNT,
};

function toText(value?: number) {
    return value != null && value > 0 ? String(value) : '';
}

/** Empty text is null, otherwise a non-negative integer; undefined when invalid. */
function parseValue(text: string, max: number): number | null | undefined {
    const trimmed = text.trim();
    if (trimmed === '') {
        return null;
    }
    if (!/^\d+$/.test(trimmed)) {
        return undefined;
    }
    const value = Number(trimmed);
    return value <= max ? value : undefined;
}

interface Props {
    open: boolean;
    onClose: () => void;
}

export type PasswordPolicyFormProps = Props;

/**
 * Dialog for editing the password policy.
 */
export function PasswordPolicyForm({ open, onClose }: Props) {
    const intl = useIntl();
    const styles = useStyles();
    const dispatch = useAppThunkDispatch();
    const [values, setValues] = useState<FormValues | null>(null);
    const [submitted, setSubmitted] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        let active = true;
        Api.admin.adminGetPasswordPolicy().then(({ data }) => {
            if (active) {
                setValues({
                    expiryDays: toText(data.expiryDays),
                    minLength: toText(data.minLength),
                    minCharGroups: toText(data.minCharGroups),
                });
            }
        });
        return () => {
            active = false;
        };
    }, []);

    const errorOf = (name: FieldName) =>
        values && parseValue(values[name], MAX_VALUE[name]) === undefined
            ? intl.formatMessage(messages.invalidNumber, { max: MAX_VALUE[name] })
            : undefined;

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setSubmitted(true);
        if (!values) {
            return;
        }
        const policy: PasswordPolicy = {};
        for (const name of Object.keys(MAX_VALUE) as FieldName[]) {
            const value = parseValue(values[name], MAX_VALUE[name]);
            if (value === undefined) {
                return;
            }
            // an absent value turns the rule off
            policy[name] = value ?? undefined;
        }
        setSaving(true);
        try {
            await Api.admin.adminUpdatePasswordPolicy(policy);
            dispatch(addToastrSuccess(intl.formatMessage(messages.saved)));
            onClose();
        } catch {
            // the error toast is shown by the API layer; the dialog stays open for a correction
        } finally {
            setSaving(false);
        }
    };

    const renderField = (name: FieldName, label: string, hint?: string) => {
        const error = submitted ? errorOf(name) : undefined;
        return (
            <Field
                label={label}
                hint={hint}
                validationState={error ? 'error' : 'none'}
                validationMessage={error}
            >
                <Input
                    inputMode="numeric"
                    value={values?.[name] ?? ''}
                    disabled={!values || saving}
                    onChange={(_, data) => setValues(v => (v ? { ...v, [name]: data.value } : v))}
                />
            </Field>
        );
    };

    return (
        <Dialog open={open} onOpenChange={(_, data) => !data.open && onClose()}>
            <DialogSurface>
                <form onSubmit={handleSubmit}>
                    <DialogBody>
                        <DialogTitle>{intl.formatMessage(messages.title)}</DialogTitle>
                        <DialogContent className={styles.content}>
                            <Text>{intl.formatMessage(messages.info)}</Text>
                            {renderField(
                                'expiryDays',
                                intl.formatMessage(messages.expiryDays),
                                intl.formatMessage(messages.expiryDaysHint),
                            )}
                            {renderField('minLength', intl.formatMessage(messages.minLength))}
                            {renderField(
                                'minCharGroups',
                                intl.formatMessage(messages.minCharGroups, { max: CHAR_GROUP_COUNT }),
                                intl.formatMessage(messages.minCharGroupsHint),
                            )}
                        </DialogContent>
                        <DialogActions>
                            <Button type="submit" appearance="primary" disabled={!values || saving}>
                                {intl.formatMessage(globalMessages.save)}
                            </Button>
                            <Button onClick={onClose}>{intl.formatMessage(globalMessages.cancel)}</Button>
                        </DialogActions>
                    </DialogBody>
                </form>
            </DialogSurface>
        </Dialog>
    );
}
