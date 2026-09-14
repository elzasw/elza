/**
 * Formulář zobrazení hostorie.
 */
import { modalDialogHide, modalDialogShow } from 'actions/global/modalDialog.jsx';
import { WebApi } from 'actions/index.jsx';
import { ErrorBoundary } from 'components/ErrorBoundary';
import { FormInput, LazyListBox } from 'components/shared';
import { FormattedMessage, useIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import { nodeListMessages } from './nodeListMessages';
import { arrMessages } from './messages';
import { historyChangeMessages, historyDescriptionMessages } from './historyMessages';
import { messageFor } from 'components/shared/lang/dynamicMessage';
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
    locked?: boolean;
    onDeleteChanges: (nodeId: number | null, changeId: number, selectedChangeId: number) => Promise<void>;
    onClose?: () => void;
    node?: SubNode;
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
    const intl = useIntl();
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
        return intl.formatMessage(messageFor(historyChangeMessages, item.type ?? 'unknown', historyChangeMessages.unknown));
    }

    const getItemDescription = (item: ChangeItem) => {
        const descriptor = item.type ? historyDescriptionMessages[item.type] : undefined;
        const description = descriptor
            ? intl.formatMessage(descriptor, { 0: String(item.nodeChanges) })
            : null;

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

            if (changes.length > 0 && changeId == null) {
                // pokud nemáme uložen první changeId, uložíme si ho do state
                setChangeId(changes[0].changeId)
            }

            setFetching(false);

            const requested = toIndex - fromIndex;
            const isLastPage = changes.length < requested;
            const count = isLastPage
                ? fromIndex + changes.length // dosáhli jsme konce – zaznamenáme skutečnou velikost
                : toIndex + requested; // ještě není konec – necháme si „rezervu“ na jednu stránku dopředu

            return {
                items: changes,
                count,
                outdated,
            };
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
                    label={<FormattedMessage {...arrMessages.historyTitleDeleteFrom} />}
                >
                    <span title={`${infoText}`} className="node-info full">
                        {`${infoText}`}
                    </span>
                    <Button variant="outline-secondary" disabled={!selectedItem} onClick={handleShowSelectedItem}>
                        {<FormattedMessage {...arrMessages.historyActionDeleteFromShow} />}
                    </Button>
                </FormInput>
            </ErrorBoundary>
        );
    };

    const handleChooseNode = () => {
        dispatch(
            modalDialogShow(
                undefined,
                intl.formatMessage(nodeListMessages.select),
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
        const response = await dispatch(showConfirmDialog(intl.formatMessage(arrMessages.historyDeleteQuestion, { 0: selectedIndex + 1 })))
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
                {<FormattedMessage {...arrMessages.historyTitleSelectNode} />}
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
        {<FormattedMessage {...arrMessages.historyDeleteInProgress} />}
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
                            label={<FormattedMessage {...arrMessages.historyTitleGlobalChanges} />}
                            className="radio"
                            />
                        <div className="selected-node-container">
                            <FormInput
                                disabled={fetching}
                                type="radio"
                                checked={showHistoryForNode}
                                onClick={() => onChangeRadio(true)}
                                label={<FormattedMessage {...arrMessages.historyTitleNodeChanges} />}
                                className="radio"
                                />
                            {currentNode && <FormInput
                                className="selected-node-info-container"
                                type="static"
                            >
                                <span title={`${currentNode.accordionLeft}`} className="node-info">
                                    {`${currentNode.accordionLeft}`}
                                </span>
                                <Button
                                    variant="outline-secondary"
                                    disabled={!showHistoryForNode || fetching}
                                    onClick={handleChooseNode}
                                >
                                    {<FormattedMessage {...globalMessages.choose} />}
                                </Button>
                            </FormInput>}
                        </div>
                        <div className="go-to-date-container">
                            <FormInput
                                value={goToDate}
                                placeholder="dd.mm.rrrr[ hh:mm[:ss]]"
                                onChange={handleGoToDateChange}
                                type="string"
                                label={<FormattedMessage {...arrMessages.historyTitleGoToDate} />}
                                />
                            <Button
                                variant="outline-secondary"
                                disabled={!goToDateValue}
                                onClick={handleGoToDate}
                            >
                                {<FormattedMessage {...arrMessages.historyActionGoToDate} />}
                            </Button>
                        </div>
                    </Form.Group>
                    <div className="changes-listbox-container">
                        <div className="header-container">
                            <div className="col col1">{<FormattedMessage {...arrMessages.historyTitleChangeDate} />}</div>
                            <div className="col col2">{<FormattedMessage {...arrMessages.historyTitleChangeTime} />}</div>
                            <div className="col col3">{<FormattedMessage {...arrMessages.historyTitleChangeDescription} />}</div>
                            <div className="col col4">{<FormattedMessage {...arrMessages.historyTitleChangeType} />}</div>
                            <div className="col col5">{<FormattedMessage {...arrMessages.historyTitleChangeUser} />}</div>
                            <div className="colScrollbar" style={{width: getScrollbarWidth()}}></div>
                        </div>
                        {content}
                    </div>
                    {!locked && renderSelectedItemInfo()}
                </Modal.Body>
                <Modal.Footer>
                    {selectedIndex !== null
                        ? intl.formatMessage(arrMessages.historyTitleChangesForDelete, { 0: selectedIndex + 1 }) + ' '
                        : ''}
                    {!locked && (
                        <Button
                            variant="outline-secondary"
                            disabled={selectedItem === null || (showHistoryForNode && !currentNode)}
                            type="submit"
                            onClick={handleDeleteChanges}
                        >
                            {<FormattedMessage {...arrMessages.historyActionDeleteChanges} />}
                        </Button>
                    )}
                    <Button variant="link" onClick={onClose}>
                        {<FormattedMessage {...globalMessages.cancel} />}
                    </Button>
                </Modal.Footer>
            </div>
        </ErrorBoundary>
    );
}
export default ArrHistoryFormFn;