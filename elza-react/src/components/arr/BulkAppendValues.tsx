import React, { useRef } from 'react';
import { Button, makeStyles, tokens } from '@fluentui/react-components';
import { AddRegular, DeleteRegular } from '@fluentui/react-icons';
import { Field } from 'react-final-form';
import { FieldArray } from 'react-final-form-arrays';
import { defineMessages, FormattedMessage, useIntl } from 'react-intl';
import { RulDataTypeCodeEnum } from 'api/RulDataTypeCodeEnum';
import { RulDataTypeVO } from 'api/RulDataTypeVO';
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { globalMessages } from 'components/shared/lang/messages';
import FormInputField from '../shared/form/FormInputField';
import { arrMessages } from './messages';
import DescItemLink from './nodeForm/DescItemLink';
import DescItemRecordRef from './nodeForm/DescItemRecordRef';
import DescItemUnitdate from './nodeForm/DescItemUnitdate';

const messages = defineMessages({
    add: { id: 'arr.fund.bulkModifications.appendValues.add', defaultMessage: 'Přidat hodnotu' },
    remove: { id: 'arr.fund.bulkModifications.appendValues.remove', defaultMessage: 'Odebrat hodnotu' },
});

export interface BulkAppendRecord {
    id: number;
}

export interface BulkAppendLink {
    value?: string;
    description?: string;
}

export interface BulkAppendValue {
    /** Stable row key, so the legacy value inputs keep their state when an earlier row is removed */
    rowId?: number;
    specId?: number | string;
    value?: string | BulkAppendRecord | BulkAppendLink;
}

const useStyles = makeStyles({
    row: {
        display: 'flex',
        alignItems: 'flex-start',
        columnGap: tokens.spacingHorizontalM,
    },
    spec: {
        flexGrow: 1,
        flexBasis: 0,
    },
    value: {
        flexGrow: 2,
        flexBasis: 0,
    },
    remove: {
        marginTop: tokens.spacingVerticalXXL,
    },
});

interface Props {
    name: string;
    dataType: RulDataTypeVO;
    refType: RulDescItemTypeExtVO;
    disabled?: boolean;
}

/**
 * Rows of values for the bulk append, each with a specification (when the item type has them)
 * and a value input by the data type.
 */
export function BulkAppendValues({ name, dataType, refType, disabled }: Props) {
    const intl = useIntl();
    const styles = useStyles();
    const nextRowId = useRef(1);

    return (
        <FieldArray<BulkAppendValue> name={name}>
            {({ fields }) => (
                <>
                    {fields.map((rowName, index) => {
                        const isOnlyRow = fields.length === 1;
                        const rowKey = fields.value?.[index]?.rowId ?? rowName;
                        return (
                            <div key={rowKey} className={styles.row}>
                                {refType.useSpecification && (
                                    <div className={styles.spec}>
                                        <SpecificationField
                                            name={`${rowName}.specId`}
                                            dataType={dataType}
                                            refType={refType}
                                            disabled={disabled}
                                        />
                                    </div>
                                )}
                                {dataType.code !== RulDataTypeCodeEnum.ENUM && (
                                    <div className={styles.value}>
                                        <ValueField
                                            name={`${rowName}.value`}
                                            specId={fields.value?.[index]?.specId}
                                            dataType={dataType}
                                            refType={refType}
                                            disabled={disabled}
                                        />
                                    </div>
                                )}
                                <Button
                                    className={styles.remove}
                                    appearance="subtle"
                                    icon={<DeleteRegular />}
                                    aria-label={intl.formatMessage(messages.remove)}
                                    title={intl.formatMessage(messages.remove)}
                                    disabled={disabled || isOnlyRow}
                                    onClick={() => fields.remove(index)}
                                />
                            </div>
                        );
                    })}
                    <Button
                        appearance="secondary"
                        icon={<AddRegular />}
                        disabled={disabled}
                        onClick={() => fields.push({ rowId: nextRowId.current++ })}
                    >
                        <FormattedMessage {...messages.add} />
                    </Button>
                </>
            )}
        </FieldArray>
    );
}

export type BulkAppendValuesProps = Props;

interface SpecificationFieldProps {
    name: string;
    dataType: RulDataTypeVO;
    refType: RulDescItemTypeExtVO;
    disabled?: boolean;
}

function SpecificationField({ name, dataType, refType, disabled }: SpecificationFieldProps) {
    const intl = useIntl();
    const isEnumType = dataType.code === RulDataTypeCodeEnum.ENUM;

    return (
        <Field
            name={name}
            type="select"
            component={FormInputField}
            label={intl.formatMessage(
                isEnumType
                    ? arrMessages.fundBulkModificationsReplaceReplaceEnum
                    : arrMessages.fundBulkModificationsReplaceReplaceSpec
            )}
            disabled={disabled}
        >
            <option />
            {refType.descItemSpecs.map((spec) => (
                <option key={spec.id} value={spec.id}>
                    {spec.name}
                </option>
            ))}
        </Field>
    );
}

interface ValueFieldProps {
    name: string;
    specId?: number | string;
    dataType: RulDataTypeVO;
    refType: RulDescItemTypeExtVO;
    disabled?: boolean;
}

function ValueField({ name, specId, dataType, refType, disabled }: ValueFieldProps) {
    const intl = useIntl();
    // shared props of the legacy desc item inputs
    const descItemProps = {
        hasSpecification: refType.useSpecification,
        locked: false,
        readMode: false,
        cal: false,
        readOnly: false,
    };

    switch (dataType.code) {
        case RulDataTypeCodeEnum.UNITDATE:
            return (
                <Field<string> name={name}>
                    {({ input, meta }) => (
                        <DescItemUnitdate
                            {...input}
                            {...descItemProps}
                            descItem={{ error: { value: meta.touched ? meta.error : null }, value: input.value }}
                            onChange={(unitdateValue: { value: string }) => input.onChange(unitdateValue.value)}
                        />
                    )}
                </Field>
            );
        case RulDataTypeCodeEnum.RECORD_REF:
            return (
                <Field<BulkAppendRecord> name={name}>
                    {({ input, meta }) => {
                        const spec = refType.descItemSpecs.find((item) => String(item.id) === String(specId));
                        // DescItemRecordRef is a connected legacy component without typed props
                        const recordRefProps = {
                            ...descItemProps,
                            itemTypeId: refType.id,
                            itemName: refType.shortcut,
                            specName: spec?.name ?? null,
                            descItem: {
                                error: { value: meta.touched ? meta.error : null },
                                record: input.value,
                                descItemSpecId: specId,
                            },
                        };
                        return <DescItemRecordRef {...input} {...recordRefProps} />;
                    }}
                </Field>
            );
        case RulDataTypeCodeEnum.URI_REF:
            return (
                <Field<BulkAppendLink> name={name}>
                    {({ input, meta }) => (
                        <DescItemLink
                            {...input}
                            {...descItemProps}
                            descItem={{
                                error: { value: meta.touched ? meta.error : null },
                                value: input.value?.value,
                                description: input.value?.description,
                            }}
                        />
                    )}
                </Field>
            );
        case RulDataTypeCodeEnum.BIT:
            return (
                <Field
                    name={name}
                    type="select"
                    component={FormInputField}
                    label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceText} />}
                    disabled={disabled}
                >
                    <option />
                    <option value="true">{intl.formatMessage(globalMessages.yes)}</option>
                    <option value="false">{intl.formatMessage(globalMessages.no)}</option>
                </Field>
            );
        default:
            return (
                <Field
                    name={name}
                    type={dataType.code === RulDataTypeCodeEnum.TEXT ? 'textarea' : 'text'}
                    component={FormInputField}
                    label={<FormattedMessage {...arrMessages.fundBulkModificationsReplaceReplaceText} />}
                    disabled={disabled}
                />
            );
    }
}
