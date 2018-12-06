import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Autocomplete, AbstractReactComponent, i18n, Icon, FormInput, NoFocusButton} from 'components/shared';
import {Modal, Button, Form} from 'react-bootstrap';
import {indexById} from 'stores/app/utils.jsx'
import {decorateFormField, submitReduxFormWithProp} from 'components/form/FormUtils.jsx'
import {getRegistryRecordTypesIfNeeded} from 'actions/registry/registryRecordTypes.jsx'
import {WebApi} from 'actions/index.jsx';
import FragmentItemForm from "../accesspoint/FragmentItemForm";
import {fragmentItemFormActions} from "../accesspoint/FragmentItemFormActions";
import {modalDialogHide, modalDialogShow} from "../../actions/global/modalDialog";
import AddDescItemTypeForm from "../arr/nodeForm/AddDescItemTypeForm";

/**
 * Formulář přidání nového rejstříkového hesla
 * <AddRegistryForm onSubmit={this.handleCallAddRegistry} />
 */
class FragmentFormModal extends AbstractReactComponent {

    static PropTypes = {
        fragmentId: React.PropTypes.number.isRequired,
        fragmentItemForm: React.PropTypes.object.isRequired,
    };

    add = () => {
        const {fragmentItemForm} = this.props;


        const formData = fragmentItemForm.formData;
        const itemTypes = [];
        const strictMode = true;

        let infoTypesMap = new Map(fragmentItemForm.infoTypesMap);

        formData.itemTypes.forEach(descItemType => {
            infoTypesMap.delete(descItemType.id);
        });

        fragmentItemForm.refTypesMap.forEach(refType => {
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
                id: "DEFAULT",
                name: i18n("subNodeForm.descItemGroup.default"),
                children: itemTypes
            }
        ];

        const submit = (data) => {
            this.props.dispatch(fragmentItemFormActions.fundSubNodeFormDescItemTypeAdd(data.descItemTypeId.id));
        };

        // Modální dialog
        this.props.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>));
    };

    onClose = () => {
        const {onClose, onSubmit} = this.props;
        onSubmit && onSubmit();
        onClose && onClose();
    };

    render() {
        const {fragmentId} = this.props;

        return (
            <div>
                <Modal.Body>
                    <NoFocusButton onClick={this.add}><Icon glyph="fa-plus-circle"/>{i18n('subNodeForm.section.item')}</NoFocusButton>
                    <FragmentItemForm parent={{id: fragmentId}} />
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={this.onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect((state) => {
    return {
        fragmentItemForm: state.ap.fragmentItemForm
    }
})(FragmentFormModal)
