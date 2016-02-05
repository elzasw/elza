/**
 * Dialog s validací verze
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading, Icon} from 'components';
import {Button, Input, Table, Modal} from 'react-bootstrap';
import {dateTimeToString} from 'components/Utils'
import {indexById} from 'stores/app/utils.jsx'
import {faSelectSubNodeInt} from 'actions/arr/nodes';
import {versionValidate} from 'actions/arr/versionValidation';

var VersionValidationDialog = class VersionValidationDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleSelectNode');
    }

    componentDidMount() {
        this.requestData(this.props.store.isDirty, this.props.store.isFetching, this.props.versionId)
    }

    componentWillReceiveProps(nextProps) {
        this.requestData(nextProps.store.isDirty, nextProps.store.isFetching, nextProps.versionId);
    }

    requestData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, true))
    }

    handleSelectNode(id, parent) {
        if (parent) {
            this.dispatch(faSelectSubNode(id, parent));
        } else {
            this.dispatch(id, createVirtualRoot(id)); /// TODO FA ROOT
        }
    }

    render() {
        const {onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    <div>
                        <div>
                            {
                                this.props.store.isFetching ?
                                    <span><Icon
                                        glyph="fa-refresh"/> {i18n('arr.fa.versionValidation.running')}</span> : (
                                    this.props.store.errors.length > 0 ?
                                        <span><Icon glyph="fa-check"/> {i18n('arr.fa.versionValidation.ok')}</span> :
                                        <span><Icon
                                            glyph="fa-exclamation-triangle"/> {i18n('arr.fa.versionValidation.err')}</span>
                                )

                            }
                        </div>
                        {!this.props.store.isFetching &&
                        <div>
                            {this.props.store.errors.length > 0 && this.props.store.errors.map((item) => (
                                <div><a
                                    onClick={() => (this.handleSelectNode(item.id, item.parentId))}>JP-{item.id}</a>: {item.description}
                                </div>
                            ))}
                            <div>
                                {i18n('arr.fa.versionValidation.count', this.props.store.errors.length, this.props.store.count)}
                            </div>
                        </div>
                        }
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.close')}</Button>
                </Modal.Footer>
            </div>
        )
    }
};


VersionValidationDialog.propTypes = {};

module.exports = connect((state) => ({
    store: state.arrRegion.fas[state.arrRegion.activeIndex].versionValidation,
    versionId: state.arrRegion.fas[state.arrRegion.activeIndex].versionId
}))(VersionValidationDialog);



