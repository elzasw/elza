import React from 'react';
import {ConfigProps, Field, formValueSelector, InjectedFormProps, reduxForm} from 'redux-form';
import {connect} from "react-redux";
import {Col, Form, Modal, Row} from "react-bootstrap";
import {Button} from "../../ui";
import i18n from "../../i18n";
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

const FORM_NAME = "relationFilterModalForm";

function validate(values, props): any {
    const errors = {} as any;

    if (!values.itemType) {
        errors.itemType = i18n('global.validation.required');
    }

    if (!values.obj) {
        errors.obj = i18n('global.validation.required');
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
}: Props) => {
    if (!refTables) {
        return <div/>;
    }

    const itemTypes = refTables.descItemTypes.items.filter((itemType: RulDescItemTypeExtVO) => {
        const dataType: RulDataTypeVO = refTables.rulDataTypes.itemsMap[itemType.dataTypeId];
        return dataType.code === RulDataTypeCodeEnum.RECORD_REF;
    });

    const itemSpecs = itemType != null && itemType.useSpecification ? itemType.descItemSpecs : null;

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            <Row>
                <Col xs={12}>
                    <Field name="itemType"
                           label={i18n('ap.ext-search.section.relations.type')}
                           type="autocomplete"
                           component={FormInputField}
                           items={itemTypes}
                           disabled={submitting}
                    />
                </Col>
                {itemSpecs && <Col xs={12}>
                    <Field name="itemSpec"
                           type="autocomplete"
                           label={i18n('ap.ext-search.section.relations.spec')}
                           component={FormInputField}
                           items={itemSpecs}
                           disabled={submitting}
                    />
                </Col>}
                <Col xs={6}>
                    <Form.Label>
                        {i18n('ap.ext-search.section.relations.area')}
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
                    <Form.Label>
                        {i18n('ap.ext-search.section.relations.only-main-part')}
                    </Form.Label>
                    <Field
                        name="onlyMainPart"
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={Form.Check}
                        type='checkbox'
                    />
                </Col>
                {itemType && <Col xs={12}>
                    <ArchiveEntityRel
                        name={'obj'}
                        label={i18n('ap.ext-search.section.relations.obj')}
                        onlyMainPart={onlyMainPart}
                        area={area}
                        api={relApi}
                        scopeId={scopeId}
                        itemTypeId={itemType.id}
                        itemSpecId={itemSpec && itemSpec.id}
                        modifyFilterData={data => {
                            data.relFilters = [{
                                relTypeId: itemType.id,
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
                {i18n('global.action.use')}
            </Button>

            <Button variant={'link'} onClick={onClose} disabled={submitting}>
                {i18n('global.action.cancel')}
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
