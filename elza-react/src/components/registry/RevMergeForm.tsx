import PropTypes from 'prop-types';
import React from 'react';
import {DecoratedFormProps, Field, FormErrors, formValueSelector, InjectedFormProps, reduxForm} from 'redux-form';
import {i18n} from 'components/shared';
import {Form, Modal} from 'react-bootstrap';
import {Button} from '../ui';
import FormInputField from '../../components/shared/form/FormInputField';
import {connect} from 'react-redux';
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

type RevMergeStateVO = {
    state: StateApproval;
};

type ConnectedProps = ReturnType<typeof mapStateToProps>;
type Props = OwnProps & ConnectedProps & InjectedFormProps<RevMergeStateVO, OwnProps, FormErrors<RevMergeStateVO>>;

class RevMergeForm extends React.Component<Props> {
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
        WebApi.getStateApproval(this.props.accessPointId).then(data => {
            this.props.change('states', data);
        });
    }

    render() {
        const {
            handleSubmit,
            onClose,
            submitting,
        } = this.props;

        return (
            <Form onSubmit={handleSubmit}>
                <Modal.Body>
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
    const selector = formValueSelector('revMergeForm');

    return {
        state: selector(state, 'state'),
        refTables: state.refTables,
        userDetail: state.userDetail as any,
        states: selector(state, 'states'),
    };
};

const form = reduxForm<RevMergeStateVO, OwnProps, FormErrors<RevMergeStateVO>>({
    form: 'revMergeForm',
    validate(
        values: RevMergeStateVO,
        props: DecoratedFormProps<RevMergeStateVO, Props, FormErrors<RevMergeStateVO>>,
    ): FormErrors<RevMergeStateVO, FormErrors<RevMergeStateVO>> {
        const errors: FormErrors<RevMergeStateVO, FormErrors<RevMergeStateVO>> = {};

        if (!values.state) {
            errors.state = i18n('global.validation.required');
        }

        return errors;
    },
})(RevMergeForm as any);

export default connect(mapStateToProps)(form);
