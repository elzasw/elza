/**
 * Formulář Požadavku na digitaliyaci nebo skartaci/delimitaci.
 */

import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button, Input, Form} from 'react-bootstrap';
import {createDigitizationName} from './ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx';
import * as arrRequestActions from "actions/arr/arrRequestActions";

const ArrRequestForm = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    static PropTypes = {
        fundVersionId: React.PropTypes.number.isRequired,
        type: React.PropTypes.oneOf(["DAO", "DIGITIZATION"]),
    };

    static fields = [
        'requestId',
        'daoType',
        'description',
    ];

    componentDidMount() {
        this.fetchDigitizationRequestList({}, this.props);
        this.trySelectRequest(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.fetchDigitizationRequestList(this.props, nextProps);
    }

    trySelectRequest = (props) => {
        const {preparedRequestList, fields: {requestId, description}} = props;
        if (preparedRequestList.rows.length > 0 && requestId.value === "") {
            let index = -1;
            for (let i = 0; i < preparedRequestList.rows.length; i++) {
                let row = preparedRequestList.rows[i];
                if (props.userDetail.username == row.username || (props.userDetail.username == 'admin' && row.username == null)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                requestId.onChange(preparedRequestList.rows[index].id);
                description.onChange(preparedRequestList.rows[index].description);
            }
        } else {
            requestId.onChange("");
            description.onChange("");
        }
    }

    fetchDigitizationRequestList = (prevProps, nextProps) => {
        this.props.dispatch(arrRequestActions.fetchPreparedListIfNeeded(nextProps.fundVersionId, nextProps.type));

        const requestFetched = nextProps.preparedRequestList.fetched && !nextProps.preparedRequestList.isFetching;
        const prevRequestFetched = prevProps.preparedRequestList &&  prevProps.preparedRequestList.fetched && !prevProps.preparedRequestList.isFetching;
        if (!prevRequestFetched && requestFetched) {    // seznam byl načten, zkusíme vybrat jednu z možností, pokud není nic vybráno
            this.trySelectRequest(nextProps);
        }
    };

    handleDaoTypeChange = (e) => {
        const {fields: {daoType, requestId, description}} = this.props;
        const value = e.target.value;
        daoType.onChange(e);
        requestId.onChange("");
        description.onChange("");
        this.props.dispatch(arrRequestActions.filterPreparedList({ daoType: value }));
        this.trySelectRequest(this.props);
    };

    handleRequestChange = (e) => {
        const {preparedRequestList, fields: {requestId, description}} = this.props;
        const id = e.target.value;

        if (id !== "") {  // vybrána nějaká možnost, převezmeme z ní popis
            const digReq = preparedRequestList.rows[indexById(preparedRequestList.rows, id)];
            requestId.onChange(e);
            description.onChange(digReq.description);
        } else {    // vybrána možnost nového požadavku, vynulujeme popis
            requestId.onChange(e);
            description.onChange("");
        }
    };

    render() {
        const {type, handleSubmit, onSubmitForm, userDetail, preparedRequestList, onClose, fields: {requestId, daoType, description}} = this.props;
        const requestFetched = preparedRequestList.fetched && !preparedRequestList.isFetching;

        const showDaoTypeSelect = type === "DAO";
        const showRequestFields = (type === "DAO" && daoType.value || type !== "DAO");

        const form = (
            <div>
                {showDaoTypeSelect && <FormInput label={i18n("arr.request.title.daoRequest.type")} componentClass="select" {...daoType} onChange={this.handleDaoTypeChange}>
                    <option key={-1} value=""></option>
                    <option key={"DESTRUCTION"} value="DESTRUCTION">{i18n('arr.request.title.type.dao.DESTRUCTION')}</option>
                    <option key={"TRANSFER"} value="TRANSFER">{i18n('arr.request.title.type.dao.TRANSFER')}</option>
                </FormInput>}
                {showRequestFields && <FormInput
                    label={i18n("arr.request.title.digitizationRequest")}
                    componentClass="select"
                    {...requestId}
                    onChange={this.handleRequestChange}
                    disabled={!requestFetched}
                >
                    <option key={-1} value="">{i18n('arr.request.title.newRequest')}</option>
                    {preparedRequestList.fetched
                    && !preparedRequestList.isFetching
                    && preparedRequestList.rows.map(digReq => <option value={digReq.id} key={digReq.id}>{createDigitizationName(digReq, userDetail)}</option>)}
                </FormInput>}
                {showRequestFields && <FormInput label={i18n("arr.request.title.description")} componentClass="textarea" {...description} disabled={!requestFetched} />}
            </div>
        );

        return (
            <Form onSubmit={handleSubmit(onSubmitForm)}>
                <Modal.Body>
                    {form}
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" onClick={handleSubmit(onSubmitForm.bind(this, false))}>{i18n('global.action.store')}</Button>
                    <Button type="submit" onClick={handleSubmit(onSubmitForm.bind(this, true))}>{i18n('arr.request.action.storeAndSend')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </Form>
        )
    }
};

function mapStateToProps(state) {
    const {arrRegion, refTables, userDetail} = state
    return {
        funds: arrRegion.funds,
        refTables,
        packets: arrRegion.packets,
        userDetail,
        preparedRequestList: state.app.preparedRequestList,
        initialValues: { daoType: null, requestId: null, description: null }
    }
}

export default reduxForm({
        form: 'arrRequestForm',
        fields: ArrRequestForm.fields,
    }, mapStateToProps
)(ArrRequestForm)
