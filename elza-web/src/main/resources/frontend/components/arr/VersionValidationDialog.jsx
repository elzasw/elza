/**
 * Dialog s validací verze
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Icon, VersionValidationState} from 'components';
import {Button, Modal} from 'react-bootstrap';
import {fundSelectSubNode} from 'actions/arr/nodes';
import {versionValidate} from 'actions/arr/versionValidation';
import {createFundRoot} from 'components/arr/ArrUtils';

var VersionValidationDialog = class VersionValidationDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleSelectNode');
    }

    componentDidMount() {
        this.requestData((this.props.store.count > 0 && this.props.store.errors.length === 0) || this.props.store.isErrorListDirty, this.props.store.isFetching, this.props.versionId)
    }

    componentWillReceiveProps(nextProps) {
        this.requestData((nextProps.store.count > 0 && nextProps.store.errors.length === 0) || nextProps.store.isErrorListDirty, nextProps.store.isFetching, nextProps.versionId);
    }

    requestData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, true))
    }

    handleSelectNode(id, parent) {
        this.dispatch(fundSelectSubNode(id, parent ? parent : createFundRoot(this.props.fund)));
        this.props.onClose();
    }

    render() {
        const {onClose} = this.props;
        return (
            <div>
                <Modal.Body>
                    {!this.props.store.isFetching && this.props.store.errors.length > 0 && this.props.store.errors.map((item) => (
                        <div>
                            <a onClick={() => (this.handleSelectNode(item.nodeId, item.parent))}>JP-{item.nodeId}</a>: {item.description}
                        </div>
                    ))}
                    <VersionValidationState
                        count={this.props.store.count}
                        errExist={this.props.store.errors.length > 0}
                        isFetching={this.props.store.isFetching}/>
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
    store: state.arrRegion.funds[state.arrRegion.activeIndex].versionValidation,
    versionId: state.arrRegion.funds[state.arrRegion.activeIndex].versionId,
    fund: state.arrRegion.funds[state.arrRegion.activeIndex]
}))(VersionValidationDialog);



