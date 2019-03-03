import * as React from 'react';
import {Button, Row, Col, Form} from 'react-bootstrap';
import {AbstractReactComponent, Icon} from 'components/shared';
import UserField from "../admin/UserField";
import FormInput from "../shared/form/FormInput";
import i18n from "../i18n";
import ListBox from "../shared/listbox/ListBox";
import {reduxForm} from 'redux-form';
import {Dispatch} from "../../typings/globals";
import {WebApi} from "../../actions";

class IssueListForm extends AbstractReactComponent {

    static propTypes = {
        onCreate: React.PropTypes.func.isRequired,
        onSave: React.PropTypes.func.isRequired,
        id: React.PropTypes.number,
        fundId: React.PropTypes.number.isRequired,
    };

    static requireFields = (...names) => data =>
        names.reduce((errors, name) => {
            if (!data[name]) {
                errors[name] = i18n('global.validation.required')
            }
            return errors
        }, {});

    static validate = (values, props) => {
        return IssueListForm.requireFields("name", "open")(values);
    };

    static fields = [
        "name",
        "open",
        "rdUsers[].id",
        "rdUsers[].username",
        "rdUsers[].description",
        "wrUsers[].id",
        "wrUsers[].username",
        "wrUsers[].description"
    ];

    static initialValues = {open: true, rdUsers: [], wrUsers: []};

    componentDidUpdate(prevProps: Readonly<P>, prevState: Readonly<S>, prevContext: any): void {
        if (prevProps.id && !this.props.id) {
            this.props.resetForm();
        } else if (this.props.fields
            && prevProps.fields
            && this.props.id
            && prevProps.id
            && prevProps.id === this.props.id
            && this.props.fields.rdUsers.length !== prevProps.fields.rdUsers.length ||
                this.props.fields.wrUsers.length !== prevProps.fields.wrUsers.length) {
                this.props.asyncValidate()
        }
    }

    addUser = (target) => (value) => {
        if (!value || target.filter(i => i.id.value === value.id).length > 0) {
            return;
        }
        target.addField(value);
    };

    deleteUser = (target, index, e) => {
        target.removeField(index)
    };

    renderUser = (target) => ({item, index}) => {
        return <div>{item.username.value} <Button bsStyle="action" bsSize="xs" className="pull-right" onClick={this.deleteUser.bind(this, target, index)}><Icon glyph="fa-trash" /></Button></div>
    };

    render() {
        const {fields: {name, open, rdUsers, wrUsers}} = this.props;

        return <Form onSubmit={null}>
            <FormInput type="text" {...name} label={i18n("issueList.name")} />
            <FormInput componentClass="select" {...open} label={i18n("issueList.open")}>
                <option value={true}>{i18n("issueList.open.true")}</option>
                <option value={false}>{i18n("issueList.open.false")}</option>
            </FormInput>
            <label>{i18n("arr.issuesList.form.permission")}</label>
            <Row>
                <Col xs={6}>
                    <label>{i18n("arr.issuesList.form.permission.read")}</label>
                    <UserField onChange={this.addUser(rdUsers)} value={null} />
                    <ListBox items={rdUsers} renderItemContent={this.renderUser(rdUsers)}/>
                </Col>
                <Col xs={6}>
                    <label>{i18n("arr.issuesList.form.permission.write")}</label>
                    <UserField onChange={this.addUser(wrUsers)} value={null} />
                    <ListBox items={wrUsers} renderItemContent={this.renderUser(wrUsers)}/>
                </Col>
            </Row>
        </Form>
    }
}

export default reduxForm({
    form: "issueList",
    fields: IssueListForm.fields,
    initialValues: IssueListForm.initialValues,
    validate: IssueListForm.validate,
    asyncBlurFields: IssueListForm.fields,
    // Async validace zneužita k ukládání dat
    asyncValidate: (values: FormData, dispatch: Dispatch<any>, props: {fundId: number, id: number, onCreate: Function, onSave: Function, values: Object}) : Promise<any> => {
        const errors = IssueListForm.validate(values, props);
        if (Object.keys(errors).length > 0) {
            return Promise.resolve(errors);
        }
        const {id} = props;
        if (id) {
            return WebApi.updateIssueList(id, {...values, fundId: props.fundId, id}).then((data) => {
                props.onSave(data);
                return {} // No errors saved correctly
            })
        } else {
            return WebApi.addIssueList({...values, fundId: props.fundId}).then((data) => {
                props.onCreate(data);
                return {} // No errors saved correctly
            });
        }

    }
})(IssueListForm);
