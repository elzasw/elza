import PropTypes from 'prop-types';
import * as React from 'react';
import {Col, Form, Row} from 'react-bootstrap';
import {Button} from '../ui';
import {AbstractReactComponent, Icon} from 'components/shared';
import FormInput from 'components/shared/form/FormInput';
import i18n from '../i18n';
import ListBox from '../shared/listbox/ListBox';
import {reduxForm} from 'redux-form';
import {WebApi} from '../../actions';
import storeFromArea from '../../shared/utils/storeFromArea';
import HorizontalLoader from '../shared/loading/HorizontalLoader';
import ScopeField from '../admin/ScopeField';
import Loading from '../shared/loading/Loading';
import * as scopeActions from '../../actions/scopes/scopes';

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
            this.props.resetForm();
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
        // WebApi.getAllLanguages().then(json => {
        //     this.setState({
        //         languages: json
        //     });
        // });
        // this.refetchScopes();
    }

    fetchData = props => {
        props.dispatch(scopeActions.scopesListFetchIfNeeded());
        props.dispatch(scopeActions.languagesListFetchIfNeeded());
    };

    // refetchScopes() {
    //     WebApi.getAllScopes().then(data => {
    //         this.setState({scopesList: data});
    //     });
    // }

    connectScope = target => value => {
        if (!value || target.filter(i => i.id.value === value.id).length > 0) {
            return;
        }
        WebApi.connectScope(this.props.id, value.id).then(() => {
            target.addField(value);
        });
    };

    disconnectScope = (target, index) => {
        WebApi.disconnectScope(this.props.id, target[index].id.value).then(() => {
            target.removeField(index);
        });
    };

    renderConnectedScope = target => ({item, index}) => {
        return (
            <div>
                {item.name.value}{' '}
                <Button
                    variant="action"
                    bsSize="xs"
                    className="pull-right"
                    onClick={this.disconnectScope.bind(this, target, index)}
                >
                    <Icon glyph="fa-trash" />
                </Button>
            </div>
        );
    };

    handleFieldChange = e => {
        this.props.fields[e.target.name].onChange(e.target.value);
        this.saveData(e.target.name, e.target.value);
    };

    saveData = (changedFieldName, changedFieldValue) => {
        const {connectedScopes, ...data} = this.props.values;
        data[changedFieldName] = changedFieldValue;
        const errors = ScopeListForm.validate(data, this.props);
        const {id} = this.props;
        if (Object.keys(errors).length === 0 && id) {
            return WebApi.updateScope(id, data).then(() => {
                WebApi.getScopeWithConnected(id).then(data => {
                    this.props.onSave(data);
                    return {};
                });
            });
        }
    };

    render() {
        const {
            fields: {name, code, language, connectedScopes},
            id,
            scopeList,
            languageList,
        } = this.props;

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
                        <FormInput
                            type="text"
                            {...code}
                            {...customProps}
                            label={i18n('accesspoint.scope.code')}
                            onChange={this.handleFieldChange}
                        />
                    </Col>
                    <Col xs={6}>
                        <FormInput
                            as="select"
                            {...language}
                            {...customProps}
                            label={i18n('accesspoint.scope.language')}
                            onChange={this.handleFieldChange}
                        >
                            <option key={0} value={null} />
                            {languagesOptions}
                        </FormInput>
                    </Col>
                </Row>
                <FormInput
                    type="text"
                    {...name}
                    {...customProps}
                    label={i18n('accesspoint.scope.name')}
                    onChange={this.handleFieldChange}
                />
                <label>{i18n('accesspoint.scope.relatedScopes')}</label>
                {scopeList.fetched ? (
                    <ScopeField
                        scopes={connectableScopes}
                        onChange={this.connectScope(connectedScopes)}
                        {...customProps}
                        value={null}
                    />
                ) : (
                    <Loading />
                )}
                <ListBox items={connectedScopes} renderItemContent={this.renderConnectedScope(connectedScopes)} />
            </Form>
        );
    }
}

export default reduxForm(
    {
        form: 'scopeList',
        fields: ScopeListForm.fields,
        initialValues: ScopeListForm.initialValues,
        validate: ScopeListForm.validate,
        asyncBlurFields: ScopeListForm.fields,
        asyncValidate: (values, dispatch, props) => {
            const errors = ScopeListForm.validate(values, props);
            if (Object.keys(errors).length > 0) {
                return Promise.resolve(errors);
            }
            return Promise.resolve({});
        },
    },
    state => {
        return {
            languageList: storeFromArea(state, scopeActions.AREA_LANGUAGE_LIST),
            scopeList: storeFromArea(state, scopeActions.AREA_SCOPE_LIST),
        };
    },
)(ScopeListForm);
