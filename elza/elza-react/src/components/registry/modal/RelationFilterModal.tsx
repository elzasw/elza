import React, { useState, useEffect } from 'react';
import {ConfigProps, Field, formValueSelector, InjectedFormProps, reduxForm} from 'redux-form';
import {connect} from "react-redux";
import {Col, Form, Modal, Row} from "react-bootstrap";
import {Button} from "../../ui";
import { FormattedMessage, useIntl } from "react-intl";
import { globalMessages } from "components/shared/lang/messages";
import { getIntl } from "components/shared/lang/intlInstance";
import { filterMessages } from "../form/filter/messages";
import ReduxFormFieldErrorDecorator from "../../shared/form/ReduxFormFieldErrorDecorator";
import * as AreaInfo from "../form/filter/AreaInfo";
import {ArchiveEntityRel} from "../field/ArchiveEntityRel";
import {ArchiveEntityResultListVO} from "../../../api/ArchiveEntityResultListVO";
import {FormInputField} from "../../shared";
import {WebApi} from "../../../actions/WebApi";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {RulDataTypeVO} from "../../../api/RulDataTypeVO";
import {RulDataTypeCodeEnum} from "../../../api/RulDataTypeCodeEnum";
import {FilteredResultVO} from "../../../api/FilteredResultVO";
import {ApAccessPointVO} from "../../../api/ApAccessPointVO";
import { ApSearchArea } from 'elza-api';

const FORM_NAME = "relationFilterModalForm";

function validate(values, props): any {
    const errors = {} as any;

    if (!values.itemType) {
        errors.itemType = getIntl().formatMessage(globalMessages.validationRequired);
    }

    if (!values.obj) {
        errors.obj = getIntl().formatMessage(globalMessages.validationRequired);
    }

    return errors;
}

// @ts-ignore
const formConfig: ConfigProps<RelationFilterClientVO, ModalFormProps> = {
    form: FORM_NAME,
    validate
};

type Props = {
    refTables?: any;
    onClose: () => void;
    relApi?: (itemTypeId: number, itemSpecId: number, filter: any) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>
    rulSetsIds?: number[];
} & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const RelationFilterModal = ({
    handleSubmit,
    onClose,
    refTables,
    onlyMainPart,
    area,
    itemType,
    itemSpec,
    submitting,
    relApi,
    scopeId,
    rulSetsIds = [],
}: Props) => {
    const intl = useIntl();
    const [rulDescItemTypes, setRulDescItemTypes] = useState<string[]>([]);
    useEffect(() => {
        (async () => {
            const result:string[][] = await Promise.all(rulSetsIds.map((rulSetId) => WebApi.getItemTypeCodesByRuleSet(rulSetId)));
            setRulDescItemTypes(result.reduce(function(a,b){ return a.concat(b) }, [])); // flattened array
        })()
    }, [rulSetsIds])

    if (!refTables) {
        return <div/>;
    }

    const getItemTypes = (_rulDescItemTypes: string[]) => {
        return _rulDescItemTypes.length === 0 ? [] : refTables.descItemTypes.items.filter((itemType: RulDescItemTypeExtVO) => {
            const dataType: RulDataTypeVO = refTables.rulDataTypes.itemsMap[itemType.dataTypeId];
            return _rulDescItemTypes.includes(itemType.code) && dataType.code === RulDataTypeCodeEnum.RECORD_REF;
        });
    }

    const itemTypes = getItemTypes(rulDescItemTypes);

    const itemSpecs = itemType != null && itemType.useSpecification ? itemType.descItemSpecs : null;

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            <Row>
                <Col xs={12}>
                    <Field name="itemType"
                           label={intl.formatMessage(filterMessages.relationsType)}
                           type="autocomplete"
                           component={FormInputField}
                           items={[{id: null, name: intl.formatMessage(filterMessages.selectAll)},...itemTypes]}
                           disabled={submitting}
                    />
                </Col>
                {itemSpecs && <Col xs={12}>
                    <Field name="itemSpec"
                           type="autocomplete"
                           label={intl.formatMessage(filterMessages.relationsSpec)}
                           component={FormInputField}
                           items={itemSpecs}
                           disabled={submitting}
                    />
                </Col>}
                <Col xs={6}>
                    <Form.Label>
                        {intl.formatMessage(filterMessages.relationsArea)}
                    </Form.Label>
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
                            type='checkbox'
                        />
                        </>}
                </Col>
                {<Col xs={12}>
                    <ArchiveEntityRel
                        name={'obj'}
                        label={intl.formatMessage(filterMessages.relationsObj)}
                        onlyMainPart={area !== ApSearchArea.AllParts && onlyMainPart}
                        area={area}
                        api={relApi}
                        scopeId={scopeId}
                        itemTypeId={itemType?.id}
                        itemSpecId={itemSpec && itemSpec.id}
                        modifyFilterData={data => {
                            data.relFilters = [{
                                relTypeId: itemType?.id,
                                relSpecId: itemSpec && itemSpec.id,
                            }]
                            return data;
                        }}
                        disabled={submitting}
                    />
                </Col>}
            </Row>
        </Modal.Body>
        <Modal.Footer>
            <Button type={'submit'} variant={'outline-secondary'} onClick={handleSubmit} disabled={submitting}>
                <FormattedMessage {...globalMessages.use} />
            </Button>

            <Button variant={'link'} onClick={onClose} disabled={submitting}>
                <FormattedMessage {...globalMessages.cancel} />
            </Button>
        </Modal.Footer>
    </Form>;
};

const selector = formValueSelector(FORM_NAME);
const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
        itemType: selector(state, "itemType"),
        itemSpec: selector(state, "itemSpec"),
        onlyMainPart: selector(state, "onlyMainPart"),
        area: selector(state, "area"),
        scopeId: selector(state, "scopeId"),
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(RelationFilterModal));
