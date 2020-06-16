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
    itemTypeId: number;
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
                                           itemSpecId,
                                           itemTypeId,
                                           geoSearchItemType,
                                           submitting,
                                       }: Props) => {
    if (!refTables) {
        return <div/>;
    }

    let itemType = refTables.descItemTypes.itemsMap[itemTypeId] as RulDescItemTypeExtVO;
    const renderSpecification = itemTypeId && itemType && itemType.useSpecification;

    const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, itemSpecId);

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

    //todo: prepsat AE fieldy
    //const EntityField = specialActionField ? ff.ArchiveEntitRelGeoAdminClass : ff.ArchiveEntitRel;

    return <Form onSubmit={handleSubmit}>
        <Modal.Body>
            {renderSpecification && <Field
                name="itemSpecId"
                label="Specifikace vztahu"
                itemTypeI={itemTypeId}
                itemSpecIds={useItemSpecIds}
                component={ReduxFormFieldErrorDecorator}
                renderComponent={SpecificationField}
            />}
            <Row /*gutter={[8, 0]}*/ className={renderSpecification ? "pt-2" : ""}>
                <Col xs={4}>
                    <Field
                        name={'area'}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={Form.Control}
                        as={'select'}
                    >
                        {/* TODO: kde vzit - v CAM to byla AreaEnumInfo */}
                    </Field>
                </Col>
                <Col xs={3}>
                    <Field
                        name="onlyMainPart"
                        component={ReduxFormFieldErrorDecorator}
                        rendercomponent={Form.Check}
                        type={'switch'}
                    />
                </Col>
                {specialActionField && <Col xs={5}>
                    {specialActionField}
                </Col>}
                <Col xs={12}>
                    <Field
                        name={'codeObj'}
                        label={'Návazná archivní entita'}
                        useObj={true}
                        onlyMainPart={onlyMainPart}
                        area={area}
                        itemTypeId={itemTypeId}
                        itemSpecId={specialActionField ? geoSearchItemType : itemSpecId}
                        disabled={renderSpecification && itemSpecId == null}
                        title={!renderSpecification ? 'Návazná archivní entita' : (itemSpecId == null ? 'Pro vybrání návazné archivní entity je nutné nejdříve zvolit specifikaci' : 'Návazná archivní entita')}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={'input'}
                        // renderComponent={EntityField}
                    />
                    {/* TODO: zde (vyse) musi byt EntityField, az se prepisou AE fieldy */}
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
        itemSpecId: selector(state, "itemSpecId"),
        onlyMainPart: selector(state, "onlyMainPart"),
        area: selector(state, "area"),
        geoSearchItemType: selector(state, "geoSearchItemType"),
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(RelationPartItemEditModalForm));
