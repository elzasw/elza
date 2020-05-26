import React from 'react';
import {Field, FieldArray, formValueSelector, reduxForm} from 'redux-form';
import {AbstractReactComponent, FormInputField, i18n} from 'components/shared';
import {Col, Form, Modal, Nav, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {visiblePolicyFetchIfNeeded} from 'actions/arr/visiblePolicy.jsx';
import {Loading} from '../shared/index';
import getMapFromList from '../../shared/utils/getMapFromList';

import './NodeSettingsForm.scss';
import {connect} from 'react-redux';
import {CheckboxArrayField} from 'components/arr/nodeForm/CheckboxArrayField';

const VIEW_KEYS = {
    RULES: 'RULES',
    EXTENSIONS: 'EXTENSIONS',
};

export const VIEW_POLICY_STATE = {
    PARENT: 'PARENT',
    NODE: 'NODE',
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

    changeView = activeView => {
        this.setState({activeView});
    };

    render() {
        const {activeView} = this.state;

        const {
                  rulesValue,
                  handleSubmit,
                  onClose,
                  pristine,
                  submitting,
                  visiblePolicy,
                  visiblePolicyTypes,
                  arrRegion,
              } = this.props;
        if (!visiblePolicy.fetched) {
            return (
                <Modal.Body>
                    <Loading/>
                </Modal.Body>
            );
        }

        const {
                  otherData: {parentExtensions, availableExtensions},
              } = visiblePolicy;

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

            for (let id in visiblePolicyTypes.itemsMap) {
                let item = visiblePolicyTypes.itemsMap[id];
                if (activeVersion.ruleSetId === item.ruleSetId) {
                    visiblePolicyTypeItems[id] = item;
                }
            }
        }

        console.log(":::", visiblePolicyTypeItems);

        return (
            <Form className="node-settings-form" onSubmit={handleSubmit}>
                <Modal.Body>
                    <Row>
                        <Col sm={3} className="menu">
                            <Nav variant="pills" activeKey={activeView} onSelect={this.changeView}>
                                <Nav.Item>
                                    <Nav.Link eventKey={VIEW_KEYS.RULES}>
                                        {i18n('visiblePolicy.rules')}
                                    </Nav.Link>
                                </Nav.Item>
                                <Nav.Item>
                                    <Nav.Link eventKey={VIEW_KEYS.EXTENSIONS}>
                                        {i18n('visiblePolicy.extensions')}
                                    </Nav.Link>
                                </Nav.Item>
                            </Nav>
                        </Col>
                        <Col sm={9} className="view">
                            {activeView === VIEW_KEYS.RULES && (
                                <Row key={VIEW_KEYS.RULES}>
                                    <Col xs={12}>
                                        <Field
                                            name="rules"
                                            type="radio"
                                            component={FormInputField}
                                            label={i18n('visiblePolicy.rules.parent')}
                                            value={VIEW_POLICY_STATE.PARENT}
                                        />
                                        <Field
                                            name="rules"
                                            type="radio"
                                            component={FormInputField}
                                            label={i18n('visiblePolicy.rules.node')}
                                            value={VIEW_POLICY_STATE.NODE}
                                        />

                                        <div className="listbox-wrapper">
                                            <div className="listbox-container">
                                                <FieldArray
                                                    name="records"
                                                    component={CheckboxArrayField}
                                                    items={visiblePolicyTypeItems}
                                                    disabled={rulesValue !== 'NODE'}
                                                />
                                                {/*{records.map((val, index) => {*/}
                                                {/*    const {checked, name, onFocus, onChange, onBlur} = val.checked;*/}
                                                {/*    const wantedProps = {checked, name, onFocus, onChange, onBlur};*/}
                                                {/*    return (*/}
                                                {/*        <FormCheck*/}
                                                {/*            {...wantedProps}*/}
                                                {/*            disabled={rules.value !== 'NODE'}*/}
                                                {/*            key={index}*/}
                                                {/*            value={true}*/}
                                                {/*        >*/}
                                                {/*            {visiblePolicyTypeItems[val.id.initialValue].name}*/}
                                                {/*        </FormCheck>*/}
                                                {/*    );*/}
                                                {/*})}*/}
                                            </div>
                                        </div>
                                    </Col>
                                </Row>
                            )}
                            {activeView === VIEW_KEYS.EXTENSIONS && (
                                <Row key={VIEW_KEYS.EXTENSIONS}>
                                    <Col xs={12}>
                                        <h4>{i18n('visiblePolicy.rules.parent')}</h4>
                                        <div className="listbox-wrapper">
                                            <div className="listbox-container">
                                                {parentExtensions && parentExtensions.length > 0
                                                    ? parentExtensions.map((i, index) => (
                                                        <div key={index}>{i.name}</div>
                                                    ))
                                                    : 'Nejsou aktivní žádná rozšíření'}
                                            </div>
                                        </div>
                                    </Col>
                                    <Col xs={12}>
                                        <h4>{i18n('visiblePolicy.rules.node')}</h4>
                                        <div className="listbox-wrapper">
                                            <div className="listbox-container">
                                                <FieldArray
                                                    name="nodeExtensions"
                                                    component={CheckboxArrayField}
                                                    items={availableExtensionsMap}
                                                />
                                                {/*    {nodeExtensions && nodeExtensions.length > 0*/}
                                                {/*        ? nodeExtensions.map((val, index) => {*/}
                                                {/*              const {*/}
                                                {/*                  checked,*/}
                                                {/*                  name,*/}
                                                {/*                  onFocus,*/}
                                                {/*                  onChange,*/}
                                                {/*                  onBlur,*/}
                                                {/*              } = val.checked;*/}
                                                {/*              const wantedProps = {*/}
                                                {/*                  checked,*/}
                                                {/*                  name,*/}
                                                {/*                  onFocus,*/}
                                                {/*                  onChange,*/}
                                                {/*                  onBlur,*/}
                                                {/*              };*/}
                                                {/*              return (*/}
                                                {/*                  <FormCheck {...wantedProps} key={index} value={true}>*/}
                                                {/*                      {availableExtensionsMap[val.id.initialValue]*/}
                                                {/*                          ? availableExtensionsMap[val.id.initialValue].name*/}
                                                {/*                          : objectById(*/}
                                                {/*                                visiblePolicy.otherData.nodeExtensions,*/}
                                                {/*                                val.id.initialValue,*/}
                                                {/*                            ).name}*/}
                                                {/*                  </FormCheck>*/}
                                                {/*              );*/}
                                                {/*          })*/}
                                                {/*        : 'Nejsou dostupná žádná rozšíření'}*/}
                                            </div>
                                        </div>
                                    </Col>
                                </Row>
                            )}
                        </Col>
                    </Row>
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={pristine || submitting}>
                        {i18n('visiblePolicy.action.save')}
                    </Button>
                    <Button variant="link" disabled={submitting} onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const selector = formValueSelector('nodeSettingsForm');

const mapState = (state) => {
    return {
        visiblePolicy: state.arrRegion.visiblePolicy,
        visiblePolicyTypes: state.refTables.visiblePolicyTypes,
        rulesValue: selector(state, 'rules'),
        arrRegion: state.arrRegion,
    };
};
const connector = connect(mapState);

export default reduxForm({
    form: 'nodeSettingsForm',
})(connector(NodeSettingsForm));
