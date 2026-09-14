import React from 'react';
import {ConfigProps, Form as ReduxForm, InjectedFormProps, reduxForm, SubmitHandler,} from 'redux-form';
import {Col, Modal, Row} from 'react-bootstrap';
import {connect} from "react-redux";
import {Action} from "redux";
import {ThunkDispatch} from "redux-thunk";
import {Button} from "../../ui";
import { FormattedMessage } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import TextFilterSection from "../form/filter/TextFilterSection";
import BaseFilterSection from "../form/filter/BaseFilterSection";
import CreExtFilterSection from "../form/filter/CreExtFilterSection";
import {WebApi} from "../../../actions/WebApi";
import {ApAdvanceSearchFilter} from "elza-api";
import ExtendsFilterSection from "../form/filter/ExtendsFilterSection";
import {ArchiveEntityResultListVO} from "../../../api/ArchiveEntityResultListVO";
import RelationsFilterSection from "../form/filter/RelationsFilterSection";
import './ExtFilterModal.scss';
import {FilteredResultVO} from "../../../api/FilteredResultVO";
import {ApAccessPointVO} from "../../../api/ApAccessPointVO";

const FORM_NAME = "extFilter";

type FormProps = {}

const formConfig: ConfigProps<FormProps> = {
    form: FORM_NAME
};

type Props = {
    refTables: {};
    handleSubmit: SubmitHandler<FormData, any, any>;
    formData?: FormProps;
    submitting: boolean;
    onClose: () => void;
    accessPointId?: number;
    onSubmit: any;
    scopeId?: number;
    rulSetsIds?: number[];
} & ReturnType<typeof mapDispatchToProps> & ReturnType<typeof mapStateToProps> & InjectedFormProps;

const ExtFilterModal = ({
    handleSubmit, 
    onClose, 
    submitting, 
    refTables, 
    reset,
    scopeId,
    rulSetsIds,
}: Props) => {
    const {apTypes} = refTables;

    return <ReduxForm className="ext-filter-modal" onSubmit={handleSubmit}>
        <Modal.Body>
            <Row>
                <Col xs={6}>
                    <TextFilterSection submitting={submitting}/>
                    <BaseFilterSection hideType submitting={submitting} types={apTypes.items}/>
                </Col>
                <Col xs={6}>
                    <CreExtFilterSection submitting={submitting}/>
                    <RelationsFilterSection rulSetsIds={rulSetsIds} scopeId={scopeId} formName={FORM_NAME} submitting={submitting}/>
                    <ExtendsFilterSection rulSetsIds={rulSetsIds} scopeId={scopeId} formName={FORM_NAME} submitting={submitting}/>
                </Col>
            </Row>
        </Modal.Body>
        <Modal.Footer>
            <Button disabled={submitting} type="submit" variant="outline-secondary">{<FormattedMessage {...globalMessages.use} />}</Button>
            <Button variant="link" onClick={onClose} disabled={submitting}>
                {<FormattedMessage {...globalMessages.close} />}
            </Button>
        </Modal.Footer>
    </ReduxForm>;
};

const mapDispatchToProps = (dispatch: ThunkDispatch<{}, {}, Action<string>>) => ({
    dispatch
});

const mapStateToProps = (state: any) => {
    return {
        refTables: state.refTables,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(reduxForm<any, any>(formConfig)(ExtFilterModal));
