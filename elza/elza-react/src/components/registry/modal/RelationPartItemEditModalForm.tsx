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
import {computeAllowedItemSpecIds, findViewItemType} from "../../../utils/ItemInfo";
import * as AreaInfo from "../form/filter/AreaInfo";
import {ArchiveEntityRel} from "../field/ArchiveEntityRel";
import storeFromArea from "../../../shared/utils/storeFromArea";
import {AP_VIEW_SETTINGS} from "../../../constants";
import {DetailStoreState} from "../../../types";
import {ApViewSettings} from "../../../api/ApViewSettings";
import {objectById} from "../../../shared/utils";
import {RulPartTypeVO} from "../../../api/RulPartTypeVO";

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
    partTypeId: number;
    apTypeId: number;
    scopeId: number;
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
                                           scopeId,
                                           partTypeId,
                                           geoSearchItemType,
                                           submitting,
                                           apViewSettings,
                                           apTypeId,
                                           geoSpecId
                                       }: Props) => {
    if (!refTables) {
        return <div/>;
    }

    let itemType = refTables.descItemTypes.itemsMap[typeId] as RulDescItemTypeExtVO;
    const renderSpecification = typeId && itemType && itemType.useSpecification;

    const useItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, itemType, specId);

    const part: RulPartTypeVO | undefined = refTables.partTypes.itemsMap[partTypeId];

    let geoType;

    const getSpecialActionField = () => {
        if (apViewSettings.data && itemType && part) {
            const apViewSettingRule = apViewSettings.data.rules[apViewSettings.data.typeRuleSetMap[apTypeId]];
            const itemTypeSettings = findViewItemType(apViewSettingRule.itemTypes, part, itemType.code);
            if (itemTypeSettings && itemTypeSettings.geoSearchItemType) {
                const getItemType = objectById(refTables.descItemTypes.items, itemTypeSettings.geoSearchItemType, 'code') as RulDescItemTypeExtVO;
                // const geoUseItemSpecIds = computeAllowedItemSpecIds(itemTypeAttributeMap, getItemType);
                // Allow all available specIds, instead of just the ones allowed by itemType.
                const geoUseItemSpecIds = getItemType.descItemSpecs.map((spec)=>spec.id);
                if (getItemType) {
                    geoType = getItemType;
                    return <Field
                        name="geoSpecId"
                        label={getItemType.name}
                        itemTypeId={getItemType.id}
                        itemSpecIds={geoUseItemSpecIds}
                        disabled={submitting}
                        component={ReduxFormFieldErrorDecorator}
                        renderComponent={SpecificationField}
                    />;
                }
            }
        }
        return null;
    };

    const specialActionField = getSpecialActionField();

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
                    <ArchiveEntityRel
                        name={'codeObj'}
                        label={i18n('ap.ext-search.section.relations.obj')}
                        onlyMainPart={onlyMainPart}
                        area={area}
                        scopeId={scopeId}
                        itemTypeId={typeId}
                        modifyFilterData={data => {
                            if (renderSpecification) {
                                data.relFilters = [{
                                    relTypeId: typeId,
                                    relSpecId: specId
                                }]
                            } else {
                                data.extFilters = [{
                                    itemTypeId: geoType && geoType.id,
                                    itemSpecId: geoSpecId,
                                    partTypeCode: part?.code
                                }]
                            }
                            return data;
                        }}
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
        geoSpecId: selector(state, "geoSpecId"),
        geoSearchItemType: selector(state, "geoSearchItemType"),
        apViewSettings: storeFromArea(state, AP_VIEW_SETTINGS) as DetailStoreState<ApViewSettings>,
    }
};

export default connect(mapStateToProps)(reduxForm<any, any>(formConfig)(RelationPartItemEditModalForm));
