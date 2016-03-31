/**
 * Dialog s validací verze
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Icon, VersionValidationState} from 'components';
import {Button, Modal, Input} from 'react-bootstrap';
import {fundSelectSubNode} from 'actions/arr/nodes';
import {versionValidate} from 'actions/arr/versionValidation';
import {createFundRoot} from 'components/arr/ArrUtils';

var VersionValidationDialog = class VersionValidationDialog extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleSelectNode', 'handleChange');
    }

    componentDidMount() {
        this.requestData((this.props.store.count > 0 && this.props.store.errors.length === 0) || this.props.store.isErrorListDirty, this.props.store.isFetching, this.props.versionId)
    }

    componentWillReceiveProps(nextProps) {
        this.requestData((nextProps.store.count > 0 && nextProps.store.errors.length === 0) || nextProps.store.isErrorListDirty, nextProps.store.isFetching, nextProps.versionId);
    }

    requestData(isDirty, isFetching, versionId, showAll = false) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, true, showAll))
    }

    handleChange(event) {
        var showAll = this.refs.showAll.refs.input.checked;
        this.requestData(true, this.props.store.isFetching, this.props.versionId, showAll)
    }

    handleSelectNode(id, parent) {
        this.dispatch(fundSelectSubNode(this.props.versionId, id, parent ? parent : createFundRoot(this.props.fund)));
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
                    <Input type="checkbox" checked={this.props.store.showAll} ref="showAll" onChange={this.handleChange} label={i18n('arr.fund.versionValidation.showAll')} />
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



