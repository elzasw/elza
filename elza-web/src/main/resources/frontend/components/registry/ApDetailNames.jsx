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

class ApDetailNames extends AbstractReactComponent {

    static PropTypes = {
        canEdit: React.PropTypes.bool.isRequired,
        accessPoint: React.PropTypes.object.isRequired,
        refreshParty: React.PropTypes.func.isRequired,
    };

    getName = (name) => {
        return name.name + " " + name.complement;
    };

    nameAdd = (data) => {
        const {accessPoint} = this.props;
        WebApi.createAccessPointName(accessPoint.id, data).then(() => {
            this.props.refreshParty();
            this.dispatch(modalDialogHide());
        });
    };

    nameUpdate = (name, data) => {
        const {accessPoint} = this.props;
        WebApi.updateAccessPointName(accessPoint.id, {...name, ...data}).then(() => {
            this.props.refreshParty();
            this.dispatch(modalDialogHide());
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
        this.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.new'), <ApNameForm onSubmit={this.nameAdd} />, "dialog-lg"));
    };

    handleNameUpdate = (name) => {
        this.dispatch(modalDialogShow(this, i18n('accesspoint.detail.name.update'), <ApNameForm initialValues={name} onSubmit={this.nameUpdate.bind(this, name)} />, "dialog-lg"));
    };

    handleDelete = (id) => {
        if (confirm(i18n('accesspoint.detail.name.delete'))) {
            this.nameDelete(id);
        }
    };

    handleSelectPreferred = (id) => {
        if (confirm(i18n('accesspoint.detail.name.setPrefferedNameAlert'))) {
            this.nameSetPreffered(id);
        }
    };

    render() {
        const {accessPoint, canEdit} = this.props;

        return <div className="accesspoint-detail-names">
            <div>
                <label className="group-label">{i18n("accesspoint.detail.formNames")}</label>
                {canEdit && <Button bsStyle="action" onClick={this.handleNameAdd}><Icon glyph="fa-plus" /></Button>}
            </div>
            <div className="name-group">
                {accessPoint.variantRecords.map((name, index) =>
                    <div key={name.id} className={name.preferredName ? "preffered value-group" : "value-group"}>
                        <div className="value">{this.getName(name)}</div>
                        <div className="actions">
                            {canEdit && <Button bsStyle="action" onClick={() => this.handleNameUpdate(name)}><Icon glyph="fa-pencil" /></Button>}
                            {canEdit && !name.preferredName && <span>
                                <Button className="delete" bsStyle="action" onClick={() => this.handleDelete(name.id)}><Icon glyph="fa-trash" /></Button>
                                <Button bsStyle="action" onClick={() => this.handleSelectPreferred(name.id)}><Icon glyph="fa-star" /></Button>
                            </span>}
                        </div>
                    </div>)}
            </div>
        </div>
    }
}

export default connect()(ApDetailNames);
