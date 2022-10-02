/**
 * Lišta akcí pro jednotku popisu
 *
 * @author Jakub Randák
 * @author Tomáš Pytelka
 * @since 31.8.2016
 */

import {
    fundNodeSubNodeFulltextSearch,
    fundSelectSubNode,
    fundSubNodesNextPage,
    fundSubNodesPrevPage
} from 'actions/arr/node.jsx';
import { i18n, Icon, Search } from 'components/shared';
import React, { useRef } from 'react';
import { Field, Form as FinalForm } from 'react-final-form';
import { useThunkDispatch } from 'utils/hooks';
import AddNodeCross from './AddNodeCross';
import './NodeActionsBar.scss';
import { Node, UserDetail } from 'typings/store';

interface NodeNavigationValues {
    position: number;
}

interface NodeNavigationProps {
    node: Node;
    versionId: number;
    onPrevNode: (event: React.MouseEvent) => void;
    onNextNode: (event: React.MouseEvent) => void;
    onMoveForward: (event: React.MouseEvent) => void;
    onMoveBackward: (event: React.MouseEvent) => void;
    simplified?: boolean;
}

const NodeNavigation = ({
    node, 
    versionId,
    onPrevNode,
    onMoveBackward,
    onNextNode,
    onMoveForward,
    simplified,
}: NodeNavigationProps) => {
    const dispatch = useThunkDispatch();
    const inputRef = useRef<HTMLInputElement>(null);

    const handleFindPositionSubmit = ({position}:NodeNavigationValues) => {
        const value = position > node.nodeCount ? node.nodeCount : position;
        const index = value - 1;
        dispatch(fundSelectSubNode(versionId, undefined, node, false, null, false, index, true));
    }

    const getInputText = () => node.nodeIndex + 1;

    /**
     * Akce kontrolující zda je na uzly použit filtr
     */
    const isFilterUsed = () => node.filterText ? true : false;

    const handleWrapperFocus = () => {
        inputRef.current && inputRef.current.focus();
    }

    return (
        <div className="node-navigation">
            {!simplified && <button
                className={`btn left`}
                disabled={node.viewStartIndex === 0 || node.selectedSubNodeId == undefined}
                onClick={e => onMoveBackward(e)}
                title={i18n( `arr.fund.subNodes.prevPage`, node.pageSize)}
            >
                <Icon glyph={'fa-backward'} />
            </button>}
            <button
                className={`btn left`}
                disabled={node.nodeIndex === 0}
                onClick={e => onPrevNode(e)}
                title={i18n( `arr.fund.subNodes.prev`)}
            >
                <Icon glyph={'fa-caret-left'} />
            </button>
            <FinalForm<NodeNavigationValues>
                initialValues={{
                    position: getInputText() 
                }} 
                onSubmit={handleFindPositionSubmit}
            >
                {({handleSubmit}) => {
                    return <form onSubmit={handleSubmit}>
                        <Field name="position">
                            {({input}) => {
                                const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
                                    if(e.currentTarget.value === ""){input.onChange(""); return;}
                                    const integerValue = parseInt(e.currentTarget.value);
                                    const value = integerValue < 1 ? 1 : integerValue;
                                    input.onChange(value);
                                }
                                const onFocus = (e: React.FocusEvent<HTMLInputElement>) => {
                                    input.onFocus(e);
                                    e.currentTarget.select();
                                }
                                const onBlur = (e: React.FocusEvent<HTMLInputElement>) => {
                                    input.onChange(getInputText());
                                    input.onBlur(e)
                                }
                                return <div 
                                    className="input-wrapper" 
                                    onClick={handleWrapperFocus}
                                >
                                    <input 
                                        disabled={isFilterUsed()}
                                        ref={inputRef}
                                        {...input} 
                                        onBlur={onBlur} 
                                        onFocus={onFocus} 
                                        onChange={onChange}
                                        autoComplete="off"
                                        />
                                    <div> / {node.nodeCount}</div>
                                </div>
                            }}
                        </Field>
                    </form>
                }}
            </FinalForm>
            <button
                className={`btn right`}
                disabled={node.nodeIndex + 1 === node.nodeCount}
                onClick={e => onNextNode(e)}
                title={i18n( `arr.fund.subNodes.next`)}
            >
                <Icon glyph={'fa-caret-right'} />
            </button>
            {!simplified && <button
                className={`btn right`}
                disabled={node.viewStartIndex + node.pageSize >= node.nodeCount || node.selectedSubNodeId == undefined}
                onClick={e => onMoveForward(e)}
                title={i18n( `arr.fund.subNodes.nextPage`, node.pageSize)}
            >
                <Icon glyph={'fa-forward'} />
            </button>}
        </div>
    )

}

interface NodeActionsBarProps {
    simplified?: boolean;
    node: Node;
    selectedSubNodeIndex: number;
    versionId: number;
    userDetail: UserDetail;
    fundId: number;
    closed: boolean;
    onSwitchNode: (direction: "prevItem" | "nextItem", event: React.MouseEvent) => void;
    arrPerm: boolean;
}

const NodeActionsBar = ({
    simplified,
    node,
    selectedSubNodeIndex,
    versionId,
    userDetail,
    fundId,
    closed,
    onSwitchNode,
    arrPerm,
}: NodeActionsBarProps) => {
    const dispatch = useThunkDispatch();

    const onNextAction = () => {
        if(simplified) return;
        console.log(node.selectedSubNodeId)
        dispatch(fundSubNodesNextPage(versionId, node.id, node.routingKey));
    };

    const onPrevAction = () => {
        if(simplified) return;
        dispatch(fundSubNodesPrevPage(versionId, node.id, node.routingKey));
    };

    const onNextNode = (e: React.MouseEvent) => onSwitchNode('nextItem', e)
    const onPrevNode = (e: React.MouseEvent) => onSwitchNode('prevItem', e)

    return (
        <div key="actions" className="node-actions-bar">
            <div key="actions" className="actions">
                <AddNodeCross
                    node={node}
                    selectedSubNodeIndex={selectedSubNodeIndex}
                    versionId={versionId}
                    userDetail={userDetail}
                    fundId={fundId}
                    arrPerm={arrPerm}
                    closed={closed}
                    />
                <div className="button-wrap">
                    <div className="left-side">
                        {!simplified && (
                            <Search
                                tabIndex={-1}
                                className="search-input"
                                placeholder={i18n('search.input.filter')}
                                value={node.filterText}
                                onClear={() => {
                                    dispatch(fundNodeSubNodeFulltextSearch(''));
                                }}
                                onSearch={(value:string) => {
                                    dispatch(fundNodeSubNodeFulltextSearch(value));
                                }}
                                filter
                                />
                        )}
                    </div>
                    <div className="right-side">
                        <NodeNavigation 
                            node={node} 
                            versionId={versionId}
                            simplified={simplified}
                            onMoveForward={onNextAction}
                            onNextNode={onNextNode}
                            onPrevNode={onPrevNode}
                            onMoveBackward={onPrevAction}
                            />
                    </div>
                </div>
            </div>
        </div>
    );
}
export default NodeActionsBar;
