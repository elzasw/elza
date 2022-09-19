/**
 * Formulář zobrazení hostorie.
 */
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog.jsx';
import { WebApi } from 'actions/index.jsx';
import { ErrorBoundary } from 'components/ErrorBoundary';
import { FormInput, i18n, LazyListBox } from 'components/shared';
import { dateTimeToZonedUTC } from 'components/Utils';
import { dateToString, getScrollbarWidth, timeToString } from 'components/Utils.jsx';
import React, { useRef, useState } from 'react';
import { Form, Modal } from 'react-bootstrap';
import { NodeBase, SubNode } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import { Button } from '../ui';
import './ArrHistoryForm.scss';
import FundNodesSelectForm from './FundNodesSelectForm';
import { showConfirmDialog } from 'components/shared/dialog';
import Icon from 'components/shared/icon/FontIcon';

interface ArrHistoryFormProps {
    versionId: number;
    locked: boolean;
    onDeleteChanges: (nodeId: number | null, changeId: number, selectedChangeId: number) => Promise<void>;
    onClose: () => void;
    node?: NodeBase;
}

enum ChangeType {
    BULK_ACTION='BULK_ACTION',
    ADD_NODES_OUTPUT='ADD_NODES_OUTPUT',
    REMOVE_NODES_OUTPUT='REMOVE_NODES_OUTPUT',
    CREATE_AS='CREATE_AS',
    BATCH_CHANGE_DESC_ITEM='BATCH_CHANGE_DESC_ITEM',
    BATCH_DELETE_DESC_ITEM='BATCH_DELETE_DESC_ITEM',
    IMPORT='IMPORT',
}

interface ChangeItem {
    username: string;
    changeDate: string;
    changeId: number;
    revert: boolean;
    type: ChangeType | null;
    label: string | null;
    nodeChanges: number;
    primaryNodeId: number | null;
}

const isEvent = (event: any): event is React.ChangeEvent => {
    return !!(event && event.stopPropagation && event.preventDefault);
}

export const ArrHistoryFormFn = ({
    versionId,
    locked = false,
    onDeleteChanges,
    onClose,
    node,
}:ArrHistoryFormProps) => {
    const dispatch = useThunkDispatch();
    const listboxRef = useRef<any>(null); // TODO - pridat typ pro LazyListBox

    console.log("history node", node);
    const [currentNode, setCurrentNode] = useState(node);
    const [goToDate, setGoToDate] = useState("");
    const [goToDateValue, setGoToDateValue] = useState<Date | null>(null);
    const [changeId, setChangeId] = useState<number | null>(null);
    const [selectedItem, setSelectedItem] = useState<ChangeItem | null>(null);
    const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
    const [activeIndex, setActiveIndex] = useState<number | null>(null);
    const [fetching, setFetching] = useState(false);
    const [showHistoryForNode, setShowHistoryForNode] = useState(node ? true : false);
    const [inProgress, setInProgress] = useState(false);

    const renderItemContent = (item:ChangeItem, _isActive: boolean, index: number) => {
        if (item == null) {
            return null;
        }

        const canDelete = selectedItem && item.changeDate >= selectedItem.changeDate;
        const typeText = getItemTypeText(item);
        const description = getItemDescription(item);

        return (
            <div
                key={index}
                className={`row-container ${item.revert ? ' canRevert' : ''} ${canDelete ? ' delete' : ''}`}
            >
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                <div className="col col3" title={description}>
                    {description}
                </div>
                <div className="col col4" title={typeText}>
                    {typeText}
                </div>
                <div className="col col5">{item.username ? item.username : <i>System</i>}</div>
            </div>
        );
    };

    const getItemTypeText = (item: ChangeItem) => {
        return i18n(`arr.history.change.title.${item.type || 'unknown'}`);
    }

    const getItemDescription = (item: ChangeItem) => {
        const description = i18n('^arr.history.change.description.' + item.type, String(item.nodeChanges));

        if(description){ return description; }
        if (item.label) { return item.label; }

        return (
            getItemTypeText(item) +
            ', primaryNodeId: ' +
            (item.primaryNodeId || '?') +
            ', changeId: ' +
            item.changeId +
            ', changeDate: ' +
            dateToString(new Date(item.changeDate))
        );
    }

    // Returns id of the selected node. Returns null when id doesnt exist or when reverting global history
    const getNodeId = () => {
        return showHistoryForNode && currentNode?.id !== undefined 
            ? currentNode.id 
            : null;
    };

    const getItems = async (fromIndex: number, toIndex: number) => {
        setFetching(true);

        try {
            const {
                totalCount, 
                changes, 
                outdated
            } = await WebApi.findChanges(versionId, getNodeId(), fromIndex, toIndex - fromIndex, changeId)

            if (totalCount > 0 && changeId == null) {
                // pokud nemáme uložen první changeId, uložíme si ho do state
                setChangeId(changes[0].changeId)
            }

            setFetching(false);
            return {
                items: changes,
                count: totalCount,
                outdated: outdated,
            };;
        } catch (error) {
            setFetching(false);
        }


    };

    const handleSelect = (item: ChangeItem, index: number) => {
        if(locked){return;}
        if (item.revert) {
            setSelectedIndex(index);
            setActiveIndex(index);
            setSelectedItem(item);
        } else {
            setActiveIndex(index);
        }
    };

    const handleShowSelectedItem = () => {
        if(locked){return;}
        setActiveIndex(selectedIndex);
        listboxRef.current?.ensureItemVisible(selectedIndex);
    };

    const renderSelectedItemInfo = () => {
        let infoText: string | null = "";
        if (selectedItem) {
            const description = getItemDescription(selectedItem);
            const typeText = getItemTypeText(selectedItem);
            const username = selectedItem?.username || 'System';
            infoText = `${dateToString(new Date(selectedItem.changeDate))}; ${timeToString(
                new Date(selectedItem.changeDate),
            )}; ${description}; ${typeText}; ${username}`;
        }

        return (
            <ErrorBoundary>
                <FormInput
                    className="selected-node-info-container"
                    type="static"
                    label={i18n('arr.history.title.deleteFrom')}
                >
                    <span title={`${infoText}`} className="node-info full">
                        {`${infoText}`}
                    </span>
                    <Button variant="outline-secondary" disabled={!selectedItem} onClick={handleShowSelectedItem}>
                        {i18n('arr.history.action.deleteFrom.show')}
                    </Button>
                </FormInput>
            </ErrorBoundary>
        );
    };

    const handleChooseNode = () => {
        dispatch(
            modalDialogShow(
                undefined,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    multipleSelection={false}
                    onSubmitForm={(_id: number, node: NodeBase) => {
                        setCurrentNode(node);
                        setSelectedItem(null);
                        setSelectedIndex(null);
                        setChangeId(null);
                        setActiveIndex(null);
                        refreshRows()
                        dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    };

    const onChangeRadio = (showHistoryForNode:boolean) => {
        setShowHistoryForNode(showHistoryForNode);
        setSelectedItem(null);
        setSelectedIndex(null);
        setChangeId(null);
        setActiveIndex(null);

        if(showHistoryForNode && !currentNode){
            handleChooseNode();
        } else {
            refreshRows();
        }
    };

    const refreshRows = () => {
        if (!(showHistoryForNode === true && currentNode == null)) {
            listboxRef.current?.reload();
        }
    };

    const handleDeleteChanges = async () => {
        if(changeId == null 
            || selectedItem?.changeId == null 
            || selectedIndex == null
        ){return;}
        const response = await dispatch(showConfirmDialog(i18n('arr.history.deleteQuestion', selectedIndex + 1)))
        if (response) {
            setInProgress(true);
            await onDeleteChanges(getNodeId(), changeId, selectedItem.changeId);
            setInProgress(false);
            dispatch(modalDialogHide());
        }
    };

    const handleGoToDateChange = (eventOrValue: React.ChangeEvent<HTMLInputElement> | string) => {
        const value = isEvent(eventOrValue) ? eventOrValue.target.value : eventOrValue;

        const dateArr = value.match(/^(\d{2})\.(\d{2})\.(\d{4})(.(\d{2}):(\d{2})(:(\d{2}))?)?$/);
        let goToDateValue:Date | null = null;
        if (dateArr) {
            const day = parseInt(dateArr[1]);
            const month = parseInt(dateArr[2]);
            const year = parseInt(dateArr[3]);
            const hh = parseInt(dateArr[5] ? dateArr[5] : '0');
            const mm = parseInt(dateArr[6] ? dateArr[6] : '0');
            const ss = parseInt(dateArr[8] ? dateArr[8] : '0');
            goToDateValue = new Date(year, month - 1, day, hh, mm, ss, 0);
        }

        setGoToDate(value);
        setGoToDateValue(goToDateValue);
    };

    const handleGoToDate = async () => {
        const {offset} = await WebApi.findChangesByDate(versionId, getNodeId(), changeId, dateTimeToZonedUTC(goToDateValue))
        setActiveIndex(offset);
        listboxRef.current?.ensureItemVisible(offset);
    };

    let content: React.ReactNode;

    if (showHistoryForNode === true && currentNode == null) {
        content = (
            <div className="lazy-listbox-container listbox-container data-container loading">
                {i18n('arr.history.title.selectNode')}
            </div>
        );
    } else {
        content = (
            <LazyListBox
                key={'listbox'}
                ref={listboxRef}
                className="data-container"
                itemIdAttrName={'changeId'}
                selectedIndex={selectedIndex}
                activeIndex={activeIndex}
                getItems={getItems}
                itemHeight={24} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                renderItemContent={renderItemContent}
                onSelect={handleSelect}
                fetching={fetching}
                />
        );
    }

    if(inProgress){ return <div className="in-progress">
        <Icon glyph="fa-refresh" className="fa-spin"/>
        &nbsp;
        {i18n('arr.history.delete.inProgress')}
    </div>}

    return (
        <ErrorBoundary>
            <div className="arr-history-form-container">
                <Modal.Body>
                    <Form.Group>
                        <FormInput
                            disabled={fetching}
                            type="radio"
                            checked={!showHistoryForNode}
                            onClick={() => onChangeRadio(false)}
                            label={i18n('arr.history.title.globalChanges')}
                            className="radio"
                            />
                        <div className="selected-node-container">
                            <FormInput
                                disabled={fetching}
                                type="radio"
                                checked={showHistoryForNode}
                                onClick={() => onChangeRadio(true)}
                                label={i18n('arr.history.title.nodeChanges')}
                                className="radio"
                                />
                            {currentNode && <FormInput 
                                className="selected-node-info-container" 
                                type="static" 
                            >
                                <span title={`${currentNode.name}`} className="node-info">
                                    {`${currentNode.name}`}
                                </span>
                                <Button
                                    variant="outline-secondary"
                                    disabled={!showHistoryForNode || fetching}
                                    onClick={handleChooseNode}
                                >
                                    {i18n('global.action.choose')}
                                </Button>
                            </FormInput>}
                        </div>
                        <div className="go-to-date-container">
                            <FormInput
                                value={goToDate}
                                placeholder="dd.mm.rrrr[ hh:mm[:ss]]"
                                onChange={handleGoToDateChange}
                                type="string"
                                label={i18n('arr.history.title.goToDate')}
                                />
                            <Button
                                variant="outline-secondary"
                                disabled={!goToDateValue}
                                onClick={handleGoToDate}
                            >
                                {i18n('arr.history.action.goToDate')}
                            </Button>
                        </div>
                    </Form.Group>
                    <div className="changes-listbox-container">
                        <div className="header-container">
                            <div className="col col1">{i18n('arr.history.title.change.date')}</div>
                            <div className="col col2">{i18n('arr.history.title.change.time')}</div>
                            <div className="col col3">{i18n('arr.history.title.change.description')}</div>
                            <div className="col col4">{i18n('arr.history.title.change.type')}</div>
                            <div className="col col5">{i18n('arr.history.title.change.user')}</div>
                            <div className="colScrollbar" style={{width: getScrollbarWidth()}}></div>
                        </div>
                        {content}
                    </div>
                    {!locked && renderSelectedItemInfo()}
                </Modal.Body>
                <Modal.Footer>
                    {selectedIndex !== null
                        ? i18n('arr.history.title.changesForDelete', selectedIndex + 1) + ' '
                        : ''}
                    {!locked && (
                        <Button
                            variant="outline-secondary"
                            disabled={selectedItem === null || (showHistoryForNode && !currentNode)}
                            type="submit"
                            onClick={handleDeleteChanges}
                        >
                            {i18n('arr.history.action.deleteChanges')}
                        </Button>
                    )}
                    <Button variant="link" onClick={onClose}>
                        {i18n('global.action.cancel')}
                    </Button>
                </Modal.Footer>
            </div>
        </ErrorBoundary>
    );
}
export default ArrHistoryFormFn;
/*
class ArrHistoryForm extends AbstractReactComponent {
    static propTypes = {};

    static defaultProps = {
        locked: false,
    };

    constructor(props) {
        super(props);

        this.state = {
            node: typeof props.node !== 'undefined' ? props.node : null,
            goToDate: '',
            goToDateValue: null,
            changeId: null,
            selectedItem: null,
            selectedIndex: null,
            activeIndex: null,
            fetching: false,
            showHistoryForNode: props.node ? true : false, // pro node je true, globalni je false
        };
    }

    renderItemContent = (item, isActive, index) => {
        if (item == null) {
            return null;
        }

        const {selectedItem} = this.state;
        const canDelete = selectedItem && item.changeDate >= selectedItem.changeDate;
        const typeText = this.getItemTypeText(item);
        const description = this.getItemDescription(item);

        return (
            <div
                key={index}
                className={`row-container ${item.revert ? ' canRevert' : ''} ${canDelete ? ' delete' : ''}`}
            >
                <div className="col col1">{dateToString(new Date(item.changeDate))}</div>
                <div className="col col2">{timeToString(new Date(item.changeDate))}</div>
                <div className="col col3" title={description}>
                    {description}
                </div>
                <div className="col col4" title={typeText}>
                    {typeText}
                </div>
                <div className="col col5">{item.username ? item.username : <i>System</i>}</div>
            </div>
        );
    };

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
            default:
                console.warn('Unknown item change type:', item.type, item);
        }

        if (item.label) {
            return item.label;
        }

        return (
            this.getItemTypeText(item) +
            ', primaryNodeId: ' +
            (item.primaryNodeId || '?') +
            ', changeId: ' +
            item.changeId +
            ', changeDate: ' +
            dateToString(new Date(item.changeDate))
        );
    }

    // Returns id of the selected node. Returns null when id doesnt exist or when reverting global history
    getNodeId = () => {
        const {node, showHistoryForNode} = this.state;
        return showHistoryForNode && node && typeof node.id !== 'undefined' ? node.id : null;
    };

    getItems = (fromIndex, toIndex) => {
        const {versionId} = this.props;
        const {changeId} = this.state;

        this.setState({
            fetching: true,
        });

        return WebApi.findChanges(versionId, this.getNodeId(), fromIndex, toIndex - fromIndex, changeId)
            .then(json => {
                if (json.totalCount > 0 && changeId === null) {
                    // pokud nemáme uložen první changeId, uložíme si ho do state
                    this.setState({
                        changeId: json.changes[0].changeId,
                    });
                }

                const lbData = {
                    items: json.changes,
                    count: json.totalCount,
                    outdated: json.outdated,
                };

                this.setState({
                    fetching: false,
                });

                return lbData;
            })
            .catch(e => {
                this.setState({
                    fetching: false,
                });
            });
    };

    handleSelect = (item, index) => {
        if (!this.props.locked) {
            if (item.revert) {
                this.setState({
                    selectedIndex: index,
                    activeIndex: index,
                    selectedItem: item,
                });
            } else {
                this.setState({
                    activeIndex: index,
                });
            }
        }
    };

    handleShowSelectedItem = () => {
        if (!this.props.locked) {
            const {selectedIndex} = this.state;
            this.setState(
                {
                    activeIndex: selectedIndex,
                },
                () => {
                    this.refs.listbox.ensureItemVisible(selectedIndex);
                },
            );
        }
    };

    renderSelectedItemInfo = () => {
        const {selectedItem} = this.state;

        let infoText;
        if (selectedItem) {
            const description = this.getItemDescription(selectedItem);
            const typeText = this.getItemTypeText(selectedItem);
            const username = selectedItem.username ? selectedItem.username : 'System';
            infoText = `${dateToString(new Date(selectedItem.changeDate))}; ${timeToString(
                new Date(selectedItem.changeDate),
            )}; ${description}; ${typeText}; ${username}`;
        } else {
            infoText = null;
        }

        return (
            <ErrorBoundary>
                <FormInput
                    className="selected-item-info-container"
                    type="static"
                    label={i18n('arr.history.title.deleteFrom')}
                >
                    <input type="text" value={infoText} disabled />
                    <Button disabled={!selectedItem} onClick={this.handleShowSelectedItem}>
                        {i18n('arr.history.action.deleteFrom.show')}
                    </Button>
                </FormInput>
            </ErrorBoundary>
        );
    };

    handleChooseNode = () => {
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    multipleSelection={false}
                    onSubmitForm={(id, node) => {
                        this.setState(
                            {
                                node,
                                selectedItem: null,
                                selectedIndex: null,
                                changeId: null,
                                activeIndex: null,
                            },
                            this.refreshRows,
                        );
                        this.props.dispatch(modalDialogHide());
                    }}
                />,
            ),
        );
    };

    onChangeRadio = showHistoryForNode => {
        this.setState(
            {
                showHistoryForNode,
                selectedItem: null,
                selectedIndex: null,
                changeId: null,
                activeIndex: null,
            },
            this.refreshRows,
        );
    };

    refreshRows = () => {
        const {showHistoryForNode, node} = this.state;
        if (!(showHistoryForNode === true && node == null)) {
            this.refs.listbox.reload();
        }
    };

    handleDeleteChanges = () => {
        const {onDeleteChanges} = this.props;
        const {changeId, selectedIndex, selectedItem} = this.state;

        if (window.confirm(i18n('arr.history.deleteQuestion', selectedIndex + 1))) {
            onDeleteChanges(this.getNodeId(), changeId, selectedItem.changeId);
        }
    };

    handleGoToDateChange = eventOrValue => {
        const isEvent = !!(eventOrValue && eventOrValue.stopPropagation && eventOrValue.preventDefault);
        const value = isEvent ? eventOrValue.target.value : eventOrValue;

        const dateArr = value.match(/^(\d{2})\.(\d{2})\.(\d{4})(.(\d{2}):(\d{2})(:(\d{2}))?)?$/);
        let goToDateValue = null;
        if (dateArr) {
            const day = parseInt(dateArr[1]);
            const month = parseInt(dateArr[2]);
            const year = parseInt(dateArr[3]);
            const hh = parseInt(dateArr[5] ? dateArr[5] : '0');
            const mm = parseInt(dateArr[6] ? dateArr[6] : '0');
            const ss = parseInt(dateArr[8] ? dateArr[8] : '0');
            goToDateValue = new Date(year, month - 1, day, hh, mm, ss, 0);
        }
        // console.log(value, dateArr);
        // console.log(goToDateValue, dateTimeToLocalUTC(goToDateValue));

        this.setState({
            goToDate: value,
            goToDateValue,
        });
    };

    handleGoToDate = () => {
        const {versionId} = this.props;
        const {goToDateValue, changeId} = this.state;

        return WebApi.findChangesByDate(versionId, this.getNodeId(), changeId, dateTimeToZonedUTC(goToDateValue)).then(
            json => {
                const offset = json.offset;
                this.setState(
                    {
                        activeIndex: offset,
                    },
                    () => {
                        this.refs.listbox.ensureItemVisible(offset);
                    },
                );
            },
        );
    };

    render() {
        const {
            goToDateValue,
            goToDate,
            selectedItem,
            node,
            showHistoryForNode,
            selectedIndex,
            activeIndex,
            fetching,
        } = this.state;
        const {onClose, locked} = this.props;

        let content;

        if (showHistoryForNode === true && node == null) {
            content = (
                <div className="lazy-listbox-container listbox-container data-container loading">
                    {i18n('arr.history.title.selectNode')}
                </div>
            );
        } else {
            content = (
                <LazyListBox
                    key={'listbox'}
                    ref="listbox"
                    className="data-container"
                    itemIdAttrName={'changeId'}
                    selectedIndex={selectedIndex}
                    activeIndex={activeIndex}
                    getItems={this.getItems}
                    itemHeight={24} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                    renderItemContent={this.renderItemContent}
                    onSelect={this.handleSelect}
                    fetching={fetching}
                />
            );
        }

        return (
            <ErrorBoundary>
                <div className="arr-history-form-container">
                    <Modal.Body>
                        <Form.Group>
                            <FormInput
                                disabled={fetching}
                                type="radio"
                                checked={!showHistoryForNode}
                                onClick={() => this.onChangeRadio(false)}
                                label={i18n('arr.history.title.globalChanges')}
                            />
                            <div className="selected-node-container">
                                <FormInput
                                    disabled={fetching}
                                    type="radio"
                                    checked={showHistoryForNode}
                                    onClick={() => this.onChangeRadio(true)}
                                    label={i18n('arr.history.title.nodeChanges')}
                                />
                                <FormInput className="selected-node-info-container" type="static" label={false}>
                                    <input type="text" value={node ? node.name : ''} disabled />
                                    <Button
                                        variant="outline-secondary"
                                        disabled={!showHistoryForNode || fetching}
                                        onClick={this.handleChooseNode}
                                    >
                                        {i18n('global.action.choose')}
                                    </Button>
                                </FormInput>
                            </div>
                            <div className="go-to-date-container">
                                <FormInput
                                    value={goToDate}
                                    placeholder="dd.mm.rrrr[ hh:mm[:ss]]"
                                    onChange={this.handleGoToDateChange}
                                    type="string"
                                    label={i18n('arr.history.title.goToDate')}
                                />
                                <Button
                                    variant="outline-secondary"
                                    disabled={!goToDateValue}
                                    onClick={this.handleGoToDate}
                                >
                                    {i18n('arr.history.action.goToDate')}
                                </Button>
                            </div>
                        </Form.Group>
                        <div className="changes-listbox-container">
                            <div className="header-container">
                                <div className="col col1">{i18n('arr.history.title.change.date')}</div>
                                <div className="col col2">{i18n('arr.history.title.change.time')}</div>
                                <div className="col col3">{i18n('arr.history.title.change.description')}</div>
                                <div className="col col4">{i18n('arr.history.title.change.type')}</div>
                                <div className="col col5">{i18n('arr.history.title.change.user')}</div>
                                <div className="colScrollbar" style={{width: getScrollbarWidth()}}></div>
                            </div>
                            {content}
                        </div>
                        {!locked && this.renderSelectedItemInfo()}
                    </Modal.Body>
                    <Modal.Footer>
                        {selectedIndex !== null
                            ? i18n('arr.history.title.changesForDelete', selectedIndex + 1) + ' '
                            : ''}
                        {!locked && (
                            <Button
                                variant="outline-secondary"
                                disabled={selectedItem === null || (showHistoryForNode && !node)}
                                type="submit"
                                variant="outline-secondary"
                                onClick={this.handleDeleteChanges}
                            >
                                {i18n('arr.history.action.deleteChanges')}
                            </Button>
                        )}
                        <Button variant="link" onClick={onClose}>
                            {i18n('global.action.cancel')}
                        </Button>
                    </Modal.Footer>
                </div>
            </ErrorBoundary>
        );
    }
}

export default connect()(ArrHistoryForm);
*/
