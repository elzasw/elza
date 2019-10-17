import * as React from 'react';
import {Button, Row, Col, Form} from 'react-bootstrap';
import {AbstractReactComponent, Icon} from 'components/shared';
import FormInput from "../shared/form/FormInput";
import i18n from "../i18n";
import ListBox from "../shared/listbox/ListBox";
import {reduxForm} from 'redux-form';
import {Dispatch} from "../../typings/globals";
import {WebApi} from "../../actions";
import storeFromArea from "../../shared/utils/storeFromArea";
import HorizontalLoader from "../shared/loading/HorizontalLoader";
import ScopeField from "../admin/ScopeField";
import Loading from "../shared/loading/Loading";
import * as scopeActions from "../../actions/scopes/scopes";

class ScopeListForm extends AbstractReactComponent {

    static propTypes = {
        onCreate: React.PropTypes.func.isRequired,
        onSave: React.PropTypes.func.isRequired,
        id: React.PropTypes.number,
    };

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (data[name] == null || !data[name]) {
                errors[name] = i18n('global.validation.required')
            }
            return errors
        }, {});

    static validate = (values, props) => {
        return ScopeListForm.requireFields("name", "code")(values);
    };

    static fields = [
        "id",
        "name",
        "code",
        "language",
        "connectedScopes[].id",
        "connectedScopes[].name",
        "connectedScopes[].code",
        "connectedScopes[].language",
    ];

    static initialValues = {connectedScopes: []};

    state = {scopesList: []};

    componentWillReceiveProps(nextProps: Readonly<P>, nextContext: any): void {
        this.fetchData(nextProps);
    }

    componentDidUpdate(prevProps: Readonly<P>, prevState: Readonly<S>, prevContext: any): void {
        if (prevProps.id && !this.props.id) {
            this.props.resetForm();
        } else if (this.props.fields
            && prevProps.fields
            && this.props.id
            && prevProps.id
            && prevProps.id === this.props.id
            && this.props.fields.connectedScopes.length !== prevProps.fields.connectedScopes.length) {
                this.props.asyncValidate()
        }
    }

    componentDidMount(): void {
        this.fetchData(this.props);
        // WebApi.getAllLanguages().then(json => {
        //     this.setState({
        //         languages: json
        //     });
        // });
        // this.refetchScopes();
    }

    fetchData = (props) => {
        props.dispatch(scopeActions.scopesListFetchIfNeeded());
        props.dispatch(scopeActions.languagesListFetchIfNeeded());
    };

    // refetchScopes() {
    //     WebApi.getAllScopes().then(data => {
    //         this.setState({scopesList: data});
    //     });
    // }

    connectScope = (target) => (value) => {
        if (!value || target.filter(i => i.id.value === value.id).length > 0) {
            return;
        }
        WebApi.connectScope(this.props.id, value.id).then(() => {
            target.addField(value);
        });
    };

    disconnectScope = (target, index) => {
        WebApi.disconnectScope(this.props.id, target[index].id.value).then(() => {
            target.removeField(index)
        });
    };

    renderConnectedScope = (target) => ({item, index}) => {
        return <div>{item.name.value} <Button bsStyle="action" bsSize="xs" className="pull-right" onClick={this.disconnectScope.bind(this, target, index)}><Icon glyph="fa-trash" /></Button></div>
    };

    handleFieldChange = (e) => {
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
                    return {}
                });
            });
        }
    };

    render() {
        const {fields: {name, code, language, connectedScopes}, id, scopeList, languageList} = this.props;

        const customProps = {
            disabled: id == null
        };

        if (!scopeList.fetched || !languageList.fetched) {
            return <HorizontalLoader/>;
        }

        const languagesOptions = languageList.rows.map(i => <option value={i.code} key={i.code}>{i.name}</option>);
        const connectableScopes = scopeList.rows.filter(s => s.id !== id);
        return <Form onSubmit={null}>
            <Row>
                <Col xs={6}>
                    <FormInput type="text" {...code} {...customProps} label={i18n("accesspoint.scope.code")} onChange={this.handleFieldChange} />
                </Col>
                <Col xs={6}>
                    <FormInput componentClass="select" {...language} {...customProps} label={i18n("accesspoint.scope.language")} onChange={this.handleFieldChange} >
                        <option key={0} value={null} />
                        {languagesOptions}
                    </FormInput>
                </Col>
            </Row>
            <FormInput type="text" {...name} {...customProps} label={i18n("accesspoint.scope.name")} onChange={this.handleFieldChange} />
            <label>{i18n("accesspoint.scope.relatedScopes")}</label>
            {scopeList.fetched ?
                <ScopeField scopes={connectableScopes} onChange={this.connectScope(connectedScopes)} {...customProps} value={null} />
                : <Loading/>
            }
            <ListBox items={connectedScopes} renderItemContent={this.renderConnectedScope(connectedScopes)}/>
        </Form>
    }
}

export default reduxForm({
    form: "scopeList",
    fields: ScopeListForm.fields,
    initialValues: ScopeListForm.initialValues,
    validate: ScopeListForm.validate,
    asyncBlurFields: ScopeListForm.fields,
    asyncValidate: (values: FormData, dispatch: Dispatch<any>, props: {id: number, onCreate: Function, onSave: Function, values: Object}) : Promise<any> => {
        const errors = ScopeListForm.validate(values, props);
        if (Object.keys(errors).length > 0) {
            return Promise.resolve(errors);
        }
        return Promise.resolve({});
    }}, state => {
    return {
        languageList: storeFromArea(state, scopeActions.AREA_LANGUAGE_LIST),
        scopeList: storeFromArea(state, scopeActions.AREA_SCOPE_LIST),
    }}
)(ScopeListForm);