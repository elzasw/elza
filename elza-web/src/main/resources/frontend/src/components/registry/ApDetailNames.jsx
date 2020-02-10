import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {FormControl, Button} from 'react-bootstrap'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {i18n, AbstractReactComponent, Icon} from 'components/shared'
import {indexById, objectById} from 'stores/app/utils.jsx'
import {normalizeNameObject} from 'actions/party/party.jsx'
import ApNameForm from './ApNameForm.jsx'
import {WebApi as WebApi} from "../../actions/WebApi";
import './ApDetailNames.less'
import NewApItemNameFormModal from "../accesspoint/NewApItemNameFormModal";
import UpdateApItemNameFormModal from "../accesspoint/UpdateApItemNameFormModal";

class ApDetailNames extends AbstractReactComponent {

    static PropTypes = {
        canEdit: React.PropTypes.bool.isRequired,
        accessPoint: React.PropTypes.object.isRequired,
        type: React.PropTypes.object.isRequired,
        refreshParty: React.PropTypes.func.isRequired,
        renderError: React.PropTypes.func.isRequired,
    };

    getName = (name) => {
        return name.fullName;
    };

    nameAdd = (data) => {
        const {accessPoint} = this.props;
        WebApi.createAccessPointName(accessPoint.id, data).then(() => {
            this.props.refreshParty();
            this.props.dispatch(modalDialogHide());
        });
    };

    nameUpdate = (name, data) => {
        const {accessPoint} = this.props;
        WebApi.updateAccessPointName(accessPoint.id, {...name, ...data}).then(() => {
            this.props.refreshParty();
            this.props.dispatch(modalDialogHide());
        });
    };

    nameDelete = (id) => {
        const {accessPoint} = this.props;
        WebApi.deleteAccessPointName(accessPoint.id, id).then(() => {
            this.props.refreshParty();
        })
    };

    nameSetPreffered = (id) => {
        const {accessPoint} = this.props;
        WebApi.setPreferredAccessPointName(accessPoint.id, id).then(() => {
            this.props.refreshParty();
        })
    };

    handleNameAdd = () => {
        const {type} = this.props;
        if (type.ruleSystemId != null) {
            const accessPointId = this.props.accessPoint.id;
            WebApi.createAccessPointStructuredName(accessPointId).then(data => {
                this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <NewApItemNameFormModal objectId={data.objectId} accessPointId={accessPointId} onSubmit={this.props.refreshParty} />, "dialog-lg"));
            })
        } else {
            this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <ApNameForm onSubmit={this.nameAdd} />, "dialog-lg"));
        }
    };

    handleNameUpdate = (name) => {
        const {type} = this.props;
        if (type.ruleSystemId != null) {
            this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <UpdateApItemNameFormModal objectId={name.objectId} accessPointId={this.props.accessPoint.id} onSubmit={this.props.refreshParty} />, "dialog-lg"));
        } else {
            this.props.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.update'), <ApNameForm initialValues={name} onSubmit={this.nameUpdate.bind(this, name)}/>, "dialog-lg"));
        }
    };

    handleDelete = (id) => {
        if (confirm(i18n('accesspoint.detail.name.delete'))) {
            this.nameDelete(id);
        }
    };

    handleSelectPreferred = (id) => {
        if (confirm(i18n('accesspoint.detail.name.setPreferredNameAlert'))) {
            this.nameSetPreffered(id);
        }
    };

    render() {
        const {accessPoint, canEdit, renderError} = this.props;

        return <div className="accesspoint-detail-names">
            <div>
                <label className="group-label">{i18n("accesspoint.detail.formNames")}</label>
                {canEdit && <Button bsStyle="action" onClick={this.handleNameAdd}><Icon glyph="fa-plus" /></Button>}
            </div>
            <div className="name-group">
                {accessPoint.names.map((name, index) =>
                    <div key={name.id} className={name.preferredName ? "preffered value-group" : "value-group"}>
                        <div className="value">{this.getName(name)}</div>
                        <div className="actions">
                            {renderError(name)}
                            {canEdit && <Button bsStyle="action" onClick={() => this.handleNameUpdate(name)}><Icon glyph="fa-pencil" /></Button>}
                            {canEdit && !name.preferredName && <span>
                                <Button className="delete" bsStyle="action" onClick={() => this.handleDelete(name.objectId)}><Icon glyph="fa-trash" /></Button>
                                <Button bsStyle="action" onClick={() => this.handleSelectPreferred(name.objectId)}><Icon glyph="fa-star" /></Button>
                            </span>}
                        </div>
                    </div>)}
            </div>
        </div>
    }
}

export default connect()(ApDetailNames);
