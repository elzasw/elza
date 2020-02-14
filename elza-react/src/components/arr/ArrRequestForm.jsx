/**
 * Formulář Požadavku na digitaliyaci nebo skartaci/delimitaci.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput, Loading} from 'components/shared';
import {Modal, Button, Input, Form} from 'react-bootstrap';
import {createDigitizationName} from './ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx';
import * as arrRequestActions from "actions/arr/arrRequestActions";
import {refExternalSystemsFetchIfNeeded} from 'actions/refTables/externalSystems';

const ArrRequestForm = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    static propTypes = {
        fundVersionId: PropTypes.number.isRequired,
        type: PropTypes.oneOf(["DAO", "DIGITIZATION"]),
    };

    static fields = [
        'requestId',
        'digitizationFrontdesk',
        'daoType',
        'description',
    ];

    static validate = (values, props) => {
        const errors = {};
        if (props.type === "DIGITIZATION") {
            if (values.digitizationFrontdesk === "") {
                errors.digitizationFrontdesk = i18n('global.validation.required');
            }
        }
        if (props.type === "DAO") {
            if (values.daoType === "") {
                errors.daoType = i18n('global.validation.required');
            }
        }
        return errors;
    };

    componentDidMount() {
        this.props.dispatch(refExternalSystemsFetchIfNeeded());
        this.fetchDigitizationRequestList({}, this.props);
        this.trySelectRequest(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(refExternalSystemsFetchIfNeeded());
        this.fetchDigitizationRequestList(this.props, nextProps);
    }

    trySelectRequest = (props) => {
        const {preparedRequestList, fields: {requestId, description, digitizationFrontdesk}} = props;
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
                const digReq = preparedRequestList.rows[index];
                requestId.onChange(digReq.id);
                description.onChange(digReq.description);
                digitizationFrontdesk.onChange(digReq.digitizationFrontdeskId)
            }
        } else {
            requestId.onChange("");
            description.onChange("");
            const digitizationFrontdesks = this.getDigitizationFrontdesks();
            if (digitizationFrontdesks.length == 1) {
                digitizationFrontdesk.onChange(digitizationFrontdesks[0].id)
            } else {
                digitizationFrontdesk.onChange(-1);
            }
        }
    }

    getDigitizationFrontdesks = () => {
        const {externalSystems} = this.props;
        let digitizationFrontdesks = [];
        if (externalSystems.fetched) {
            externalSystems.items.forEach((item) => {
                if (item['@class'] === '.ArrDigitizationFrontdeskSimpleVO') {
                    digitizationFrontdesks.push(item);
                }
            });
        }
        return digitizationFrontdesks;
    };

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

    handleDigitizationFrontdeskChange = (e) => {
        const {fields: {digitizationFrontdesk}} = this.props;
        const id = e.target.value;
        digitizationFrontdesk.onChange(id);
    };

    handleRequestChange = (e) => {
        const {preparedRequestList, fields: {requestId, description, digitizationFrontdesk}} = this.props;
        const id = e.target.value;

        requestId.onChange(e);

        if (id !== "") {  // vybrána nějaká možnost, převezmeme z ní popis
            const digReq = preparedRequestList.rows[indexById(preparedRequestList.rows, id)];
            digitizationFrontdesk.onChange(digReq.digitizationFrontdeskId);
            description.onChange(digReq.description);
        } else {    // vybrána možnost nového požadavku, vynulujeme popis
            description.onChange("");
            const digitizationFrontdesks = this.getDigitizationFrontdesks();
            if (digitizationFrontdesks.length == 1) {
                digitizationFrontdesk.onChange(digitizationFrontdesks[0].id)
            } else {
                digitizationFrontdesk.onChange(-1);
            }
        }
    };

    render() {
        const {type, handleSubmit, onSubmitForm, userDetail, preparedRequestList, onClose, fields: {requestId, daoType, description, digitizationFrontdesk}, externalSystems} = this.props;
        const requestFetched = preparedRequestList.fetched && !preparedRequestList.isFetching;

        const showDaoTypeSelect = type === "DAO";
        const showDigitizationFrontdeskSelect = type === "DIGITIZATION";
        const showRequestFields = (type === "DAO" && daoType.value || type !== "DAO");

        let digitizationFrontdesks = [];

        if (showDigitizationFrontdeskSelect) {
            digitizationFrontdesks = this.getDigitizationFrontdesks();
        }

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
                {showDigitizationFrontdeskSelect && <FormInput
                    label={i18n("arr.request.title.daoRequest.digitizationFrontdesk")}
                    componentClass="select" {...digitizationFrontdesk}
                    onChange={this.handleDigitizationFrontdeskChange}
                    disabled={!requestFetched}
                >
                    <option key={-1} value=""></option>
                    {digitizationFrontdesks.map(digFrontdesk => <option value={digFrontdesk.id} key={digFrontdesk.id}>{digFrontdesk.name}</option>)}
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
        externalSystems: refTables.externalSystems,
        userDetail,
        preparedRequestList: state.app.preparedRequestList,
        initialValues: { daoType: null, requestId: null, description: null }
    }
}

export default reduxForm({
        form: 'arrRequestForm',
        fields: ArrRequestForm.fields,
        validate: ArrRequestForm.validate
    }, mapStateToProps
)(ArrRequestForm)
