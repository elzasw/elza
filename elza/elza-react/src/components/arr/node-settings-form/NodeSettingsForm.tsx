import { CheckboxGroup } from 'components/arr/nodeForm/checkbox-group';
import { FormInputField} from 'components/shared';
import { FormattedMessage, defineMessages } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { nodeMessages } from 'components/arr/nodeMessages';

import { useState } from 'react';
import { Col, Form, Modal, Nav, Row } from 'react-bootstrap';
import { connect } from 'react-redux';
import { Field, formValueSelector, InjectedFormProps, reduxForm } from 'redux-form';
import { AppState } from "../../../typings/store";
import { Button } from '../../ui';
import './NodeSettingsForm.scss';
import { VIEW_KEYS, VIEW_POLICY_STATE } from "./static-data";

const messages = defineMessages({
    noActiveExtensions: {
        id: 'arr.node.settings.noActiveExtensions',
        defaultMessage: 'Nejsou aktivní žádná rozšíření',
    },
});

interface OwnProps {
    onClose: () => void;
}

export interface NodeSettingsFormFields {
    rules: VIEW_POLICY_STATE;
    records: Record<string | number, boolean>;
    nodeExtensions: Record<string | number, boolean>
}

const FORM = 'nodeSettingsForm';

export type Props = OwnProps & ReturnType<typeof mapStateToProps> & InjectedFormProps<NodeSettingsFormFields>

const NodeSettingsForm = ({
    rulesValue,
    handleSubmit,
    onClose,
    pristine,
    submitting,
    visiblePolicy,
    visiblePolicyTypes,
    arrRegion,
}: Props) => {
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
                                <Nav.Link eventKey={VIEW_KEYS.RULES}>{<FormattedMessage {...nodeMessages.visiblePolicyRules} />}</Nav.Link>
                            </Nav.Item>
                            <Nav.Item>
                                <Nav.Link eventKey={VIEW_KEYS.EXTENSIONS}>
                                    {<FormattedMessage {...nodeMessages.visiblePolicyExtensions} />}
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
                                        label={<FormattedMessage {...nodeMessages.visiblePolicyRulesParent} />}
                                        value={VIEW_POLICY_STATE.PARENT}
                                        />
                                    <Field
                                        name="rules"
                                        type="radio"
                                        component={FormInputField}
                                        label={<FormattedMessage {...nodeMessages.visiblePolicyRulesNode} />}
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
                                    <h4>{<FormattedMessage {...nodeMessages.visiblePolicyRulesParent} />}</h4>
                                    <div className="listbox-wrapper">
                                        <div className="listbox-container">
                                            {parentExtensions && parentExtensions.length > 0
                                            ? parentExtensions.map((i, index) => (
                                                <div key={index}>{i.name}</div>
                                            ))
                                            : <FormattedMessage {...messages.noActiveExtensions} />}
                                        </div>
                                    </div>
                                </Col>
                                <Col xs={12}>
                                    <h4>{<FormattedMessage {...nodeMessages.visiblePolicyRulesNode} />}</h4>
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
                    {<FormattedMessage {...nodeMessages.visiblePolicyActionSave} />}
                </Button>
                <Button variant="link" disabled={submitting} onClick={onClose}>
                    {<FormattedMessage {...globalMessages.cancel} />}
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

export const ReduxForm = connect(mapStateToProps)(reduxForm<NodeSettingsFormFields, OwnProps & ReturnType<typeof mapStateToProps>>({ form: FORM })(NodeSettingsForm));

export default ReduxForm;
