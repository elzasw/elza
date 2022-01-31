import PropTypes from 'prop-types';
import React from 'react';
import {DecoratedFormProps, Field, FormErrors, formValueSelector, InjectedFormProps, reduxForm} from 'redux-form';
import {Autocomplete, i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {indexByProperty} from 'stores/app/utils';
import Scope from '../shared/scope/Scope';
import FormInputField from '../../components/shared/form/FormInputField';
import {connect} from 'react-redux';
import FF from '../shared/form/FF';
import {AppState} from "typings/store";
import {RevStateApproval, RevStateApprovalCaption} from "../../api/RevStateApproval";

const stateToOption = (item: RevStateApproval) => ({
    id: item,
    name: RevStateApprovalCaption(item),
});

type OwnProps = {
    accessPointId: number;
    versionId?: number;
    hideType?: boolean;
    onClose?: Function;
    states: string[];
};

type RevStateChangeVO = {
    state: RevStateApproval;
    typeId: number;
    scopeId: number;
};

type ConnectedProps = ReturnType<typeof mapStateToProps>;
type Props = OwnProps & ConnectedProps & InjectedFormProps<RevStateChangeVO, OwnProps, FormErrors<RevStateChangeVO>>;

class RevStateChangeForm extends React.Component<Props> {
    static propTypes = {
        accessPointId: PropTypes.number.isRequired,
        versionId: PropTypes.number,
        hideType: PropTypes.bool,
    };

    static defaultProps = {
        hideType: false,
    };

    getStateWithAll() {
        if (this.props.states) {
            return Object.values(this.props.states).map(stateToOption);
        } else {
            return [];
        }
    }

    componentDidMount() {
        let data = [
            RevStateApproval.ACTIVE,
            RevStateApproval.TO_AMEND,
            RevStateApproval.TO_APPROVE,
        ];
        this.props.change('states', data);
        if (!this.props.scopeId) {
            const {
                refTables: {scopesData},
                versionId,
            } = this.props;

            let index = scopesData.scopes ? indexByProperty(scopesData.scopes, versionId, "versionId") : false;
            if (index && scopesData.scopes[index].scopes && scopesData.scopes[index].scopes[0].id) {
                this.props.change('scopeId', scopesData.scopes[index].scopes[0].id);
            }
        }
    }

    render() {
        const {
            handleSubmit,
            onClose,
            hideType,
            versionId,
            refTables: {apTypes},
            submitting,
        } = this.props;

        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
                    <FF
                        field={Scope}
                        disabled={submitting}
                        versionId={versionId}
                        label={i18n('ap.state.title.scope')}
                        name={'scopeId'}
                    />
                    {!hideType && (
                        <FF
                            field={Autocomplete}
                            label={i18n('ap.state.title.type')}
                            items={apTypes.items ? apTypes.items : []}
                            tree
                            alwaysExpanded
                            allowSelectItem={item => item.addRecord}
                            name={'typeId'}
                            useIdAsValue
                            disabled={submitting}
                        />
                    )}
                    <Field
                        component={FormInputField}
                        type="autocomplete"
                        disabled={submitting}
                        useIdAsValue
                        required
                        label={i18n('ap.state.title.state')}
                        items={this.getStateWithAll()}
                        name={'state'}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button type="submit" variant="outline-secondary" disabled={submitting}>
                        {i18n('global.action.store')}
                    </Button>
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </Form>
        );
    }
}

const mapStateToProps = (state:AppState) => {
    const selector = formValueSelector('revStateChangeForm');

    return {
        scopeId: selector(state, 'scopeId'),
        typeId: selector(state, 'typeId'),
        state: selector(state, 'state'),
        refTables: state.refTables,
        userDetail: state.userDetail as any,
        states: selector(state, 'states'),
    };
};

const form = reduxForm<RevStateChangeVO, OwnProps, FormErrors<RevStateChangeVO>>({
    form: 'revStateChangeForm',
    validate(
        values: RevStateChangeVO,
        props: DecoratedFormProps<RevStateChangeVO, Props, FormErrors<RevStateChangeVO>>,
    ): FormErrors<RevStateChangeVO, FormErrors<RevStateChangeVO>> {
        const errors: FormErrors<RevStateChangeVO, FormErrors<RevStateChangeVO>> = {};

        if (!values.state) {
            errors.state = i18n('global.validation.required');
        }

        return errors;
    },
})(RevStateChangeForm as any);

export default connect(mapStateToProps)(form);
