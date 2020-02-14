/**
 * Formulář zobrazení hostorie.
 */
import React from 'react';
import {connect} from 'react-redux'
import {reduxForm} from 'redux-form';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button, FormGroup, Form} from 'react-bootstrap';
import {LazyListBox} from 'components/shared';
import {WebApi} from 'actions/index.jsx';
import {getScrollbarWidth, timeToString, dateToString} from 'components/Utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {dateTimeToLocalUTC, dateTimeToZonedUTC} from "components/Utils"
import FundNodesSelectForm from "./FundNodesSelectForm";

import './ArrHistoryForm.scss';

class ArrHistoryForm extends AbstractReactComponent {

    static propTypes = {};

    static defaultProps = {
        locked: false
    };

    constructor(props) {
        super(props);

        this.state = {
            node: typeof props.node !== 'undefined' ? props.node : null,
            goToDate: "",
            goToDateValue: null,
            changeId: null,
            selectedItem: null,
            selectedIndex: null,
            activeIndex: null,
            fetching: false,
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
        const typeText = this.getItemTypeText(item);
        const description = this.getItemDescription(item);

        return (
            <div className={`row-container ${item.revert ? " canRevert" : ""} ${canDelete ? " delete" : ""}`}>
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                <div className="col col3" title={description}>{description}</div>
                <div className="col col4" title={typeText}>{typeText}</div>
                <div className="col col5">{item.username ? item.username : <i>System</i>}</div>
            </div>
        )
    }

    getItemTypeText(item) {
        return i18n(`arr.history.change.title.${item.type || 'unknown'}`);
    }

    getItemDescription(item) {
        switch (item.type || '') {
            case 'BULK_ACTION':
            case 'ADD_NODES_OUTPUT':
            case 'REMOVE_NODES_OUTPUT':
            case 'CREATE_AS':
            case 'BATCH_CHANGE_DESC_ITEM':
            case 'BATCH_DELETE_DESC_ITEM':
            case 'IMPORT':
                return i18n('arr.history.change.description.' + item.type, String(item.nodeChanges));
        }

        if (item.label) {
            return item.label;
        }

        return this.getItemTypeText(item)
            + ', primaryNodeId: ' + (item.primaryNodeId || '?')
            + ', changeId: ' + item.changeId
            + ', changeDate: ' + dateToString(new Date(item.changeDate));
    }

    // Returns id of the selected node. Returns null when id doesnt exist or when reverting global history
    getNodeId = () => {
        const { node, showHistoryForNode } = this.state;
        return showHistoryForNode && node && typeof node.id !== "undefined" ? node.id : null;
    }

    getItems = (fromIndex, toIndex) => {
        const {versionId} = this.props;
        const {changeId, node, showHistoryForNode} = this.state;

        this.setState({
            fetching: true
        });

        return WebApi.findChanges(versionId, this.getNodeId(), fromIndex, toIndex - fromIndex, changeId)
            .then(json => {
                if (json.totalCount > 0 && changeId === null) {    // pokud nemáme uložen první changeId, uložíme si ho do state
                    this.setState({
                        changeId: json.changes[0].changeId
                    });
                }

                const lbData = {
                    items: json.changes,
                    count: json.totalCount,
                    outdated: json.outdated
                };

                this.setState({
                    fetching: false
                });

                return lbData;
            }).catch(e => {
                this.setState({
                    fetching: false
                });
            });
    }

    handleSelect = (item, index) => {
        if (!this.props.locked) {
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
    }

    handleShowSelectedItem = () => {
        if (!this.props.locked) {
            const {selectedIndex, activeIndex} = this.state;
            this.setState({
                activeIndex: selectedIndex
            }, () => {
                this.refs.listbox.ensureItemVisible(selectedIndex)
            })
        }
    }

    renderSelectedItemInfo = () => {
        const {selectedItem} = this.state;

        let infoText;
        if (selectedItem) {
            const description = this.getItemDescription(selectedItem);
            const typeText = this.getItemTypeText(selectedItem);
            const username = selectedItem.username ? selectedItem.username : 'System';
            infoText = `${dateToString(new Date(selectedItem.changeDate))}; ${timeToString(new Date(selectedItem.changeDate))}; ${description}; ${typeText}; ${username}`;
        } else {
            infoText = null;
        }

        return (
            <FormInput className="selected-item-info-container" type="static" label={i18n('arr.history.title.deleteFrom')}>
                <input type="text" value={infoText} disabled />
                <Button disabled={!selectedItem} onClick={this.handleShowSelectedItem}>{i18n("arr.history.action.deleteFrom.show")}</Button>
            </FormInput>
        )
    }

    handleChooseNode = () => {
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.nodes.title.select'),
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
                    this.props.dispatch(modalDialogHide());
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
        const {showHistoryForNode, node} = this.state;
        if (!(showHistoryForNode == true && node == null)) {
            this.refs.listbox.reload();
        }
    }

    handleDeleteChanges = () => {
        const {onDeleteChanges} = this.props;
        const {node, changeId, selectedIndex, selectedItem} = this.state;

        if (confirm(i18n('arr.history.deleteQuestion', selectedIndex + 1))) {
            onDeleteChanges(this.getNodeId(), changeId, selectedItem.changeId);
        }
    }

    handleGoToDateChange = (eventOrValue) => {
        const isEvent = !!(eventOrValue && eventOrValue.stopPropagation && eventOrValue.preventDefault);
        const value = isEvent ? eventOrValue.target.value : eventOrValue;

        const dateArr = value.match(/^(\d{2})\.(\d{2})\.(\d{4})(.(\d{2}):(\d{2})(:(\d{2}))?)?$/);
        let goToDateValue = null;
        if (dateArr) {
            const day = parseInt(dateArr[1]);
            const month = parseInt(dateArr[2]);
            const year = parseInt(dateArr[3]);
            const hh = parseInt(dateArr[5] ? dateArr[5] : "0");
            const mm = parseInt(dateArr[6] ? dateArr[6] : "0");
            const ss = parseInt(dateArr[8] ? dateArr[8] : "0");
            goToDateValue = new Date(year, month - 1, day, hh, mm, ss, 0);
        }
        // console.log(value, dateArr);
        // console.log(goToDateValue, dateTimeToLocalUTC(goToDateValue));

        this.setState({
            goToDate: value,
            goToDateValue
        })
    }

    handleGoToDate = () => {
        const {versionId} = this.props;
        const {goToDateValue, changeId, node, showHistoryForNode} = this.state;

        return WebApi.findChangesByDate(versionId, this.getNodeId(), changeId, dateTimeToZonedUTC(goToDateValue))
            .then(json => {
                const offset = json.offset;
                this.setState({
                    activeIndex: offset,
                }, () => {
                    this.refs.listbox.ensureItemVisible(offset);
                });
            });
    }

    render() {
        const {goToDateValue, goToDate, selectedItem, node, showHistoryForNode, selectedIndex, activeIndex, fetching} = this.state;
        const {onClose, locked} = this.props;

        let content;

        if (showHistoryForNode == true && node == null) {
            content = <div className="lazy-listbox-container listbox-container data-container loading">{i18n("arr.history.title.selectNode")}</div>
        } else {
            content = <LazyListBox
                ref="listbox"
                className="data-container"
                selectedIndex={selectedIndex}
                activeIndex={activeIndex}
                getItems={this.getItems}
                itemHeight={24} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                renderItemContent={this.renderItemContent}
                onSelect={this.handleSelect}
                fetching={fetching}
            />;
        }

        return (
            <div className="arr-history-form-container">
                <Modal.Body>
                    <FormGroup>
                        <FormInput disabled={fetching} type="radio" checked={!showHistoryForNode} onClick={() => this.onChangeRadio(false)} label={i18n('arr.history.title.globalChanges')}/>
                        <div className="selected-node-container">
                            <FormInput disabled={fetching} type="radio" checked={showHistoryForNode} onClick={() => this.onChangeRadio(true)} label={i18n('arr.history.title.nodeChanges')}/>
                            <FormInput className="selected-node-info-container" type="static" label={false}>
                                <input type="text" value={node ? node.name : ""} disabled />
                                <Button disabled={!showHistoryForNode || fetching} onClick={this.handleChooseNode}>{i18n("global.action.choose")}</Button>
                            </FormInput>
                        </div>
                        <div className="go-to-date-container">
                            <FormInput value={goToDate} placeholder="dd.mm.rrrr[ hh:mm[:ss]]" onChange={this.handleGoToDateChange} type="string" label={i18n("arr.history.title.goToDate")} />
                            <Button disabled={!goToDateValue} onClick={this.handleGoToDate}>{i18n("arr.history.action.goToDate")}</Button>
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
                        {content}
                    </div>
                    {!locked && this.renderSelectedItemInfo()}
                </Modal.Body>
                <Modal.Footer>
                    {selectedIndex !== null ? i18n("arr.history.title.changesForDelete", selectedIndex + 1) + " " : ""}
                    {!locked && <Button disabled={selectedItem === null || (showHistoryForNode && !node)} type="submit" onClick={this.handleDeleteChanges}>{i18n('arr.history.action.deleteChanges')}</Button>}
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

export default connect()(ArrHistoryForm);
