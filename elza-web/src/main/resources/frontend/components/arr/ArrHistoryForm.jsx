import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, FundNodesSelectForm, i18n, FormInput} from 'components/index.jsx';
import {Modal, Button, FormGroup, Form} from 'react-bootstrap';
import {decorateFormField, submitReduxForm} from 'components/form/FormUtils.jsx'
import {LazyListBox} from 'components/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {getScrollbarWidth, timeToString, dateToString} from 'components/Utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'

require("./ArrHistoryForm.less");

/**
 * Formulář zobrazení hostorie.
 */
class ArrHistoryForm extends AbstractReactComponent {
    static PropTypes = {};

    constructor(props) {
        super(props);

        this.state = {
            node: typeof props.node !== 'undefined' ? props.node : null,
            changeId: null,
            selectedItem: null,
            selectedIndex: null,
            activeIndex: null,
            showHistoryForNode: props.node ? true : false,  // pro node je true, globalni je false
        }
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    renderItemContent = (item) => {
        if (item === null) {
            return null;
        }

        const {selectedItem} = this.state;
        const canDelete = selectedItem && item.changeDate >= selectedItem.changeDate;

        return (
            <div className={`row-container ${item.revert ? " canRevert" : ""} ${canDelete ? " delete" : ""}`}>
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                <div className="col col3">{item.description}</div>
                <div className="col col4">{i18n(`arr.history.change.title.${item.type ? item.type : "unknown"}`)}</div>
                <div className="col col5">{item.username ? item.username : <i>System</i>}</div>
            </div>
        )
    }

    getItems = (fromIndex, toIndex) => {
        const {versionId} = this.props;
        const {changeId, node, showHistoryForNode} = this.state;

        const useNodeId = showHistoryForNode ? ( node ? node.id : null ) : null;
        return WebApi.findChanges(versionId, useNodeId, fromIndex, toIndex - fromIndex, changeId)
            .then(json => {
                console.log(json);
                if (json.totalCount > 0 && changeId === null) {    // pokud nemáme uložen první changeId, uložíme si ho do state
                    this.setState({
                        changeId: json.changes[0].changeId
                    });
                }

                const lbData = {
                    items: json.changes,
                    count: json.totalCount,
                    outdated: json.outdated
                }
                return lbData;
            })
    }

    handleSelect = (item, index) => {
        if (item.revert) {
            this.setState({
                selectedIndex: index,
                activeIndex: index,
                selectedItem: item,
            })
        } else {
            this.setState({
                activeIndex: index,
            })
        }
    }

    handleShowSelectedItem = () => {
        const {selectedIndex, activeIndex} = this.state;
        this.setState({
            activeIndex: selectedIndex
        }, () => { this.refs.listbox.ensureItemVisible(selectedIndex) })
    }

    renderSelectedItemInfo = () => {
        const {selectedItem} = this.state;

        const infoText = selectedItem ? `${dateToString(new Date(selectedItem.changeDate))};${timeToString(new Date(selectedItem.changeDate))};${selectedItem.description};${selectedItem.type};${selectedItem.userId}` : null;

        return (
            <FormInput className="selected-item-info-container" type="static" label={i18n('arr.history.title.deleteFrom')}>
                <input type="text" value={infoText} disabled />
                <Button disabled={!selectedItem} onClick={this.handleShowSelectedItem}>{i18n("arr.history.action.deleteFrom.show")}</Button>
            </FormInput>
        )
    }

    handleChooseNode = () => {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
            <FundNodesSelectForm
                multipleSelection={false}
                onSubmitForm={(id, node) => {
                    this.setState({
                        node,
                        selectedItem: null,
                        selectedIndex: null,
                        changeId: null,
                        activeIndex: null,
                    }, this.refreshRows);
                    this.dispatch(modalDialogHide());
                }}
            />))
    }

    onChangeRadio = (showHistoryForNode) => {
        this.setState({
            showHistoryForNode,
            selectedItem: null,
            selectedIndex: null,
            changeId: null,
            activeIndex: null,
        }, this.refreshRows);
    }

    refreshRows = () => {
        this.refs.listbox.reload();
    }

    handleDeleteChanges = () => {
        const {onDeleteChanges} = this.props;
        const {node, changeId, selectedIndex, selectedItem} = this.state;

        if (confirm(i18n('arr.history.deleteQuestion', selectedIndex + 1))) {
            onDeleteChanges(node ? node.id : null, changeId, selectedItem.changeId);
        }
    }

    render() {
        const {selectedItem, node, showHistoryForNode, selectedIndex, activeIndex} = this.state;
        const {onClose} = this.props;

        return (
            <div className="arr-history-form-container">
                <Modal.Body>
                    <FormGroup>
                        <FormInput type="radio" checked={!showHistoryForNode} onClick={() => this.onChangeRadio(false)} label={i18n('arr.history.title.globalChanges')}/>
                        <div className="selected-node-container">
                            <FormInput type="radio" checked={showHistoryForNode} onClick={() => this.onChangeRadio(true)} label={i18n('arr.history.title.nodeChanges')}/>
                            <FormInput className="selected-node-info-container" type="static" label={false}>
                                <input type="text" value={node ? node.name : ""} disabled />
                                <Button disabled={!showHistoryForNode} onClick={this.handleChooseNode}>{i18n("global.action.choose")}</Button>
                            </FormInput>
                        </div>
                    </FormGroup>
                    <div className="changes-listbox-container">
                        <div className="header-container">
                            <div className="col col1">{i18n("arr.history.title.change.date")}</div>
                            <div className="col col2">{i18n("arr.history.title.change.time")}</div>
                            <div className="col col3">{i18n("arr.history.title.change.description")}</div>
                            <div className="col col4">{i18n("arr.history.title.change.type")}</div>
                            <div className="col col5">{i18n("arr.history.title.change.user")}</div>
                            <div className="colScrollbar" style={{width: getScrollbarWidth()}}></div>
                        </div>
                        <LazyListBox
                            ref="listbox"
                            className="data-container"
                            selectedIndex={selectedIndex}
                            activeIndex={activeIndex}
                            getItems={this.getItems}
                            itemHeight={24} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                            renderItemContent={this.renderItemContent}
                            onSelect={this.handleSelect}
                        />
                    </div>
                    {this.renderSelectedItemInfo()}
                </Modal.Body>
                <Modal.Footer>
                    {selectedIndex !== null ? i18n("arr.history.title.changesForDelete", selectedIndex + 1) + " " : ""}
                    <Button disabled={selectedItem === null || (showHistoryForNode && !node)} type="submit" onClick={this.handleDeleteChanges}>{i18n('arr.history.action.deleteChanges')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect()(ArrHistoryForm);