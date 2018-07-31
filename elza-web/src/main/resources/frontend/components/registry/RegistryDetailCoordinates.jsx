import React from 'react';
import ReactDOM from 'react-dom';

import {UrlFactory} from 'actions/index.jsx';
import {connect} from 'react-redux'
import {
    AbstractReactComponent,
    i18n,
    FormInput,
    Icon,
    NoFocusButton
} from 'components/shared';
import {
    registryCoordinatesUpload,
    registryCoordinatesAddRow,
    registryCoordinatesChange,
    registryCoordinatesCreate,
    registryCoordinatesDelete,
    registryCoordinatesUpdate,
    registryCoordinatesInternalDelete
} from 'actions/registry/registry.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import {downloadFile} from "../../actions/global/download";
import RegistryCoordinate from "./RegistryCoordinate";
import {FOCUS_KEYS} from "../../constants.tsx";




class RegistryDetailCoordinates extends AbstractReactComponent {

    static PropTypes = {
        value: React.PropTypes.array.isRequired,
        apRecordId: React.PropTypes.number.isRequired
    };

    handleUploadButtonClick = () => {
        ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).click();
    };

    handleUpload = () => {
        const fileList = ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).files;

        if (fileList.length != 1) {
            return;
        }
        const {apRecordId: apRecordId} = this.props;

        this.dispatch(registryCoordinatesUpload(fileList[0], apRecordId));

        ReactDOM.findDOMNode(this.refs.uploadInput.refs.input).value = null;
    };

    handleBlur = (item) => {
        if (item.hasError === undefined) {
            this.handleChange(item);
        }
        if (!item.hasError && item.oldValue && (item.oldValue.description !== item.description || item.oldValue.value !== item.value)) {
            this.dispatch(registryCoordinatesUpdate(item));
        }
    };


    handleAdd = () => {
        // Index Musíme zjistit předem, protože po dispatch přidání záznamu se store změní
        const newIndex = this.props.value.length;

        // Přidání nového proádného záznamu
        this.dispatch(registryCoordinatesAddRow());

        // Nastavení focus
        this.dispatch(setFocus(FOCUS_KEYS.REGISTRY, 2, 'coordinates', {index: newIndex}))
    };

    handleChange = (item) => {
        this.dispatch(registryCoordinatesChange(item))
    };

    handleCreate = (item) => {
        if (item.value == null) {
            return this.handleChange(item);
        }
        !item.hasError && this.dispatch(registryCoordinatesCreate(item, item.coordinatesInternalId));
    };

    handleDownload = (objectId) => {
        this.dispatch(downloadFile(UrlFactory.exportRegCoordinate(objectId)));
    };

    handleDelete = (item, index) => {
        if(confirm(i18n('registry.deleteCoordinatesQuestion'))) {
            // Zjištění nového indexu focusu po smazání - zjisšťujeme zde, abychom měli aktuální stav store
            const {value} = this.props;
            let setFocusFunc;
            if (index + 1 < value.length) {    // má položku za, nový index bude aktuální
                setFocusFunc = () => setFocus(FOCUS_KEYS.REGISTRY, 2, 'coordinates', {index: index})
            } else if (index > 0) { // má položku před
                setFocusFunc = () => setFocus(FOCUS_KEYS.REGISTRY, 2, 'coordinates', {index: index - 1})
            } else {    // byla smazána poslední položka, focus dostane formulář
                setFocusFunc = () => setFocus(FOCUS_KEYS.REGISTRY, 2)
            }

            // Smazání
            if (item.id) {
                this.dispatch(registryCoordinatesDelete(item.id))
            } else {
                this.dispatch(registryCoordinatesInternalDelete(item.coordinatesInternalId))
            }

            // Nastavení focus
            this.dispatch(setFocusFunc())
        }
    };

    renderItem = (item, index) => {
        const {disabled} = this.props;

        let variantKey;
        let blurField = this.handleBlur.bind(this, item);

        if (!item.id) {
            variantKey = 'internalId' + item.coordinatesInternalId;
            blurField = this.handleCreate.bind(this, item);
        } else {
            variantKey = item.id;
        }
        return <RegistryCoordinate
            ref={'coordinates-' + index}
            key={variantKey}
            item={item}
            disabled={disabled}
            onChange={this.handleChange}
            onBlur={blurField}
            onEnterKey={blurField}
            onDelete={this.handleDelete.bind(this, item, index)}
            onDownload={this.handleDownload.bind(this, item.id)}
        />
    };

    render() {
        const {value, disabled} = this.props;
        return <div>
            {value.map(this.renderItem)}
            <div className="registry-coordinate-add">
                <NoFocusButton disabled={disabled} onClick={this.handleAdd} title={i18n('registry.coordinates.addPoint')}  ><Icon glyph='fa-plus' /></NoFocusButton>
                <NoFocusButton onClick={this.handleUploadButtonClick} title={i18n('registry.coordinates.upload')} disabled={disabled}><Icon glyph="fa-upload" /></NoFocusButton>
                <FormInput className="hidden" accept="application/vnd.google-earth.kml+xml" type="file" ref='uploadInput' onChange={this.handleUpload} />
            </div>
        </div>
    }
}

export default connect((state) => {
    const {focus} = state;
    return {
        focus
    }
})(RegistryDetailCoordinates);
