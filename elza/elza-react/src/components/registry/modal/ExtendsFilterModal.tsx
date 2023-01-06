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
import i18n from '../../i18n';
import './ApExtSearchModal.scss';
import {FormInputField} from '../../shared';
import {RulDataTypeVO} from '../../../api/RulDataTypeVO';
import {RulDataTypeCodeEnum} from '../../../api/RulDataTypeCodeEnum';
import ReduxFormFieldErrorDecorator from '../../shared/form/ReduxFormFieldErrorDecorator';
import UnitdateField from '../field/UnitdateField';
import * as AreaInfo from '../form/filter/AreaInfo';
import {ArchiveEntityRel} from '../field/ArchiveEntityRel';
import {Area} from '../../../api/Area';
import {ArchiveEntityResultListVO} from '../../../api/ArchiveEntityResultListVO';
import {FilteredResultVO} from '../../../api/FilteredResultVO';
import {ApAccessPointVO} from '../../../api/ApAccessPointVO';
import {WebApi} from "../../../actions/WebApi";
import { RulDescItemTypeExtVO } from 'api/RulDescItemTypeExtVO';

const FORM_NAME = 'extendsFilter';

type FormProps = {};

const validate = values => {
    const errors: any = {};
    if (!values.partType) {
        errors.partType = i18n('global.validation.required');
    }
    if (values.itemType) {
        if (values.itemType.useSpecification && !values.itemSpec) {
            errors.itemSpec = i18n('global.validation.required');
        }
    } else {
        errors.itemType = i18n('global.validation.required');
    }
    if (!values.value) {
        errors.value = i18n('global.validation.required');
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

const bitItems = [
    {
        id: 'true',
        name: i18n('global.title.yes'),
    },
    {
        id: 'false',
        name: i18n('global.title.no'),
    },
];

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
            const dataType: RulDataTypeVO = refTables.rulDataTypes.itemsMap[itemType.dataTypeId];
            return _rulDescItemTypes.includes(itemType.code);
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
                        label={i18n('ap.ext-search.section.extends.value')}
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
                        label={i18n('ap.ext-search.section.extends.value')}
                        disabled={submitting}
                    />
                );
            case RulDataTypeCodeEnum.UNITDATE:
                return (
                    <Field
                        name="value"
                        label={i18n('ap.ext-search.section.extends.value')}
                        disabled={submitting}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={UnitdateField}
                    />
                );

            case RulDataTypeCodeEnum.RECORD_REF:
                return (
                    <Row>
                        <Col xs={6}>
                            <Form.Label>Oblast hledání</Form.Label>
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
                            <Form.Label>Pouze hlavní část</Form.Label>
                            <Field
                                name="onlyMainPart"
                                component={ReduxFormFieldErrorDecorator}
                                renderComponent={Form.Check}
                                type="checkbox"
                            />
                        </Col>
                        {itemType && (
                            <Col xs={12}>
                                <ArchiveEntityRel
                                    name={'obj'}
                                    label={i18n('ap.ext-search.section.relations.obj')}
                                    onlyMainPart={onlyMainPart}
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
                        label={i18n('ap.ext-search.section.extends.value')}
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
                return <div className="mt-2 text-center">Nepodporovaný typ</div>;
        }
    };

    return (
        <ReduxForm className="extends-filter-modal" onSubmit={handleSubmit(onSubmit)}>
            <Modal.Body>
                <Field
                    name="partType"
                    type="autocomplete"
                    component={FormInputField}
                    label={i18n('ap.ext-search.section.extends.part')}
                    items={parts}
                    disabled={submitting}
                />
                <Field
                    name="itemType"
                    type="autocomplete"
                    component={FormInputField}
                    label={i18n('ap.ext-search.section.extends.type')}
                    items={itemTypes}
                    disabled={submitting}
                />
                {itemSpecs && (
                    <Field
                        name="itemSpec"
                        type="autocomplete"
                        component={FormInputField}
                        label={i18n(
                            dataType && RulDataTypeCodeEnum.ENUM === dataType.code
                                ? 'ap.ext-search.section.extends.value'
                                : 'ap.ext-search.section.extends.spec',
                        )}
                        items={itemSpecs}
                        disabled={submitting}
                    />
                )}
                {dataType && renderData(dataType)}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="link" onClick={handleSubmit(onSubmit)}>
                    {i18n('global.action.use')}
                </Button>
                <Button variant="link" onClick={onClose} disabled={submitting}>
                    {i18n('global.action.close')}
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
