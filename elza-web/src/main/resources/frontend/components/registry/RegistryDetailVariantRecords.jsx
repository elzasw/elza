import React from 'react';
import ReactDOM from 'react-dom';

import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    i18n,
    Icon,
    NoFocusButton,
    Utils
} from 'components/shared';
import {
    registryVariantAddRow,
    registryVariantCreate,
    registryVariantUpdate,
    registryVariantDelete,
    registryVariantInternalDelete
} from 'actions/registry/registry.jsx'
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {PropTypes} from 'prop-types';
import {setFocus} from 'actions/global/focus.jsx'
import RegistryLabel from "./RegistryLabel";
import defaultKeymap from './RegistryDetailVariantRecordsKeymap.jsx';
import {FOCUS_KEYS} from "../../constants.tsx";

/**
 * @deprecated
 */
class RegistryDetailVariantRecords extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    static PropTypes = {
        value: React.PropTypes.array.isRequired,
        regRecordId: React.PropTypes.number.isRequired
    };

    componentDidMount() {
        this.trySetFocus();
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    trySetFocus(props = this.props) {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.REGISTRY, 2, 'variantRecords')) {   // focus na konkrétní variantní rejstříkové heslo
                this.setState({}, () => {
                    this.refs['variant-' + focus.item.index].focus();
                    focusWasSet()
                })
            }
        }
    }

    handleAdd = () => {
        // Index Musíme zjistit předem, protože po dispatch přidání záznamu se store změní
        const newIndex = this.props.value.length;

        // Přidání nového proádného záznamu
        this.dispatch(registryVariantAddRow());

        // Nastavení focus
        this.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 2, 'variantRecords', {index: newIndex}))
    };


    handleCreateCall = (item, element) => {
        if (!element.target.value) {
            return false;
        }
        const {regRecordId} = this.props;
        const data = {record: element.target.value, regRecordId};
        this.dispatch(registryVariantCreate(data, item.variantRecordInternalId));
    };

    handleBlur = (item, element) => {
        if (!element.target.value) {
            return false;
        }
        const {regRecordId} = this.props;

        this.dispatch(registryVariantUpdate({
            id: item.id,
            regRecordId,
            record: element.target.value,
            version: item.version
        }));
    };

    handleDelete(item, index) {
        if(confirm(i18n('registry.deleteRegistryQuestion'))) {
            // Zjištění nového indexu focusu po smazání - zjisšťujeme zde, abychom měli aktuální stav store
            const {value} = this.props;
            let setFocusFunc;
            if (index + 1 < value.length) {    // má položku za, nový index bude aktuální
                setFocusFunc = () => setFocus(FOCUS_KEYS.REGISTRY, 2, 'variantRecords', {index: index})
            } else if (index > 0) { // má položku před
                setFocusFunc = () => setFocus(FOCUS_KEYS.REGISTRY, 2, 'variantRecords', {index: index - 1})
            } else {    // byla smazána poslední položka, focus dostane formulář
                setFocusFunc = () => setFocus(FOCUS_KEYS.REGISTRY, 2)
            }

            // Smazání
            if (item.id) {
                this.dispatch(registryVariantDelete(item.id))
            } else {
                this.dispatch(registryVariantInternalDelete(item.variantRecordInternalId))
            }

            // Nastavení focus
            this.dispatch(setFocusFunc())
        }
    }

    handleShortcuts = (item, index, action) => {
        console.log("#handleShortcuts", '[' + action + ']', this, item, index);
        const {disabled} = this.props;
        switch (action) {
            case 'deleteRegistryVariant':
                if (!disabled) {
                    this.handleDelete(item, index)
                }
                break
        }
    };

    renderItem = (item, index) => {
        const {disabled} = this.props;
        let variantKey;
        let onBlur = this.handleBlur.bind(this, item);

        if (!item.id) {
            variantKey = 'internalId' + item.variantRecordInternalId;
            onBlur = this.handleCreateCall.bind(this, item);
        } else {
            variantKey = item.id;
        }

        return <Shortcuts key={variantKey} name='VariantRecord' handler={this.handleShortcuts.bind(this, item, index)} alwaysFireHandler>
            <RegistryLabel
                ref={'variant-' + index}
                value={item.record}
                item={item}
                disabled={disabled}
                onBlur={onBlur}
                onEnter={onBlur}
                onDelete={this.handleDelete.bind(this, item, index)}
            />
        </Shortcuts>
    };

    render() {
        const {value, disabled} = this.props;
        return <div>
            {value.map(this.renderItem)}
            <NoFocusButton disabled={disabled} className="registry-variant-add" onClick={this.handleAdd}><Icon glyph='fa-plus' /></NoFocusButton>
        </div>
    }
}

export default connect((state) => {
    const {focus} = state;
    return {
        focus,
    }
})(RegistryDetailVariantRecords);
