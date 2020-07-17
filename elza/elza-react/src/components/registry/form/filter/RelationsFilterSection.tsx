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
import RelationFilterModal from "../../modal/RelationFilterModal";
import {Area} from "../../../../api/Area";
import {ArchiveEntityResultListVO} from "../../../../api/ArchiveEntityResultListVO";
import {FilteredResultVO} from "../../../../api/FilteredResultVO";
import {ApAccessPointVO} from "../../../../api/ApAccessPointVO";

type OwnProps = {
    submitting: boolean;
    name?: string;
    nameFormSection?: string; // název pro FormSection
    relApi?: (itemTypeId: number, itemSpecId: number, filter: any) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>
}

type Props = {
    formName: string;
} & OwnProps & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const RelationsFilterSection = ({submitting, dispatch, relApi, nameFormSection = "", name = 'ap.ext-search.section.relations'}: Props) => {
    return <FormSection name={nameFormSection} className="filter-section">
        <span className="name-section">{i18n(name)}</span>
        <FieldArray
            name="relFilters"
            dispatch={dispatch}
            component={RelFilters}
            relApi={relApi}
            disabled={submitting}
        />
    </FormSection>
};

interface RelFilterProps {
    itemType: RulDescItemTypeExtVO;
    obj: any;
}

const renderRelFilter = (index: number, disabled: boolean, item: RelFilterProps, remove: () => void) => {
    return <Row className="rel-filter mb-1" key={index}>
        <Col xs={10}>
            {item.itemType && <span className="type">{item.itemType.name}</span>}
            {item.obj && <span className="value">{item.obj.name}</span>}
        </Col>
        <Col xs={2} className="remove">
            <Button size="small" className="fr" disabled={disabled} variant="outline-secondary" onClick={remove}>
                <Icon glyph="fa-trash"/>
            </Button>
        </Col>
    </Row>;
}

interface RelFilterFieldProps extends WrappedFieldArrayProps<string> {
    disabled: boolean;
    dispatch: ThunkDispatch<{}, {}, Action<string>>;
    relApi?: (itemTypeId: number, itemSpecId: number, filter: any) => Promise<ArchiveEntityResultListVO | FilteredResultVO<ApAccessPointVO>>
}

const RelFilters: React.FC<RelFilterFieldProps> = memo(({fields, disabled = false, meta, relApi, dispatch, ...props}) => {
    return <>
        <Button size="small" className="fr" disabled={disabled} variant="outline-secondary" onClick={() => {
            dispatch(
                modalDialogShow(
                    this,
                    i18n('ap.ext-search.section.extends.title'),
                    <RelationFilterModal initialValues={{
                        area: Area.ALLNAMES,
                        onlyMainPart: "false"
                    }} relApi={relApi} onSubmit={(data) => {
                        fields.push(data);
                        dispatch(modalDialogHide());
                    }} />,
                ),
            );
        }}><Icon glyph="fa-plus"/></Button>
        {fields.length > 0 && <div>
        {fields.map((field, index) => {
                const item: any = fields.get(index);
                return renderRelFilter(index, disabled, item, () => {
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

export default connect(mapStateToProps, mapDispatchToProps)(RelationsFilterSection);
