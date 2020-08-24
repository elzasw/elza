import PropTypes from 'prop-types';
import * as React from 'react';
import {connect} from 'react-redux';
import {Col, Form, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent, Icon} from 'components/shared';
import i18n from '../i18n';
import ListBox from '../shared/listbox/ListBox';
import {Field, FieldArray, reduxForm} from 'redux-form';
import {WebApi} from '../../actions';
import storeFromArea from '../../shared/utils/storeFromArea';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import ScopeField from '../admin/ScopeField';
import Loading from '../shared/loading/Loading';
import * as scopeActions from '../../actions/scopes/scopes';
import FormInputField from '../shared/form/FormInputField';

class ScopeListForm extends AbstractReactComponent {
    static propTypes = {
        onCreate: PropTypes.func.isRequired,
        onSave: PropTypes.func.isRequired,
        id: PropTypes.number,
    };

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (data[name] == null || !data[name]) {
                errors[name] = i18n('global.validation.required');
            }
            return errors;
        }, {});

    static validate = (values, props) => {
        return ScopeListForm.requireFields('name', 'code')(values);
    };

    static fields = [
        'id',
        'name',
        'code',
        'language',
        'connectedScopes[].id',
        'connectedScopes[].name',
        'connectedScopes[].code',
        'connectedScopes[].language',
    ];

    static initialValues = {connectedScopes: []};

    state = {scopesList: []};

    UNSAFE_componentWillReceiveProps(nextProps, nextContext) {
        this.fetchData(nextProps);
    }

    componentDidUpdate(prevProps, prevState, prevContext) {
        if (prevProps.id && !this.props.id) {
            this.props.reset();
        } else if (
            this.props.fields &&
            prevProps.fields &&
            this.props.id &&
            prevProps.id &&
            prevProps.id === this.props.id &&
            this.props.fields.connectedScopes.length !== prevProps.fields.connectedScopes.length
        ) {
            this.props.asyncValidate();
        }
    }

    componentDidMount() {
        this.fetchData(this.props);
    }

    fetchData = props => {
        props.dispatch(scopeActions.scopesListFetchIfNeeded());
        props.dispatch(scopeActions.languagesListFetchIfNeeded());
    };

    connectScope = target => value => {
        const all = target.getAll() || [];
        if (!value || all.filter(i => i.id === value.id).length > 0) {
            return;
        }
        WebApi.connectScope(this.props.id, value.id).then(() => {
            target.push(value);
        });
    };

    disconnectScope = (target, index) => {
        WebApi.disconnectScope(this.props.id, target.get(index).id).then(() => {
            target.remove(index);
        });
    };

    renderConnectedScope = target => ({item, index}) => {
        return (
            <div className={'d-flex align-items-center pl-1'}>
                {item.name}{' '}
                <Button
                    variant="action"
                    bsSize="xs"
                    className="ml-auto"
                    onClick={this.disconnectScope.bind(this, target, index)}
                >
                    <Icon glyph="fa-trash" />
                </Button>
            </div>
        );
    };

    render() {
        const {id, scopeList, languageList} = this.props;

        const customProps = {
            disabled: id == null,
        };

        if (!scopeList || !scopeList.fetched || !languageList.fetched) {
            return <HorizontalLoader />;
        }

        const languagesOptions = languageList.rows.map(i => (
            <option value={i.code} key={i.code}>
                {i.name}
            </option>
        ));
        const connectableScopes = scopeList.rows.filter(s => s.id !== id);
        return (
            <Form onSubmit={null}>
                <Row>
                    <Col xs={6}>
                        <Field
                            component={FormInputField}
                            type="text"
                            name={'code'}
                            {...customProps}
                            label={i18n('accesspoint.scope.code')}
                        />
                    </Col>
                    <Col xs={6}>
                        {/** TODO předělat na {@link LanguageCodeField.jsx} */}
                        <Field
                            component={FormInputField}
                            as="select"
                            name={'language'}
                            {...customProps}
                            label={i18n('accesspoint.scope.language')}
                        >
                            <option key={0} value={null} />
                            {languagesOptions}
                        </Field>
                    </Col>
                </Row>
                <Field
                    component={FormInputField}
                    type="text"
                    name={'name'}
                    {...customProps}
                    label={i18n('accesspoint.scope.name')}
                />
                <label>{i18n('accesspoint.scope.relatedScopes')}</label>
                <FieldArray
                    name={'connectedScopes'}
                    component={({fields, meta}) => {
                        const all = fields.getAll();
                        return (
                            <div>
                                {scopeList.fetched ? (
                                    <ScopeField
                                        scopes={connectableScopes}
                                        onChange={this.connectScope(fields)}
                                        {...customProps}
                                        value={null}
                                    />
                                ) : (
                                    <Loading />
                                )}
                                <ListBox items={all || []} renderItemContent={this.renderConnectedScope(fields)} />
                            </div>
                        );
                    }}
                />
            </Form>
        );
    }
}

const form = reduxForm({
    form: 'scopeList',
    initialValues: ScopeListForm.initialValues,
    validate: ScopeListForm.validate,
    asyncBlurFields: ScopeListForm.fields,
    asyncValidate: (values, dispatch, props) => {
        return WebApi.updateScope(values.id, values).then(() => {
            WebApi.getScopeWithConnected(values.id).then(data => {
                props.onSave(data);
            });
        });
    },
    enableReinitialize: true,
})(ScopeListForm);

export default connect(state => {
    return {
        languageList: storeFromArea(state, scopeActions.AREA_LANGUAGE_LIST),
        scopeList: storeFromArea(state, scopeActions.AREA_SCOPE_LIST),
    };
})(form);
