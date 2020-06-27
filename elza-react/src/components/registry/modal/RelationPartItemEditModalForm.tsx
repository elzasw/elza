import React from 'react';
import {ConfigProps, Field, formValueSelector, InjectedFormProps, reduxForm} from 'redux-form';
import {connect} from "react-redux";
import {ApCreateTypeVO} from "../../../api/ApCreateTypeVO";
import {RulDescItemTypeExtVO} from "../../../api/RulDescItemTypeExtVO";
import {Col, Form, Modal, Row} from "react-bootstrap";
import {Button} from "../../ui";
import i18n from "../../i18n";
import ReduxFormFieldErrorDecorator from "../../shared/form/ReduxFormFieldErrorDecorator";
import SpecificationField from "../field/SpecificationField";
import {computeAllowedItemSpecIds} from "../../../utils/ItemInfo";
import * as AreaInfo from "../form/filter/AreaInfo";
import {ArchiveEntityRel} from "../field/ArchiveEntityRel";

const FORM_NAME = "relationPartItemEditModalForm";

// function validate(values: RelationFilterClientVO, props: ModalFormProps & InjectedFormProps<RelationFilterClientVO, ModalFormProps, string>): any {
function validate(values, props): any {
    const errors = {} as any;

    if (!values.codeObj || values.codeObj.id == null) {
        errors.codeObj = "Návazná archivní entita je povinná";
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
    typeId: number;
    itemTypeAttributeMap: Record<number, ApCreateTypeVO>;
    onClose: () => void;
} & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const RelationPartItemEditModalForm = ({
                                           handleSubmit,
                                           onClose,
                                           refTables,
                                           onlyMainPart,
                                           itemTypeAttributeMap,
                                           area,
                                           specId,
                                           typeId,
                                           geoSearchItemType,
                                           submitting,
                                       }: Props) => {
    if (!refTables) {
        return <div/>;
    }

    let itemType = refTables.descItemTypes.itemsMap[typeId] as RulDescItemTypeExtVO;
    const renderSpecification = typeId && itemType && itemType.useSpecification;

    const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, specId);

    const getSpecialActionField = () => {
        //todo: Zatim nepodporujeme

        // if (itemType.itemTypeInfo) {
        //     const geoSearch = itemType.itemTypeInfo.find(info => info.geoSearchItemType !== null);
        //     if (geoSearch) {
        //         const geoSearchItemTypeCode = geoSearch.geoSearchItemType;
        //         const geoSearchItemType = itemTypes.find(itemType => itemType.code === geoSearchItemTypeCode);
        //         if (geoSearchItemType) {
        //             return <ff.Specification
        //                 name={`geoSearchItemType`}
        //                 label={geoSearchItemType.name}
        //                 fieldProps={{
        //                     itemTypeId: geoSearchItemType.id,
        //                     itemSpecIds: geoSearchItemType.itemSpecIds,
        //                 }}
        //             />
        //         }
        //     }
        // }

        return null;
    };

    const specialActionField = getSpecialActionField();

    //const EntityField = specialActionField ? ff.ArchiveEntitRelGeoAdminClass : ff.ArchiveEntitRel;
    // TODO: dodělat podporu pro geo admin class
    const EntityField = ArchiveEntityRel;

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            {renderSpecification && <Field
                name="specId"
                label="Specifikace vztahu"
                itemTypeId={typeId}
                itemSpecIds={useItemSpecIds}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={SpecificationField}
            />}
            <Row /*gutter={[8, 0]}*/ className={renderSpecification ? "pt-2" : ""}>
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
                {specialActionField && <Col xs={5}>
                    {specialActionField}
                </Col>}
                <Col xs={12}>
                    <EntityField
                        name={'codeObj'}
                        label={'Návazná archivní entita'}
                        onlyMainPart={onlyMainPart}
                        area={area}
                        itemTypeId={typeId}
                        itemSpecId={specialActionField ? geoSearchItemType : specId}
                        disabled={renderSpecification ? specId == null : false}
                    />
                </Col>
            </Row>
        </Modal.Body>
        <Modal.Footer>
            <Button type={'submit'} variant={'outline-secondary'} onClick={handleSubmit} disabled={submitting}>
                {i18n('global.action.store')}
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
        specId: selector(state, "specId"),
        onlyMainPart: selector(state, "onlyMainPart"),
        area: selector(state, "area"),
        geoSearchItemType: selector(state, "geoSearchItemType"),
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(RelationPartItemEditModalForm));
