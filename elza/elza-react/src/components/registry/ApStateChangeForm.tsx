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
import * as perms from '../../actions/user/Permission';
import {StateApproval, StateApprovalCaption} from '../../api/StateApproval';
import {AppState} from "typings/store";
import {WebApi} from 'actions';

const stateToOption = (item: StateApproval) => ({
    id: item,
    name: StateApprovalCaption(item),
});

type OwnProps = {
    accessPointId: number;
    versionId?: number;
    hideType?: boolean;
    onClose?: Function;
    states: string[];
};

type ApStateChangeVO = {
    state: StateApproval;
    comment: string;
    typeId: number;
    scopeId: number;
};

type ConnectedProps = ReturnType<typeof mapStateToProps>;
type Props = OwnProps & ConnectedProps & InjectedFormProps<ApStateChangeVO, OwnProps, FormErrors<ApStateChangeVO>>;

class ApStateChangeForm extends React.Component<Props> {
    static propTypes = {
        accessPointId: PropTypes.number.isRequired,
        versionId: PropTypes.number,
        hideType: PropTypes.bool,
    };

    static defaultProps = {
        hideType: false,
    };

    getStateWithAll() {
        console.log(this.props.states);
        if (this.props.states) {
            return Object.values(this.props.states).map(stateToOption);
        } else {
            return [];
        }
    }

    componentDidMount() {
        WebApi.getStateApproval(this.props.accessPointId).then(data => {
            this.props.change('states', data);
        });
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
                    <Field
                        component={FormInputField}
                        disabled={submitting}
                        type="text"
                        label={i18n('ap.state.title.comment')}
                        name={'comment'}
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
    const selector = formValueSelector('apStateChangeForm');

    return {
        scopeId: selector(state, 'scopeId'),
        typeId: selector(state, 'typeId'),
        state: selector(state, 'state'),
        refTables: state.refTables,
        userDetail: state.userDetail as any,
        states: selector(state, 'states'),
    };
};

const form = reduxForm<ApStateChangeVO, OwnProps, FormErrors<ApStateChangeVO>>({
    form: 'apStateChangeForm',
    validate(
        values: ApStateChangeVO,
        props: DecoratedFormProps<ApStateChangeVO, Props, FormErrors<ApStateChangeVO>>,
    ): FormErrors<ApStateChangeVO, FormErrors<ApStateChangeVO>> {
        const errors: FormErrors<ApStateChangeVO, FormErrors<ApStateChangeVO>> = {};

        if (!values.state) {
            errors.state = i18n('global.validation.required');
        }

        if (
            props.initialValues.state !== StateApproval.APPROVED &&
            values.state === StateApproval.APPROVED &&
            !props.userDetail.hasOne(perms.AP_CONFIRM_ALL, {
                type: perms.AP_CONFIRM,
                id: parseInt(values.scopeId as any),
            })
        ) {
            errors.state = i18n('ap.state.state.insufficient.right');
        }

        return errors;
    },
})(ApStateChangeForm as any);

export default connect(mapStateToProps)(form);
