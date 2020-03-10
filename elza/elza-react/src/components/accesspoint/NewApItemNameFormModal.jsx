import PropTypes from 'prop-types';
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon, NoFocusButton} from 'components/shared';
import {Modal} from 'react-bootstrap';
import {Button} from '../ui';
import {WebApi} from 'actions/index.jsx';
import ApItemNameForm from '../accesspoint/ApItemNameForm';
import {accessPointFormActions} from '../accesspoint/AccessPointFormActions';
import {modalDialogShow} from '../../actions/global/modalDialog';
import AddDescItemTypeForm from '../arr/nodeForm/AddDescItemTypeForm';

/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */
class NewApItemNameFormModal extends AbstractReactComponent {

    static propTypes = {
        objectId: PropTypes.number.isRequired,
        accessPointId: PropTypes.number.isRequired,
        nameItemForm: PropTypes.object.isRequired,
    };

    add = () => {
        const {nameItemForm} = this.props;


        const formData = nameItemForm.formData;
        const itemTypes = [];
        const strictMode = true;

        let infoTypesMap = new Map(nameItemForm.infoTypesMap);

        formData.itemTypes.forEach(descItemType => {
            infoTypesMap.delete(descItemType.id);
        });

        nameItemForm.refTypesMap.forEach(refType => {
            if (infoTypesMap.has(refType.id)) {    // ještě ji na formuláři nemáme
                const infoType = infoTypesMap.get(refType.id);
                // v nestriktním modu přidáváme všechny jinak jen možné
                if (!strictMode || infoType.type !== 'IMPOSSIBLE') {
                    // nový item type na základě původního z refTables
                    itemTypes.push(refType);
                }
            }
        });

        const descItemTypes = [
            {
                groupItem: true,
                id: 'DEFAULT',
                name: i18n('subNodeForm.descItemGroup.default'),
                children: itemTypes,
            },
        ];

        const submit = (data) => {
            //this.props.dispatch(modalDialogHide());
            this.props.dispatch(accessPointFormActions.fundSubNodeFormDescItemTypeAdd(data.descItemTypeId.id));
        };

        // Modální dialog
        this.props.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), <AddDescItemTypeForm
            descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>));
    };

    onStore = () => {
        const {onClose, objectId, accessPointId, onSubmit} = this.props;
        WebApi.confirmAccessPointStructuredName(accessPointId, objectId).then(() => {
            onSubmit && onSubmit();
            onClose && onClose();
        });
    };

    onClose = () => {
        const {onClose, objectId, accessPointId} = this.props;
        WebApi.deleteAccessPointName(accessPointId, objectId).then(() => {
            onClose && onClose();
        });
    };

    render() {
        const {objectId, accessPointId} = this.props;

        return (
            <div>
                <Modal.Body>
                    <NoFocusButton onClick={this.add}><Icon glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}
                    </NoFocusButton>
                    <ApItemNameForm parent={{id: objectId, accessPointId}}/>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="link" onClick={this.onStore}>{i18n('global.action.store')}</Button>
                    <Button variant="link" onClick={this.onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        );
    }
}

export default connect((state) => {
    return {
        nameItemForm: state.ap.nameItemForm,
    };
})(NewApItemNameFormModal);
