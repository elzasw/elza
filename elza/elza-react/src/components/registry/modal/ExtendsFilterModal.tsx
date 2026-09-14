import React, { useState, useEffect } from 'react';
import {
    ConfigProps,
    Field,
    Form as ReduxForm,
    formValueSelector,
    InjectedFormProps,
    reduxForm,
    SubmitHandler,
} from 'redux-form';
import {Col, Form, Modal, Row} from 'react-bootstrap';
import {connect} from 'react-redux';
import {Action} from 'redux';
import {ThunkDispatch} from 'redux-thunk';
import {Button} from '../../ui';
import { FormattedMessage, useIntl } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";
import { getIntl } from "components/shared/lang/intlInstance";
import { filterMessages } from "../form/filter/messages";
import './ApExtSearchModal.scss';
import {FormInputField} from '../../shared';
import {RulDataTypeVO} from '../../../api/RulDataTypeVO';
import {RulDataTypeCodeEnum} from '../../../api/RulDataTypeCodeEnum';
import ReduxFormFieldErrorDecorator from '../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../field/UnitdateField';
import * as AreaInfo from '../form/filter/AreaInfo';
import {ArchiveEntityRel} from '../field/ArchiveEntityRel';
import {ApSearchArea} from 'elza-api';
import {ArchiveEntityResultListVO} from '../../../api/ArchiveEntityResultListVO';
import {FilteredResultVO} from '../../../api/FilteredResultVO';
import {ApAccessPointVO} from '../../../api/ApAccessPointVO';
import {WebApi} from "../../../actions/WebApi";
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';
import { registryModalMessages } from './messages';

const FORM_NAME = 'extendsFilter';

type FormProps = {};

const validate = values => {
    const errors: any = {};
    if (!values.itemType) {
        errors.itemType = getIntl().formatMessage(globalMessages.validationRequired);
    }
    if (!values.value) {
        errors.value = getIntl().formatMessage(globalMessages.validationRequired);
    }
    return errors;
};

const formConfig: ConfigProps<FormProps> = {
    form: FORM_NAME,
    validate,
};

type Props = {
    handleSubmit: SubmitHandler<FormData, any, any>;
    formData?: FormProps;
    submitting: boolean;
    onSubmit: (data: any) => void;
    onClose: () => void;
    relEntityApi?: (
        itemTypeId: number,
        itemSpecId: number,
        filter: any,
    ) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>;
    rulSetsIds?: number[];
} & ReturnType<typeof mapDispatchToProps> &
    ReturnType<typeof mapStateToProps> &
    InjectedFormProps;



const ExtendsFilterModal = ({
    handleSubmit,
    onClose,
    submitting,
    onSubmit,
    refTables,
    itemType,
    relEntityApi,
    area,
    onlyMainPart,
    scopeId,
    rulSetsIds = [],
}: Props) => {
    const intl = useIntl();
    // Skládá se při renderu, aby popisky reagovaly na přepnutí jazyka.
    const bitItems = [
        { id: 'true', name: intl.formatMessage(globalMessages.yes) },
        { id: 'false', name: intl.formatMessage(globalMessages.no) },
    ];
    const [rulDescItemTypes, setRulDescItemTypes] = useState<string[]>([]);
    const parts = refTables.partTypes.items;
    const dataType = itemType == null ? null : (refTables.rulDataTypes.itemsMap[itemType.dataTypeId] as RulDataTypeVO);
    const itemSpecs = itemType != null && itemType.useSpecification ? itemType.descItemSpecs : null;

    useEffect(() => {
        (async () => {
            // fetch list of DescItemType codes for ruleSets
            const result:string[][] = await Promise.all(rulSetsIds.map((rulSetId) => WebApi.getItemTypeCodesByRuleSet(rulSetId)));
            setRulDescItemTypes(result.reduce(function(a,b){ return a.concat(b) }, [])); // flattened array
        })()
    }, [rulSetsIds]);

    if (!refTables) {
        return <div/>;
    }

    const getItemTypes = (_rulDescItemTypes: string[]) => {
        return _rulDescItemTypes.length === 0 ? [] : refTables.descItemTypes.items.filter((itemType: RulDescItemTypeExtVO) => {
            return _rulDescItemTypes.includes(itemType.code);
        })
        // Docasne zakazani nekterych datovych typu kvuli chybam na serveru(#9089)
        .filter((itemType:RulDescItemTypeExtVO) => {
            const dataType: RulDataTypeVO = refTables.rulDataTypes.itemsMap[itemType.dataTypeId];

            if(
                //dataType.code === RulDataTypeCodeEnum.INT || // #9085
				//dataType.code === RulDataTypeCodeEnum.BIT || // #9087
                dataType.code === RulDataTypeCodeEnum.COORDINATES // #9086
            ){
                console.log("#### remove data type", dataType.code, itemType.code)
                return false;
            }
            return true;
        });
    }

    const itemTypes = getItemTypes(rulDescItemTypes);

    const renderData = dataType => {
        switch (dataType.code) {
            case RulDataTypeCodeEnum.DECIMAL:
            case RulDataTypeCodeEnum.INT:
            case RulDataTypeCodeEnum.COORDINATES:
            case RulDataTypeCodeEnum.UNITID:
            case RulDataTypeCodeEnum.URI_REF:
            case RulDataTypeCodeEnum.STRING:
                return (
                    <Field
                        name="value"
                        type="text"
                        component={FormInputField}
                        label={intl.formatMessage(filterMessages.extendsValue)}
                        disabled={submitting}
                    />
                );
            case RulDataTypeCodeEnum.FORMATTED_TEXT:
            case RulDataTypeCodeEnum.TEXT:
                return (
                    <Field
                        name="value"
                        type="textarea"
                        component={FormInputField}
                        label={intl.formatMessage(filterMessages.extendsValue)}
                        disabled={submitting}
                    />
                );
            case RulDataTypeCodeEnum.UNITDATE:
                return (
                    <Field
                        name="value"
                        label={intl.formatMessage(filterMessages.extendsValue)}
                        disabled={submitting}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={UnitdateField}
                    />
                );

            case RulDataTypeCodeEnum.RECORD_REF:
                return (
                    <Row>
                        <Col xs={6}>
                            <Form.Label><FormattedMessage {...registryModalMessages.searchArea} /></Form.Label>
                            <Field
                                name={'area'}
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Form.Control}
                                as={'select'}
                            >
                                {AreaInfo.getValues().map(area => (
                                    <option key={area} value={area}>
                                        {AreaInfo.getName(area)}
                                    </option>
                                ))}
                            </Field>
                        </Col>
                        <Col xs={6}>
                                {area !== ApSearchArea.AllParts && <>
                                    <Form.Label>
                                        {intl.formatMessage(filterMessages.relationsOnlyMainPart)}
                                    </Form.Label>
                                        <Field
                                        name="onlyMainPart"
                                        component={ReduxFormFieldErrorDecorator}
                                        renderComponent={Form.Check}
                                        type="checkbox"
                                    />
                                    </>}
                        </Col>
                        {itemType && (
                            <Col xs={12}>
                                <ArchiveEntityRel
                                    name={'obj'}
                                    label={intl.formatMessage(filterMessages.relationsObj)}
                                    onlyMainPart={area !== ApSearchArea.AllParts && onlyMainPart}
                                    area={area}
                                    api={relEntityApi}
                                    itemTypeId={itemType.id}
                                    modifyFilterData={data => {
                                        data.relFilters = [
                                            {
                                                relTypeId: itemType.id,
                                            },
                                        ];
                                        return data;
                                    }}
                                    disabled={submitting}
                                    scopeId={scopeId}
                                />
                            </Col>
                        )}
                    </Row>
                );

            case RulDataTypeCodeEnum.BIT:
                return (
                    <Field
                        name="value"
                        type="autocomplete"
                        component={FormInputField}
                        label={intl.formatMessage(filterMessages.extendsValue)}
                        useIdAsValue
                        items={bitItems}
                        disabled={submitting}
                    />
                );

            case RulDataTypeCodeEnum.ENUM:
                // hodnota je specifikace
                return;

            case RulDataTypeCodeEnum.DATE:
            case RulDataTypeCodeEnum.FILE_REF:
            case RulDataTypeCodeEnum.JSON_TABLE:
            case RulDataTypeCodeEnum.STRUCTURED:
                return <div className="mt-2 text-center"><FormattedMessage {...registryModalMessages.unsupportedType} /></div>;
        }
    };

    return (
        <ReduxForm className="extends-filter-modal" onSubmit={handleSubmit(onSubmit)}>
            <Modal.Body>
                <Field
                    name="partType"
                    type="autocomplete"
                    component={FormInputField}
                    label={intl.formatMessage(filterMessages.extendsPart)}
                    items={parts}
                    disabled={submitting}
                />
                <Field
                    name="itemType"
                    type="autocomplete"
                    component={FormInputField}
                    label={intl.formatMessage(filterMessages.extendsType)}
                    items={itemTypes}
                    disabled={submitting}
                />
                {itemSpecs && (
                    <Field
                        name="itemSpec"
                        type="autocomplete"
                        component={FormInputField}
                        label={intl.formatMessage(
                            dataType && RulDataTypeCodeEnum.ENUM === dataType.code
                                ? filterMessages.extendsValue
                                : filterMessages.extendsSpec,
                        )}
                        items={itemSpecs}
                        disabled={submitting}
                    />
                )}
                {dataType && renderData(dataType)}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={handleSubmit(onSubmit)}>
                    <FormattedMessage {...globalMessages.use} />
                </Button>
                <Button variant="link" onClick={onClose} disabled={submitting}>
                    <FormattedMessage {...globalMessages.close} />
                </Button>
            </Modal.Footer>
        </ReduxForm>
    );
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
    dispatch,
});

const mapStateToProps = (state: any) => {
    const selector = formValueSelector(FORM_NAME);
    return {
        itemType: selector(state, 'itemType'),
        area: selector(state, 'area'),
        scopeId: selector(state, 'scopeId'),
        onlyMainPart: selector(state, 'onlyMainPart'),
        refTables: state.refTables,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(ExtendsFilterModal));
