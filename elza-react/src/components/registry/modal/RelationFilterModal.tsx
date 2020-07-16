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
    scopeId: number;
    externalSystemCode: string;
    onClose: () => void;
} & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const RelationFilterModal = ({
                                 handleSubmit,
                                 onClose,
                                 refTables,
                                 onlyMainPart,
                                 area,
                                 itemType,
                                 scopeId,
                                 externalSystemCode,
                                 submitting,
                             }: Props) => {
    if (!refTables) {
        return <div/>;
    }

    const itemTypes = refTables.descItemTypes.items.filter((itemType: RulDescItemTypeExtVO) => {
        const dataType: RulDataTypeVO = refTables.rulDataTypes.itemsMap[itemType.dataTypeId];
        return dataType.code === RulDataTypeCodeEnum.RECORD_REF;
    });

    const callApi = (itemTypeId: number, itemSpecId: number, filter: any): Promise<ArchiveEntityResultListVO> => {
        return WebApi.findArchiveEntitiesInExternalSystem(0, 50, externalSystemCode, filter);
    };

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            <Row>
                <Col xs={12}>
                    <Form.Label>
                        Typ vztahu
                    </Form.Label>
                    <Field name="itemType"
                           type="autocomplete"
                           component={FormInputField}
                           items={itemTypes}
                           disabled={submitting}
                    />
                </Col>
                <Col xs={6}>
                    <Form.Label>
                        Oblast hledání
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
                        Pouze hlavní část
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
                        label={'Návazná archivní entita'}
                        onlyMainPart={onlyMainPart}
                        area={area}
                        api={callApi}
                        scopeId={scopeId}
                        itemTypeId={itemType.id}
                        modifyFilterData={data => {
                            data.relFilters = [{
                                relTypeId: itemType.id,
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
        onlyMainPart: selector(state, "onlyMainPart"),
        area: selector(state, "area"),
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(RelationFilterModal));
