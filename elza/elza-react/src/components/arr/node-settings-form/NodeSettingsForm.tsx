import { CheckboxGroup } from 'components/arr/nodeForm/CheckboxArrayField';
import { FormInputField, i18n } from 'components/shared';
import React, { FC, useState } from 'react';
import { Col, Form, Modal, Nav, Row } from 'react-bootstrap';
import { connect } from 'react-redux';
import { Field, formValueSelector, InjectedFormProps, reduxForm } from 'redux-form';
import { AppState } from "../../../typings/store";
import { Button } from '../../ui';
import './NodeSettingsForm.scss';
import { VIEW_KEYS, VIEW_POLICY_STATE } from "./static-data";

interface OwnProps {
    onClose: () => void;
}

export interface NodeSettingsFormFields {
    rules: VIEW_POLICY_STATE;
    records: Record<string | number, boolean>;
    nodeExtensions: Record<string | number, boolean>
}

const FORM = 'nodeSettingsForm';

const NodeSettingsForm:FC<
    InjectedFormProps & 
    ReturnType<typeof mapStateToProps> & 
    OwnProps
> = ({
    rulesValue,
    handleSubmit,
    onClose,
    pristine,
    submitting,
    visiblePolicy,
    visiblePolicyTypes,
    arrRegion,
}) => {
    const [activeView, setActiveView] = useState(VIEW_KEYS.RULES);

    if(!visiblePolicy?.otherData){
        return <></>
    }

    const changeView = (view: VIEW_KEYS) => {
        setActiveView(view);
    };

    const {
        otherData: {
            parentExtensions, 
            availableExtensions
        },
    } = visiblePolicy;

    const activeFund = arrRegion.activeIndex != null ? 
    arrRegion.funds[arrRegion.activeIndex] : 
    null;

    let visiblePolicyTypeItems = visiblePolicyTypes?.items ? [...visiblePolicyTypes.items] : [];

    if (activeFund != null) {
        const activeVersion:any = activeFund.activeVersion;

        if(activeVersion != null){
            visiblePolicyTypeItems = visiblePolicyTypeItems.filter((item) => 
                { return activeVersion.ruleSetId === item.ruleSetId }
            )
        }
    }

    return (
        <Form className="node-settings-form" onSubmit={handleSubmit}>
            <Modal.Body>
                <Row>
                    <Col sm={3} className="menu">
                        <Nav variant="pills" activeKey={activeView} onSelect={changeView}>
                            <Nav.Item>
                                <Nav.Link eventKey={VIEW_KEYS.RULES}>{i18n('visiblePolicy.rules')}</Nav.Link>
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
                                            <CheckboxGroup 
                                                name="records"
                                                items={visiblePolicyTypeItems}
                                                disabled={rulesValue !== VIEW_POLICY_STATE.NODE}
                                                />
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
                                            <CheckboxGroup 
                                                name="nodeExtensions"
                                                items={availableExtensions}
                                                />
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

const selector = formValueSelector(FORM);

const mapStateToProps = (state: AppState) => {
    const rulesValue = selector(state, "rules") as VIEW_POLICY_STATE;
    return {
        visiblePolicy: state.arrRegion.visiblePolicy,
        visiblePolicyTypes: state.refTables.visiblePolicyTypes,
        rulesValue,
        arrRegion: state.arrRegion,
    };
}

export const ConnectedForm = connect(mapStateToProps)(NodeSettingsForm);

export const ReduxForm = reduxForm<NodeSettingsFormFields, OwnProps>({ form: FORM })(ConnectedForm);

export default ReduxForm;
