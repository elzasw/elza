import React, {memo} from 'react';
import {FieldArray, FormSection, formValueSelector, InjectedFormProps, WrappedFieldArrayProps} from 'redux-form';
import i18n from "../../../i18n";
import {Button} from "../../../ui";
import {Icon} from "../../../index";
import {connect} from "react-redux";
import {modalDialogHide, modalDialogShow} from "../../../../actions/global/modalDialog";
import {ThunkDispatch} from "redux-thunk";
import {Action} from "redux";
import ExtendsFilterModal from "../../modal/ExtendsFilterModal";
import {RulPartTypeVO} from "../../../../api/RulPartTypeVO";
import {RulDescItemTypeExtVO} from "../../../../api/RulDescItemTypeExtVO";
import {RulDescItemSpecExtVO} from "../../../../api/RulDescItemSpecExtVO";
import {Col, Row} from "react-bootstrap";
import {ArchiveEntityResultListVO} from "../../../../api/ArchiveEntityResultListVO";
import {Area} from "../../../../api/Area";
import {ApAccessPointVO} from "../../../../api/ApAccessPointVO";
import {FilteredResultVO} from "../../../../api/FilteredResultVO";

type OwnProps = {
    submitting: boolean;
    name?: string;
    nameFormSection?: string; // název pro FormSection
    scopeId?: number;
    rulSetsIds?: number[];
}

type Props = {
    formName: string;
    relEntityApi?: (itemTypeId: number, itemSpecId: number, filter: any) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>;
} & OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const ExtendsFilterSection = ({
    submitting, 
    dispatch, 
    array, 
    nameFormSection = "", 
    name = 'ap.ext-search.section.extends', 
    relEntityApi,
    scopeId,
    rulSetsIds
}: Props) => {
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        <FieldArray
            name="extFilters"
            dispatch={dispatch}
            relEntityApi={relEntityApi}
            component={ExtFilters}
            disabled={submitting}
            scopeId={scopeId}
            rulSetsIds={rulSetsIds}
        />
    </FormSection>
};

interface ExtFilterProps {
    partType: RulPartTypeVO;
    itemType: RulDescItemTypeExtVO;
    itemSpec?: RulDescItemSpecExtVO;
    value?: any;
    obj?: ApAccessPointVO;
}

const renderExtFilter = (index: number, disabled: boolean, item: ExtFilterProps, remove: () => void) => {
    return <Row className="ext-filter mb-1" key={index}>
        <Col xs={10}>
            {item.partType && <span className="part">{item.partType.name}</span>}
            {item.itemType && <span className="type">{item.itemType.name}</span>}
            {item.itemSpec && <span className="spec">{item.itemSpec.name}</span>}
            {item.value && <span className="value">{item.value}</span>}
            {item.obj && <span className="value">{item.obj.name}</span>}
        </Col>
        <Col xs={2} className="remove">
            <Button size="small" className="fr" disabled={disabled} variant="outline-secondary" onClick={remove}>
                <Icon glyph="fa-trash"/>
            </Button>
        </Col>
    </Row>;
}

interface ExtFilterFieldProps extends WrappedFieldArrayProps<string> {
    disabled: boolean;
    relEntityApi?: (itemTypeId: number, itemSpecId: number, filter: any) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>;
    dispatch: ThunkDispatch<{}, {}, Action<string>>;
    scopeId?: number;
    rulSetsIds?: number[];
}

const ExtFilters: React.FC<ExtFilterFieldProps> = memo(({
    fields,
    disabled = false,
    meta,
    dispatch,
    relEntityApi,
    scopeId,
    rulSetsIds,
}) => {
    return <>
        <Button size="small" className="fr" disabled={disabled} variant="outline-secondary" onClick={() => {
            dispatch(
                modalDialogShow(
                    this,
                    i18n('ap.ext-search.section.extends.title'),
                    <ExtendsFilterModal
                        initialValues={{
                            onlyMainPart: true,
                            area: Area.ALLNAMES,
                            scopeId,
                        }}
                        rulSetsIds={rulSetsIds}
                        relEntityApi={relEntityApi}
                        onSubmit={(data) => {
                        fields.push(data);
                        dispatch(modalDialogHide());
                    }} />,
                ),
            );
        }}><Icon glyph="fa-plus"/></Button>
        {fields.length > 0 && <div>
        {fields.map((field, index) => {
                const item: any = fields.get(index);
                return renderExtFilter(index, disabled, item, () => {
                    fields.remove(index);
                });
            }
        )}
    </div>}</>
});

const mapStateToProps = (state: any, props: any) => {
    const selector = formValueSelector(props.formName);
    return {
        extFilters: selector(state, 'extFilters'),
    };
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
    dispatch
});

export default connect(mapStateToProps, mapDispatchToProps)(ExtendsFilterSection);
