import {
    Button,
    Field as FluentField,
    MessageBar,
    MessageBarBody,
    Select,
    Spinner,
    Textarea,
    makeStyles,
    mergeClasses,
    tokens,
} from '@fluentui/react-components';
import * as issuesActions from 'actions/arr/issues';
import * as issueTypesActions from 'actions/refTables/issueTypes';
import { globalMessages } from 'components/shared/lang/messages';
import { FORM_ERROR, FormApi, SubmissionErrors } from 'final-form';
import { RefObject, useCallback, useEffect, useLayoutEffect, useMemo, useRef } from 'react';
import { Field, Form, useForm, useFormState } from 'react-final-form';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { IssueListVO, IssueVO } from 'types';
import { IssueType } from 'typings/store';
import { useActiveFund, useAppSelector, useAppThunkDispatch } from 'utils/hooks';
import { useFundProtocols } from './useFundProtocols';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    type: { id: 'issue.type', defaultMessage: 'Typ připomínky' },
    issueList: { id: 'issue.issueList', defaultMessage: 'Protokol' },
    text: { id: 'issue.text', defaultMessage: 'Text připomínky' },
});

const actionMessages = defineMessages({
    addAndClose: { id: 'issue.form.addAndClose', defaultMessage: 'Přidat a zavřít' },
    saving: { id: 'issue.form.saving', defaultMessage: 'Ukládání…' },
    saveFailed: { id: 'issue.form.saveFailed', defaultMessage: 'Připomínku se nepodařilo uložit.' },
});

const useStyles = makeStyles({
    form: {
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalM,
        padding: `${tokens.spacingVerticalL} ${tokens.spacingHorizontalL}`,
    },
    savingOverlay: {
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: `color-mix(in srgb, ${tokens.colorNeutralBackground1} 90%, transparent)`,
        backdropFilter: 'blur(4px)',
        animationName: { from: { opacity: 0 }, to: { opacity: 1 } },
        animationDuration: tokens.durationNormal,
        animationTimingFunction: tokens.curveEasyEase,
    },
    actions: {
        display: 'flex',
        justifyContent: 'flex-end',
        gap: tokens.spacingHorizontalS,
    },
});

export interface IssueFormValues {
    issueListId?: number;
    issueTypeId?: number;
    description?: string;
}

const NO_PROTOCOLS: IssueListVO[] = [];
const NO_ISSUE_TYPES: IssueType[] = [];

interface SelectionSyncProps {
    name: 'issueListId' | 'issueTypeId';
    /** Values the select offers. */
    ids: number[];
}

/**
 * Keeps a select of a new comment on an offered value. The creation window follows the fund the user opens
 * (the protocols change) and its defaults may come before the code lists load or point to a protocol that
 * is not offered.
 */
function SelectionSync({ name, ids }: SelectionSyncProps): null {
    const form = useForm<IssueFormValues>();
    const { values } = useFormState<IssueFormValues>({ subscription: { values: true } });
    // compared as a number: callers may pass the id as read from a <select>, i.e. as a string
    const selectedId = values[name] != null ? Number(values[name]) : undefined;

    useEffect(() => {
        const isOffered = selectedId != null && ids.includes(selectedId);
        if (!isOffered) {
            form.change(name, ids[0]);
        }
    }, [form, name, selectedId, ids]);

    return null;
}

/**
 * Covers the form while a save runs. The form turns inert at once, so nothing typed during the save can be
 * lost (the text is cleared after "Přidat").
 */
interface SavingOverlayProps {
    formRef: RefObject<HTMLFormElement>;
    /** Called once the form is usable again after a save; focus cannot be moved into an inert form before. */
    onSavingEnd: () => void;
}

function SavingOverlay({ formRef, onSavingEnd }: SavingOverlayProps) {
    const intl = useIntl();
    const styles = useStyles();
    const { submitting } = useFormState<IssueFormValues>({ subscription: { submitting: true } });
    const wasSubmittingRef = useRef(false);

    useLayoutEffect(() => {
        formRef.current?.toggleAttribute('inert', submitting);
        const hasSavingEnded = wasSubmittingRef.current && !submitting;
        wasSubmittingRef.current = submitting;
        if (hasSavingEnded) {
            onSavingEnd();
        }
    }, [formRef, submitting, onSavingEnd]);

    if (!submitting) {
        return null;
    }
    return (
        <div className={styles.savingOverlay}>
            <Spinner size="small" label={intl.formatMessage(actionMessages.saving)} />
        </div>
    );
}

export interface IssueSubmitResult {
    /** The user continues with another comment; the form stays open with the text cleared. */
    addAnother: boolean;
}

interface Props {
    initialValues: IssueFormValues;
    onSubmit: (values: IssueFormValues) => Promise<IssueVO>;
    onSubmitSuccess?: (issue: IssueVO, result: IssueSubmitResult) => void;
    /** Shows a Cancel button that calls it; injected by `modalDialogShow`. */
    onClose?: () => void;
    update?: boolean;
    className?: string;
    submitDisabled?: boolean;
    /** "Přidat" keeps the form open for another comment, "Přidat a zavřít" finishes. */
    canAddAnother?: boolean;
    /** Fund whose protocols are offered; defaults to the fund active in the arrangement. */
    fundId?: number;
    /** Whether a comment can go to the protocol; only such protocols are offered. All are when omitted. */
    canAddToProtocol?: (protocol: IssueListVO) => boolean;
    /** Moves focus to the comment text when the form appears. */
    autoFocus?: boolean;
}

function toOptionalId(value: string) {
    return value ? Number(value) : undefined;
}

/** A failed server call from `AjaxUtils`; a toast with the server's message has already been shown if it sent one. */
function isProcessedRequestError(error: unknown) {
    return typeof error === 'object' && error != null && 'processed' in error && error.processed === true;
}

export function IssueForm({
    initialValues,
    onSubmit,
    onSubmitSuccess,
    onClose,
    update = false,
    className,
    submitDisabled = false,
    canAddAnother = false,
    fundId,
    canAddToProtocol,
    autoFocus = false,
}: Props) {
    const intl = useIntl();
    const styles = useStyles();
    const dispatch = useAppThunkDispatch();
    const activeFundId = useActiveFund()?.id;
    const protocolFundId = fundId ?? activeFundId;
    const issueTypes = useAppSelector(({ refTables }) => refTables.issueTypes);
    const fundProtocols = useFundProtocols(protocolFundId);
    // set by the clicked button just before the submit it triggers
    const addAnotherRef = useRef(false);
    const formRef = useRef<HTMLFormElement>(null);
    const descriptionRef = useRef<HTMLTextAreaElement>(null);
    // after "Přidat" the user continues with the next comment's text
    const focusDescriptionAfterSaveRef = useRef(false);

    const focusDescriptionIfRequested = useCallback(() => {
        if (focusDescriptionAfterSaveRef.current) {
            focusDescriptionAfterSaveRef.current = false;
            descriptionRef.current?.focus();
        }
    }, []);

    // a frame later, not with `autoFocus`: a react-bootstrap dropdown that opened the form (ribbon, lecturing
    // panel) closes in the same click and returns focus to its toggle in its own effect
    useEffect(() => {
        if (!autoFocus) {
            return undefined;
        }
        const frame = requestAnimationFrame(() => descriptionRef.current?.focus());
        return () => cancelAnimationFrame(frame);
    }, [autoFocus]);

    useEffect(() => {
        dispatch(issueTypesActions.fetchIfNeeded());
    }, [dispatch]);

    useEffect(() => {
        if (protocolFundId != null) {
            dispatch(issuesActions.protocols.fetchIfNeeded(protocolFundId));
        }
    }, [dispatch, protocolFundId]);

    function validate(values: IssueFormValues) {
        const errors: Partial<Record<keyof IssueFormValues, string>> = {};
        const requiredMessage = intl.formatMessage(globalMessages.validationRequired);

        if (!values.issueListId) {
            errors.issueListId = requiredMessage;
        }
        if (!values.issueTypeId) {
            errors.issueTypeId = requiredMessage;
        }
        if (!values.description) {
            errors.description = requiredMessage;
        }

        return errors;
    }

    async function handleSubmit(
        values: IssueFormValues,
        form: FormApi<IssueFormValues>
    ): Promise<SubmissionErrors | undefined> {
        const addAnother = addAnotherRef.current;
        let savedIssue: IssueVO;
        try {
            savedIssue = await onSubmit(values);
        } catch (error) {
            // anything else is a bug and goes on to the global error handler
            if (!isProcessedRequestError(error)) {
                throw error;
            }
            return { [FORM_ERROR]: intl.formatMessage(actionMessages.saveFailed) };
        }
        if (addAnother) {
            // final-form refuses reset() during a submit; protocol and type stay for the next comment anyway
            form.change('description', undefined);
            form.resetFieldState('description');
            focusDescriptionAfterSaveRef.current = true;
        }
        onSubmitSuccess?.(savedIssue, { addAnother });
        return undefined;
    }

    function markAddAnother() {
        addAnotherRef.current = true;
    }

    function submitAndClose(form: FormApi<IssueFormValues>) {
        addAnotherRef.current = false;
        form.submit();
    }

    const areProtocolsLoaded = fundProtocols != null;
    const protocolOptions = useMemo(() => {
        if (!fundProtocols) {
            return NO_PROTOCOLS;
        }
        return canAddToProtocol ? fundProtocols.filter(canAddToProtocol) : fundProtocols;
    }, [fundProtocols, canAddToProtocol]);
    const hasNoProtocols = areProtocolsLoaded && protocolOptions.length === 0;
    const shouldSyncProtocol = !update && areProtocolsLoaded;
    const isSubmitBlocked = submitDisabled || !areProtocolsLoaded;
    const protocolIds = useMemo(() => protocolOptions.map(({ id }) => id), [protocolOptions]);
    const issueTypeOptions = issueTypes.fetched ? (issueTypes.data ?? NO_ISSUE_TYPES) : NO_ISSUE_TYPES;
    const issueTypeIds = useMemo(() => issueTypeOptions.map(({ id }) => id), [issueTypeOptions]);
    const shouldSyncIssueType = !update && !!issueTypes.fetched;

    return (
        <Form<IssueFormValues> initialValues={initialValues} validate={validate} onSubmit={handleSubmit}>
            {({ handleSubmit: submitForm, submitting, submitError, form }) => (
                // noValidate: Fluent `required` sets the native attribute, whose browser check would block
                // the submit before final-form validation shows its own messages
                <form ref={formRef} className={mergeClasses(styles.form, className)} onSubmit={submitForm} noValidate>
                    {shouldSyncProtocol && <SelectionSync name="issueListId" ids={protocolIds} />}
                    {shouldSyncIssueType && <SelectionSync name="issueTypeId" ids={issueTypeIds} />}
                    <Field<number> name="issueListId">
                        {({ input, meta }) => (
                            <FluentField
                                label={intl.formatMessage(messages.issueList)}
                                required
                                validationMessage={meta.touched ? meta.error : undefined}
                                validationState={meta.touched && meta.error ? 'error' : 'none'}
                            >
                                <Select
                                    value={input.value ? String(input.value) : ''}
                                    onChange={(_event, data) => input.onChange(toOptionalId(data.value))}
                                    onBlur={input.onBlur}
                                >
                                    {hasNoProtocols && <option value="" />}
                                    {protocolOptions.map(({ id, name }) => (
                                        <option key={id} value={id}>
                                            {name}
                                        </option>
                                    ))}
                                </Select>
                            </FluentField>
                        )}
                    </Field>
                    <Field<number> name="issueTypeId">
                        {({ input, meta }) => {
                            const errorMessage = meta.touched ? meta.error : undefined;
                            return (
                                <FluentField
                                    label={intl.formatMessage(messages.type)}
                                    required
                                    validationMessage={errorMessage}
                                    validationState={errorMessage ? 'error' : 'none'}
                                >
                                    <Select
                                        value={input.value ? String(input.value) : ''}
                                        onChange={(_event, data) => input.onChange(toOptionalId(data.value))}
                                        onBlur={input.onBlur}
                                    >
                                        {issueTypeOptions.map(({ id, name }) => (
                                            <option key={id} value={id}>
                                                {name}
                                            </option>
                                        ))}
                                    </Select>
                                </FluentField>
                            );
                        }}
                    </Field>
                    <Field<string> name="description">
                        {({ input, meta }) => {
                            const errorMessage = meta.touched ? meta.error : undefined;
                            return (
                                <FluentField
                                    label={intl.formatMessage(messages.text)}
                                    required
                                    validationMessage={errorMessage}
                                    validationState={errorMessage ? 'error' : 'none'}
                                >
                                    <Textarea
                                        ref={descriptionRef}
                                        resize="vertical"
                                        rows={5}
                                        value={input.value ?? ''}
                                        onChange={(_event, data) => input.onChange(data.value)}
                                        onBlur={input.onBlur}
                                    />
                                </FluentField>
                            );
                        }}
                    </Field>
                    {submitError && (
                        <MessageBar intent="error">
                            <MessageBarBody>{submitError}</MessageBarBody>
                        </MessageBar>
                    )}
                    <div className={styles.actions}>
                        {canAddAnother ? (
                            <>
                                <Button
                                    type="submit"
                                    appearance="primary"
                                    disabled={submitting || isSubmitBlocked}
                                    onClick={markAddAnother}
                                >
                                    <FormattedMessage {...globalMessages.add} />
                                </Button>
                                <Button disabled={submitting || isSubmitBlocked} onClick={() => submitAndClose(form)}>
                                    <FormattedMessage {...actionMessages.addAndClose} />
                                </Button>
                            </>
                        ) : (
                            <Button type="submit" appearance="primary" disabled={submitting || isSubmitBlocked}>
                                <FormattedMessage {...(update ? globalMessages.save : globalMessages.add)} />
                            </Button>
                        )}
                        {onClose && (
                            <Button onClick={onClose} disabled={submitting}>
                                <FormattedMessage {...globalMessages.cancel} />
                            </Button>
                        )}
                    </div>
                    <SavingOverlay formRef={formRef} onSavingEnd={focusDescriptionIfRequested} />
                </form>
            )}
        </Form>
    );
}

export type IssueFormProps = Props;
