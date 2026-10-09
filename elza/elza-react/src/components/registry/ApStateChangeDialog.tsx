import {
    Button,
    Dialog,
    DialogActions,
    DialogBody,
    DialogContent,
    DialogSurface,
    DialogTitle,
    Dropdown,
    Field as FluentField,
    Option,
    Spinner,
    Textarea,
} from '@fluentui/react-components';
import { Dismiss24Regular } from '@fluentui/react-icons';
import { WebApi } from 'actions';
import { requestScopesIfNeeded } from 'actions/refTables/scopesData';
import { globalMessages } from 'components/shared/lang/messages';
import { useEffect, useState } from 'react';
import { Field, Form } from 'react-final-form';
import { FormattedMessage, useIntl } from 'react-intl';
import { useThunkDispatch } from 'utils/hooks';
import { useAppSelector } from 'utils/hooks/useAppSelector';
import { StateApproval, StateApprovalCaption } from '../../api/StateApproval';
import { ApTypePicker } from './ApTypePicker';
import { AssignedUserField } from './AssignedUserField';
import { stateChangeMessages, useStateChangeDialogStyles } from './stateChangeShared';

const messages = stateChangeMessages;

export interface ApStateChangeVO {
    state?: StateApproval | null;
    comment?: string;
    typeId?: number;
    scopeId?: number;
    assignedTo?: number;
}

export interface ApStateChangeDialogProps {
    title: string;
    accessPointId: number;
    initialValues: ApStateChangeVO;
    onClose: () => void;
    onSubmit: (data: ApStateChangeVO) => Promise<unknown> | unknown;
}

/**
 * Change of the scope, class, state and assignment of an archival entity. An entity waiting for
 * approval opens without a state, so whoever may approve it chooses deliberately.
 */
export const ApStateChangeDialog = ({
    title,
    accessPointId,
    initialValues,
    onClose,
    onSubmit,
}: ApStateChangeDialogProps) => {
    const intl = useIntl();
    const styles = useStateChangeDialogStyles();
    const dispatch = useThunkDispatch();
    const scopes = useAppSelector(
        ({ refTables }) => refTables.scopesData.scopes?.find(({ versionId }) => versionId === -1)?.scopes ?? []
    );
    const { id: currentUserId } = useAppSelector(({ userDetail }) => userDetail);
    const [states, setStates] = useState<StateApproval[]>();

    useEffect(() => {
        dispatch(requestScopesIfNeeded(-1));
    }, [dispatch]);

    useEffect(() => {
        let cancelled = false;
        WebApi.getStateApproval(accessPointId).then((nextStates: StateApproval[]) => {
            if (!cancelled) {
                setStates(nextStates);
            }
        });
        return () => {
            cancelled = true;
        };
    }, [accessPointId]);

    const handleSubmit = (data: ApStateChangeVO) => {
        // an approved entity is assigned to nobody
        if (data.state === StateApproval.APPROVED) {
            return onSubmit({ ...data, assignedTo: undefined });
        }
        return onSubmit(data);
    };

    const validate = (values: ApStateChangeVO) => {
        const errors: Partial<Record<keyof ApStateChangeVO, string>> = {};
        const assignedToChanged = values.assignedTo !== initialValues.assignedTo;
        const stateChanged = values.state !== initialValues.state;
        if (
            values.state === StateApproval.TO_APPROVE &&
            values.assignedTo === currentUserId &&
            (assignedToChanged || stateChanged)
        ) {
            errors.assignedTo = intl.formatMessage(messages.toApproveSameUser);
        }
        return errors;
    };

    const canApprove = states?.includes(StateApproval.APPROVED);
    const isToApprove = initialValues.state === StateApproval.TO_APPROVE;

    return (
        <Dialog open modalType="modal" onOpenChange={(_event, data) => !data.open && onClose()}>
            <DialogSurface className={styles.surface}>
                {states === undefined ? (
                    <Spinner />
                ) : (
                    <Form<ApStateChangeVO>
                        validate={validate}
                        onSubmit={handleSubmit}
                        initialValues={{
                            ...initialValues,
                            state: canApprove && isToApprove ? null : initialValues.state,
                        }}
                    >
                        {({ submitting, handleSubmit, values, valid, pristine }) => {
                            const isApproved = values.state === StateApproval.APPROVED;
                            return (
                                <form onSubmit={handleSubmit}>
                                    <DialogBody>
                                        <DialogTitle
                                            action={
                                                <Button
                                                    appearance="subtle"
                                                    aria-label={intl.formatMessage(globalMessages.close)}
                                                    icon={<Dismiss24Regular />}
                                                    onClick={onClose}
                                                />
                                            }
                                        >
                                            {title}
                                        </DialogTitle>
                                        <DialogContent className={styles.content}>
                                            <Field<number> name="scopeId">
                                                {({ input }) => (
                                                    <FluentField label={intl.formatMessage(messages.scope)} required>
                                                        <Dropdown
                                                            disabled={submitting}
                                                            value={scopes.find((s) => s.id === input.value)?.name ?? ''}
                                                            selectedOptions={input.value ? [String(input.value)] : []}
                                                            onOptionSelect={(_event, data) =>
                                                                input.onChange(Number(data.optionValue))
                                                            }
                                                        >
                                                            {scopes.map((scope) => (
                                                                <Option key={scope.id} value={String(scope.id)}>
                                                                    {scope.name}
                                                                </Option>
                                                            ))}
                                                        </Dropdown>
                                                    </FluentField>
                                                )}
                                            </Field>
                                            <Field<number> name="typeId">
                                                {({ input }) => (
                                                    <ApTypePicker
                                                        label={intl.formatMessage(messages.type)}
                                                        scopeId={values.scopeId}
                                                        value={input.value || undefined}
                                                        onChange={(type) => input.onChange(type?.id)}
                                                        disabled={submitting || isApproved || !values.state}
                                                    />
                                                )}
                                            </Field>
                                            <Field<StateApproval | null> name="state">
                                                {({ input }) => (
                                                    <FluentField label={intl.formatMessage(messages.state)} required>
                                                        <Dropdown
                                                            disabled={submitting || states.length <= 1}
                                                            value={input.value ? StateApprovalCaption(input.value) : ''}
                                                            placeholder={
                                                                initialValues.state
                                                                    ? StateApprovalCaption(initialValues.state)
                                                                    : undefined
                                                            }
                                                            selectedOptions={input.value ? [input.value] : []}
                                                            onOptionSelect={(_event, data) =>
                                                                input.onChange(data.optionValue as StateApproval)
                                                            }
                                                        >
                                                            {states.map((state) => (
                                                                <Option key={state} value={state}>
                                                                    {StateApprovalCaption(state)}
                                                                </Option>
                                                            ))}
                                                        </Dropdown>
                                                    </FluentField>
                                                )}
                                            </Field>
                                            <Field<string> name="comment">
                                                {({ input }) => (
                                                    <FluentField label={intl.formatMessage(messages.comment)}>
                                                        <Textarea
                                                            disabled={submitting}
                                                            resize="vertical"
                                                            rows={4}
                                                            value={input.value ?? ''}
                                                            onChange={(_event, data) => input.onChange(data.value)}
                                                        />
                                                    </FluentField>
                                                )}
                                            </Field>
                                            {!isApproved && (
                                                <Field<number> name="assignedTo">
                                                    {({ input, meta }) => (
                                                        <AssignedUserField
                                                            accessPointId={accessPointId}
                                                            label={intl.formatMessage(messages.assignedUser)}
                                                            value={input.value || undefined}
                                                            onChange={input.onChange}
                                                            disabled={submitting}
                                                            error={meta.error}
                                                            currentUserId={currentUserId}
                                                            excludeUserIds={
                                                                values.state === StateApproval.TO_APPROVE
                                                                    ? [currentUserId]
                                                                    : undefined
                                                            }
                                                        />
                                                    )}
                                                </Field>
                                            )}
                                        </DialogContent>
                                        <DialogActions>
                                            <Button
                                                type="submit"
                                                appearance="primary"
                                                disabled={submitting || !valid || pristine || !values.state}
                                            >
                                                <FormattedMessage {...globalMessages.save} />
                                            </Button>
                                            <Button onClick={onClose} disabled={submitting}>
                                                <FormattedMessage {...globalMessages.cancel} />
                                            </Button>
                                        </DialogActions>
                                    </DialogBody>
                                </form>
                            );
                        }}
                    </Form>
                )}
            </DialogSurface>
        </Dialog>
    );
};

export default ApStateChangeDialog;
