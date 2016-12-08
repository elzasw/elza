/**
 * Formulář Požadavku na digitaliyaci.
 */

import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button, Input, Form} from 'react-bootstrap';
import {createDigitizationName} from './ArrUtils.jsx'
import {indexById} from 'stores/app/utils.jsx';
import * as digitizationActions from "actions/arr/digitizationActions";

const DigitizationRequestForm = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        this.fetchDigitizationRequestList({}, this.props);

    }

    componentWillReceiveProps(nextProps) {
        this.fetchDigitizationRequestList(this.props, nextProps);
    }

    fetchDigitizationRequestList = (prevProps, nextProps) => {
        this.props.dispatch(digitizationActions.fetchPreparedListIfNeeded(nextProps.fundVersionId));

        const digitizationRequestFetched = nextProps.preparedDigitizationRequestList.fetched && !nextProps.preparedDigitizationRequestList.isFetching;
        const prevDigitizationRequestFetched = prevProps.preparedDigitizationRequestList &&  prevProps.preparedDigitizationRequestList.fetched && !prevProps.preparedDigitizationRequestList.isFetching;
        if (!prevDigitizationRequestFetched && digitizationRequestFetched) {    // seznam byl načten, zkusíme vybrat jednu z možností, pokud není nic vybráno
            const {fields: {digitizationRequestId, description}} = nextProps;
            if (nextProps.preparedDigitizationRequestList.rows.length > 0 && digitizationRequestId.value === "") {
                digitizationRequestId.onChange(nextProps.preparedDigitizationRequestList.rows[0].id)
                description.onChange(nextProps.preparedDigitizationRequestList.rows[0].description)
            }
        }
    }

    static PropTypes = {
        nodeId: React.PropTypes.number.isRequired,
        fundVersionId: React.PropTypes.number.isRequired,
    };

    static fields = [
        'digitizationRequestId',
        'description',
    ];

    handleRequestChange = (e) => {
        const {preparedDigitizationRequestList, fields: {digitizationRequestId, description}} = this.props;
        const id = e.target.value;

        if (id !== "") {  // vybrána nějaká možnost, převezmeme z ní popis
            const digReq = preparedDigitizationRequestList.rows[indexById(preparedDigitizationRequestList.rows, id)];
            digitizationRequestId.onChange(e);
            description.onChange(digReq.description);
        } else {    // vybrána možnost nového požadavku, vynulujeme popis
            digitizationRequestId.onChange(e);
            description.onChange("");
        }
    }

    render() {
        const {handleSubmit, onSubmitForm, userDetail, preparedDigitizationRequestList, onClose, fields: {digitizationRequestId, description}} = this.props;
        const digitizationRequestFetched = preparedDigitizationRequestList.fetched && !preparedDigitizationRequestList.isFetching;

        const form = (
            <div>
                <FormInput label={i18n("arr.request.title.digitizationRequest")} componentClass="select" {...digitizationRequestId} onChange={this.handleRequestChange} disabled={!digitizationRequestFetched}>
                    <option key={-1} value="">{i18n('digitizationRequest.title.newRequest')}</option>
                    {preparedDigitizationRequestList.fetched
                    && !preparedDigitizationRequestList.isFetching
                    && preparedDigitizationRequestList.rows.map(digReq => <option value={digReq.id}
                                                                          key={digReq.id}>{createDigitizationName(digReq, userDetail)}</option>)}
                </FormInput>
                <FormInput label={i18n("arr.request.title.description")} componentClass="textarea" {...description} disabled={!digitizationRequestFetched} />
            </div>
        )

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
}

function mapStateToProps(state) {
    const {arrRegion, refTables, userDetail} = state
    return {
        funds: arrRegion.funds,
        refTables,
        packets: arrRegion.packets,
        userDetail,
        preparedDigitizationRequestList: state.app.preparedDigitizationRequestList
    }
}

export default reduxForm({
        form: 'digitizationRequestForm',
        fields: DigitizationRequestForm.fields,
    }, mapStateToProps
)(DigitizationRequestForm)
