import { MutableRefObject, useEffect, useMemo, useState } from 'react';
import { Form } from 'react-bootstrap';
import { Field, Form as FinalForm } from 'react-final-form';
import { FormApi } from 'final-form';
import { FormattedMessage, useIntl } from 'react-intl';
import { PersistentSortRequest } from 'elza-api';
import { WebApi } from 'actions/WebApi';
import { descItemTypesFetchIfNeeded } from 'actions/refTables/descItemTypes';
import { RulDescItemSpecExtVO } from 'api/RulDescItemSpecExtVO';
import { DescItemTypeRef } from 'typings/store/RefTables.types';
import { useAppSelector, useAppThunkDispatch } from 'utils/hooks';
import FormInputField from '../shared/form/FormInputField';
import { fundFormMessages } from './fundFormMessages';

/** How to sort: the request of the persistent sort without its units. */
export type PersistentSortSettings = Omit<PersistentSortRequest, 'nodeIds'>;

enum Direction {
    ASC = 'ASC',
    DESC = 'DESC',
}

export interface PersistentSortFields {
    itemType?: DescItemTypeRef;
    itemSpec?: RulDescItemSpecExtVO;
    direction: Direction;
    sortChildren: boolean;
}

interface Props {
    /** Rule set whose item types are offered. */
    ruleSetId: number;
    /** Settings to start from, e.g. those of a previous run. */
    initialSettings?: Partial<PersistentSortSettings>;
    onSubmit: (settings: PersistentSortSettings) => unknown;
    /** Receives the form, so that a button outside it can submit it. */
    formApiRef: MutableRefObject<FormApi<PersistentSortFields> | null>;
}

export type PersistentSortFormProps = Props;

function toFields(settings: Partial<PersistentSortSettings> | undefined, itemTypes: DescItemTypeRef[]): PersistentSortFields {
    const itemType = settings?.itemTypeCode ? itemTypes.find(i => i.code === settings.itemTypeCode) : undefined;
    const itemSpec = itemType && settings?.itemSpecCode
        ? itemType.descItemSpecs.find(i => i.code === settings.itemSpecCode)
        : undefined;
    return {
        itemType,
        itemSpec,
        direction: settings?.asc === false ? Direction.DESC : Direction.ASC,
        sortChildren: settings?.sortChildren ?? false,
    };
}

function toSettings(values: PersistentSortFields): PersistentSortSettings {
    return {
        itemTypeCode: values.itemType!.code,
        itemSpecCode: values.itemType!.useSpecification ? values.itemSpec?.code : undefined,
        asc: values.direction === Direction.ASC,
        sortChildren: values.sortChildren,
    };
}

/**
 * Settings of the persistent sort: the item by which the children of the units are sorted, the
 * direction and whether the lower levels are sorted too.
 */
export function PersistentSortForm({ ruleSetId, initialSettings, onSubmit, formApiRef }: Props) {
    const intl = useIntl();
    const dispatch = useAppThunkDispatch();
    const descItemTypes = useAppSelector(state => state.refTables.descItemTypes);
    const [ruleSetItemTypeCodes, setRuleSetItemTypeCodes] = useState<string[]>();

    useEffect(() => {
        dispatch(descItemTypesFetchIfNeeded());
    }, [dispatch]);

    useEffect(() => {
        let current = true;
        WebApi.getItemTypeCodesByRuleSet(ruleSetId).then((codes: string[]) => {
            if (current) {
                setRuleSetItemTypeCodes(codes);
            }
        });
        return () => {
            current = false;
        };
    }, [ruleSetId]);

    const itemTypes = useMemo(
        () => (ruleSetItemTypeCodes
            ? descItemTypes.items
                .filter(item => ruleSetItemTypeCodes.includes(item.code))
                .sort((a, b) => a.name.localeCompare(b.name))
            : []),
        [descItemTypes.items, ruleSetItemTypeCodes],
    );

    // a new object resets the form, so only when the settings or the item types change
    const initialValues = useMemo(
        () => toFields(initialSettings, descItemTypes.items),
        [initialSettings, descItemTypes.items],
    );

    function validate(values: PersistentSortFields) {
        const errors: Partial<Record<keyof PersistentSortFields, string>> = {};
        if (!values.itemType) {
            errors.itemType = intl.formatMessage(fundFormMessages.functionsPersistentSortNoSelectionItem);
        } else if (values.itemType.useSpecification && !values.itemSpec) {
            errors.itemSpec = intl.formatMessage(fundFormMessages.functionsPersistentSortNoSelectionSpec);
        }
        return errors;
    }

    return (
        <FinalForm<PersistentSortFields>
            initialValues={initialValues}
            validate={validate}
            onSubmit={values => onSubmit(toSettings(values))}
        >
            {({ handleSubmit, form, submitting, values }) => {
                formApiRef.current = form;
                return (
                    <Form onSubmit={handleSubmit}>
                        <Field
                            alwaysExpanded
                            label={<FormattedMessage {...fundFormMessages.fundBulkModificationsDescItemType} />}
                            items={itemTypes}
                            name="itemType"
                            disabled={submitting}
                            type="autocomplete"
                            component={FormInputField}
                        />

                        {values.itemType?.useSpecification && (
                            <Field
                                name="itemSpec"
                                alwaysExpanded
                                label={<FormattedMessage {...fundFormMessages.functionsPersistentSortSpec} />}
                                items={values.itemType.descItemSpecs}
                                getItemRenderClass={(item: RulDescItemSpecExtVO) => item.name.toLowerCase()}
                                disabled={submitting}
                                type="autocomplete"
                                component={FormInputField}
                            />
                        )}

                        <Field
                            type="radio"
                            name="direction"
                            disabled={submitting}
                            component={FormInputField}
                            value={Direction.ASC}
                            label={<FormattedMessage {...fundFormMessages.functionsPersistentSortDirectionAsc} />}
                            inline
                        />

                        <Field
                            type="radio"
                            name="direction"
                            disabled={submitting}
                            component={FormInputField}
                            value={Direction.DESC}
                            label={<FormattedMessage {...fundFormMessages.functionsPersistentSortDirectionDesc} />}
                            inline
                        />

                        <Field
                            name="sortChildren"
                            type="checkbox"
                            component={FormInputField}
                            label={<FormattedMessage {...fundFormMessages.functionsPersistentSortSortChildren} />}
                            disabled={submitting}
                        />
                    </Form>
                );
            }}
        </FinalForm>
    );
}
