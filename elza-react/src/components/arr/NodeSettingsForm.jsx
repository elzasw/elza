import React from 'react';
import ReactDOM from 'react-dom';
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Modal, Button, Checkbox, Form} from 'react-bootstrap';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {decorateFormField, submitForm} from 'components/form/FormUtils.jsx'
import {visiblePolicyFetchIfNeeded} from 'actions/arr/visiblePolicy.jsx'
import {modalDialogHide} from 'actions/global/modalDialog.jsx'
import {Row, Col, Nav, NavItem, Radio} from 'react-bootstrap'
import {FormInput, Loading} from "../shared/index";
import getMapFromList from "../../shared/utils/getMapFromList";

import './NodeSettingsForm.scss'

const VIEW_KEYS = {
  RULES: "RULES",
  EXTENSIONS: "EXTENSIONS",
};

const VIEW_POLICY_STATE = {
    PARENT:"PARENT",
    NODE:"NODE"
};

class NodeSettingsForm extends AbstractReactComponent {
    state = {activeView: VIEW_KEYS.RULES};

    static VIEW_POLICY_STATE = VIEW_POLICY_STATE;

    componentDidMount() {
        this.loadVisiblePolicy();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.loadVisiblePolicy(nextProps);
    }

    loadVisiblePolicy = (nextProps = this.props) => {
        const {nodeId, fundVersionId} = nextProps;
        this.props.dispatch(visiblePolicyFetchIfNeeded(nodeId, fundVersionId));
    };

    changeView = (activeView) => {
        this.setState({activeView});
    };

    render() {
        const {activeView} = this.state;

        const {fields: {records, rules, nodeExtensions}, handleSubmit, onClose, nodeId, fundVersionId, submitting, visiblePolicy, visiblePolicyTypes, arrRegion} = this.props;
        if (!visiblePolicy.fetched) {
            return <Modal.Body>
                <Loading />
            </Modal.Body>
        }

        const {otherData: {parentExtensions, availableExtensions}} = visiblePolicy;

        const availableExtensionsMap = getMapFromList(availableExtensions, 'id');

        let activeFund = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
        }

        let visiblePolicyTypeItems;

        if (activeFund == null) {
            visiblePolicyTypeItems = visiblePolicyTypes;
        } else {
            let activeVersion = activeFund.activeVersion;
            visiblePolicyTypeItems = {};

            for(let id in visiblePolicyTypes.items) {
                let item = visiblePolicyTypes.items[id];
                if (activeVersion.ruleSetId === item.ruleSetId) {
                    visiblePolicyTypeItems[id] = item;
                }
            }
        }

        return (
            <Form className="node-settings-form" onSubmit={handleSubmit}>
                <Modal.Body>
                    <Row>
                        <Col sm={3} className="menu">
                            <Nav bsStyle="pills" stacked activeKey={activeView} onSelect={this.changeView}>
                                <NavItem eventKey={VIEW_KEYS.RULES}>{i18n('visiblePolicy.rules')}</NavItem>
                                <NavItem eventKey={VIEW_KEYS.EXTENSIONS}>{i18n('visiblePolicy.extensions')}</NavItem>
                            </Nav>
                        </Col>
                        <Col sm={9} className="view">
                            {activeView === VIEW_KEYS.RULES && <Row key={VIEW_KEYS.RULES}>
                                <Col xs={12}>
                                    <FormInput type="radio" {...rules} checked={rules.value === VIEW_POLICY_STATE.PARENT} value={VIEW_POLICY_STATE.PARENT} label={i18n('visiblePolicy.rules.parent')} />
                                    <FormInput type="radio" {...rules} checked={rules.value === VIEW_POLICY_STATE.NODE} value={VIEW_POLICY_STATE.NODE} label={i18n('visiblePolicy.rules.node')} />
                                    <div className="listbox-wrapper">
                                        <div className="listbox-container">
                                            {records.map((val, index) => {
                                                const {checked, name, onFocus, onChange, onBlur} = val.checked;
                                                const wantedProps = {checked, name, onFocus, onChange, onBlur};
                                                return <Checkbox {...wantedProps} disabled={rules.value !== "NODE"} key={index} value={true}>{visiblePolicyTypeItems[val.id.initialValue].name}</Checkbox>
                                            })}
                                        </div>
                                    </div>
                                </Col>
                            </Row>}
                            {activeView === VIEW_KEYS.EXTENSIONS && <Row key={VIEW_KEYS.EXTENSIONS}>
                                <Col xs={12}>
                                    <h4>{i18n('visiblePolicy.rules.parent')}</h4>
                                    <div className="listbox-wrapper">
                                        <div className="listbox-container">
                                            {parentExtensions && parentExtensions.length > 0 ? parentExtensions.map((i,index) => <div key={index}>
                                                {i.name}
                                            </div>) : "Nejsou aktivní žádná rozšíření"}
                                        </div>
                                    </div>
                                </Col>
                                <Col xs={12}>
                                    <h4>{i18n('visiblePolicy.rules.node')}</h4>
                                    <div className="listbox-wrapper">
                                        <div className="listbox-container">
                                            {nodeExtensions && nodeExtensions.length > 0 ? nodeExtensions.map((val, index) => {
                                                const {checked, name, onFocus, onChange, onBlur} = val.checked;
                                                const wantedProps = {checked, name, onFocus, onChange, onBlur};
                                                return <Checkbox {...wantedProps} key={index} value={true}>
                                                    {availableExtensionsMap[val.id.initialValue] ? availableExtensionsMap[val.id.initialValue].name : objectById(visiblePolicy.otherData.nodeExtensions, val.id.initialValue).name}
                                                </Checkbox>
                                            }) : "Nejsou dostupná žádná rozšíření"}
                                        </div>
                                    </div>
                                </Col>
                            </Row>}
                        </Col>
                    </Row>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" disabled={submitting}>{i18n('visiblePolicy.action.save')}</Button>
                    <Button bsStyle="link" disabled={submitting} onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </Form>
        )
    }
}

export default reduxForm({
    form: 'nodeSettingsForm',
    fields: ['rules', 'records[].id', 'records[].checked', 'nodeExtensions[].id', 'nodeExtensions[].checked']
}, state => {
    const {visiblePolicy} = state.arrRegion;
    const allExtensions = [];
    let rules = null;
    if (visiblePolicy.otherData !== null) {
        const {nodeExtensions, availableExtensions, nodePolicyTypeIdsMap} = visiblePolicy.otherData;
        const nodeExtensionsMap = getMapFromList(nodeExtensions, 'code');
        availableExtensions.forEach(i => {
            const checked = nodeExtensionsMap.hasOwnProperty(i.code);
            allExtensions.push({...i, checked});
            if (checked) {
                delete nodeExtensionsMap[i.code];
            }
        });
        allExtensions.concat(Object.values(nodeExtensionsMap).map(i => ({...i, checked: true})));
        rules = Object.values(nodePolicyTypeIdsMap).length > 0 ? VIEW_POLICY_STATE.NODE : VIEW_POLICY_STATE.PARENT
    }

    return {
        initialValues: {
            rules,
            records: visiblePolicy.data,
            nodeExtensions: allExtensions
        },
        visiblePolicy: visiblePolicy,
        visiblePolicyTypes: state.refTables.visiblePolicyTypes,
        arrRegion: state.arrRegion
    }
})(NodeSettingsForm);
