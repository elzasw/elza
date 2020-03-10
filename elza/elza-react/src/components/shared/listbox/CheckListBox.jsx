import React from 'react';
import {PropTypes} from 'prop-types';
import AbstractReactComponent from '../../AbstractReactComponent';
import {FormCheck} from 'react-bootstrap';
import {Button} from '../../ui';
import ReactDOM from 'react-dom';
import {Shortcuts} from 'react-shortcuts';
import * as Utils from '../../Utils';
import scrollIntoView from 'dom-scroll-into-view';

import defaultKeymap from './ListBoxKeymap.jsx';
import './CheckListBox.scss';

let _ListBox_placeholder = document.createElement('div');
let _ListBox_placeholder_cls = 'placeholder';
_ListBox_placeholder.className = _ListBox_placeholder_cls;

const PAGE_SIZE = 10;

/**
 *  ListBox komponenta.
 *
 **/
class ListBox extends AbstractReactComponent {
    state = {
        activeIndexes: null,
        checkedIndexes: null,
        lastFocus: null,
    };

    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    UNSAFE_componentWillMount() {
        Utils.addShortcutManager(this, defaultKeymap);
    }

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    constructor(props) {
        super(props);

        if (props.multiselect) {
            var activeIndexes = {};
            if (typeof props.activeIndexes !== 'undefined' && props.activeIndexes !== null) {
                props.activeIndexes.forEach(index => activeIndexes[index] = true);
            }
            this.state = {
                activeIndexes: activeIndexes,
                lastFocus: null,
            };
        } else {
            this.state = {
                activeIndex: this.getActiveIndexForUse(props, {}),
                lastFocus: null,
            };
        }
    }

    static propTypes = {
        items: PropTypes.array.isRequired,
        onSelect: PropTypes.func,
        onCheck: PropTypes.func,
        onDelete: PropTypes.func,
        canSelectItem: PropTypes.func,
        multiselect: PropTypes.bool,
        onFocus: PropTypes.func,
        onChangeSelection: PropTypes.func,
        activeIndex: PropTypes.number,
        activeIndexes: PropTypes.array,
        onChangeOrder: PropTypes.func,
        className: PropTypes.string,
        renderItemContent: PropTypes.func,
        sortable: PropTypes.bool,
        filter: PropTypes.object,
    };

    static defaultProps = {
        renderItemContent: ({item, active, index}, onCheckItem) => {
            return (
                <div>{item.name}</div>
            );
        },
        canSelectItem: (item, index) => {
            return true;
        },
    };

    selectorMoveUp = (e) => {
        this.selectorMoveRelative(-1);
    };
    selectorMoveDown = (e) => {
        this.selectorMoveRelative(1);
    };
    selectorMovePageUp = (e) => {
        this.selectorMoveRelative(-1 * PAGE_SIZE);
    };
    selectorMovePageDown = (e) => {
        this.selectorMoveRelative(PAGE_SIZE);
    };
    selectorMoveTop = (e) => {
        this.selectorMoveToIndex(0);
    };
    selectorMoveEnd = (e) => {
        this.selectorMoveToIndex(this.props.items.length - 1);
    };
    selectedItemCheck = (e) => {
        this.selectedItemOperation(this.props.onCheck);
    };
    selectedItemDelete = (e) => {
        this.selectedItemOperation(this.props.onDelete);
    };
    selectItem = (e) => {
        this.selectedItemOperation(e, this.props.onSelect);
    };
    /**
     * Wrapper for item operations from props. Checks if the operation exists and that an item is selected.
     * @param {function} operation
     */
    selectedItemOperation = (e, operation) => {
        const {items, multiselect} = this.props;

        if (multiselect) {
            let {activeIndexes, checkedIndexes} = this.state;
            const selectedIndexes = Object.keys(activeIndexes);

            checkedIndexes = {...checkedIndexes};
            Object.keys(activeIndexes).map((i) => {
                const checkedIndex = checkedIndexes[i];
                if (checkedIndex) {
                    delete checkedIndexes[i];
                } else {
                    checkedIndexes[i] = true;
                }
            });

            this.setState({
                checkedIndexes,
            }, () => this.props.onChangeSelection && this.props.onChangeSelection(Object.keys(checkedIndexes)));

            if (operation && selectedIndexes.length > 0) {
                const selectedItems = [];
                for (let a = 0; a < selectedIndexes.length; a++) {
                    selectedItems.push(items[selectedIndexes[a]]);
                }
                operation(selectedItems, selectedIndexes, e);
            }
        } else {
            const {activeIndex} = this.state;
            if (operation && activeIndex !== null) {
                operation(items[activeIndex], activeIndex, e);
                this.props.onChangeSelection && this.props.onChangeSelection([activeIndex]);
            }
        }
    };
    selectorMoveRelative = (step) => {
        const {lastFocus} = this.state;
        var newIndex = this.getRelativeSelectableItemIndex(lastFocus, step);
        this.selectorMoveToIndex(newIndex);
    };
    selectorMoveToIndex = (index) => {
        const {items, multiselect} = this.props;
        if (items.length > 0) {
            if (index !== null) {
                var state = multiselect ? {lastFocus: index, activeIndexes: {[index]: true}} : {
                    lastFocus: index,
                    activeIndex: index,
                };
                this.setState(state, this.ensureItemVisible.bind(this, index));
                this.props.onFocus && this.props.onFocus(items[index], index);
            }
        }
    };
    actionMap = {
        'MOVE_UP': this.selectorMoveUp,
        'MOVE_DOWN': this.selectorMoveDown,
        'MOVE_PAGE_UP': this.selectorMovePageUp,
        'MOVE_PAGE_DOWN': this.selectorMovePageDown,
        'MOVE_TOP': this.selectorMoveTop,
        'MOVE_END': this.selectorMoveEnd,
        'ITEM_CHECK': this.selectedItemCheck,
        'ITEM_DELETE': this.selectedItemDelete,
        'ITEM_SELECT': this.selectItem,
    };
    handleShortcuts = (action, e) => {
        e.stopPropagation();
        e.preventDefault();
        this.actionMap[action](e);
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {multiselect, filter, items} = this.props;

        if (multiselect !== nextProps.multiselect || filter !== nextProps.filter) {
            this.handleClear();
        }

        if (nextProps.multiselect) {
            if (typeof nextProps.activeIndexes !== 'undefined') {
                var activeIndexes = {};
                nextProps.activeIndexes !== null && nextProps.activeIndexes.forEach(index => activeIndexes[index] = true);
                this.setState({
                    activeIndexes: activeIndexes,
                });
            }
        } else {
            this.setState({
                activeIndex: this.getActiveIndexForUse(nextProps, this.state),
                lastFocus: this.getActiveIndexForUse(nextProps, this.state),
            });
        }
    }

    handleClick = (index, e) => {
        const {items, multiselect, canSelectItem} = this.props;
        var {activeIndexes, checkedIndexes, lastFocus} = this.state;

        if (multiselect) {
            if (e.ctrlKey || e.spaceKey) {
                if (activeIndexes && activeIndexes[index]) { // je označená, odznačíme ji
                    activeIndexes = {...activeIndexes};
                    delete activeIndexes[index];
                } else {
                    if (canSelectItem(items[index], index)) {
                        activeIndexes = {...activeIndexes, [index]: true};
                    }
                }
            } else if (e.shiftKey) {
                this.unFocus();

                const from = Math.min(index, lastFocus);
                const to = Math.max(index, lastFocus);
                activeIndexes = {...activeIndexes};
                for (var a = from; a <= to; a++) {
                    if (canSelectItem(items[a], a)) {
                        activeIndexes[a] = true;
                    }
                }
            } else {
                if (canSelectItem(items[index], index)) {
                    activeIndexes = {[index]: true};
                }
            }
            this.setState({
                activeIndexes: activeIndexes,
                lastFocus: index,
            });
            this.props.onChangeSelection && this.props.onChangeSelection(Object.keys(checkedIndexes || {}));
        } else {
            if (canSelectItem(items[index], index)) {
                this.setState({
                    activeIndex: index,
                    lastFocus: index,
                }, () => this.props.onChangeSelection && this.props.onChangeSelection([this.state.activeIndex]));
                this.props.onFocus && this.props.onFocus(items[index], index);
            }
        }
    };

    unFocus = () => {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges();
        }
    };

    dragStart = (index, e) => {
        const {items, multiselect, canSelectItem} = this.props;

        this.dragged = e.currentTarget;
        e.dataTransfer.effectAllowed = 'move';
        // Firefox requires dataTransfer data to be set
        e.dataTransfer.setData('text/html', e.currentTarget);

        var canSelect = canSelectItem(items[index], index);
    };

    dragEnd = (e) => {
        this.dragged.style.display = 'block';
        this.dragged.parentNode.removeChild(_ListBox_placeholder);
        // Update data
        var data = this.state.data;
        var from = Number(this.dragged.dataset.id);
        var to = Number(this.over.dataset.id);
        if (from < to) to--;
        if (this.nodePlacement == 'after') to++;

        if (from !== to) {
            this.props.onChangeOrder(from, to);
        }
    };

    dragOver = (e) => {
        e.preventDefault();
        this.dragged.style.display = 'none';
        if (e.target.className == _ListBox_placeholder_cls) return;
        this.over = e.target;
        // Inside the dragOver method
        var relY = e.clientY - this.over.offsetTop;
        var height = this.over.offsetHeight / 2;
        var parent = e.target.parentNode;
        var container = this.refs.container;

        var realTarget = e.target;
        var found = false;
        while (realTarget !== null) {
            if (typeof realTarget.dataset.id !== 'undefined') {
                found = true;
                break;
            }
            if (realTarget == container) {
                realTarget = container.lastChild;
                if (realTarget.className.indexOf(_ListBox_placeholder_cls) > -1) {
                    realTarget = realTarget.previousSibling;
                }
                found = true;
                break;
            }
            realTarget = realTarget.parentNode;
        }

        if (!found) {
            return;
        }

        this.over = realTarget;

        // Inside the dragOver method
        var parent = realTarget.parentNode;
        var overRect = this.over.getBoundingClientRect();
        var height2 = (overRect.bottom - overRect.top) / 2;

        if (e.clientY < overRect.top + height2) {
            this.nodePlacement = 'before';
            parent.insertBefore(_ListBox_placeholder, realTarget);
        } else if (e.clientY >= overRect.top + height2) {
            this.nodePlacement = 'after';
            parent.insertBefore(_ListBox_placeholder, realTarget.nextElementSibling);
        } else {

        }
    };
    getRelativeSelectableItemIndex = (index, step) => {
        const {items, canSelectItem} = this.props;
        var isDecrementing = step < 0;
        if (index || index === 0) {
            while (step) {
                var i = index + step;
                while (i >= 0 && i < items.length) {
                    if (canSelectItem(items[i], i)) {
                        return i;
                    }
                    isDecrementing ? i-- : i++;
                }
                isDecrementing ? step++ : step--;
            }
            return index;
        } else {
            return 0;
        }
    };

    getActiveIndexForUse(props, state) {
        var index;

        if (typeof props.activeIndex !== 'undefined') {
            index = props.activeIndex;
        } else if (typeof state.activeIndex !== 'undefined') {
            index = state.activeIndex;
        } else {
            index = null;
        }

        if (index < 0 || index >= props.items.length) {
            index = null;
        }
        return index;
    }

    ensureItemVisible = (index) => {
        var itemNode = ReactDOM.findDOMNode(this.refs['item-' + index]);
        if (itemNode !== null) {
            var containerNode = ReactDOM.findDOMNode(this.refs.container);
            scrollIntoView(itemNode, containerNode, {onlyScrollIfNeeded: true, alignWithTop: false});
        }
    };

    handleDoubleClick = (e) => {
        const {onDoubleClick} = this.props;

        if (onDoubleClick) {
            this.unFocus();
            onDoubleClick(e);
        }
    };

    unFocus = () => {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges();
        }
    };

    focus = () => {
        this.setState({}, () => {
            ReactDOM.findDOMNode(this.refs.wrapper).focus();
        });
    };

    handleCheckItem = (e, item, index) => {
        const {multiselect} = this.props;

        e.stopPropagation();
        e.preventDefault();

        // Pokud je položka v seznamu označených, bud event obsahovat všechny, jinak je aktuálně kliknutou
        let isItemActive;
        if (multiselect) {
            const {activeIndexes} = this.state;
            isItemActive = activeIndexes[index];
        } else {
            const {activeIndex} = this.state;
            isItemActive = activeIndex;
        }

        if (isItemActive) { // je jedna z označených
            this.selectedItemOperation(this.props.onCheck);
        } else {    // je mimo
            this.handleClick(index, e);
            this.setState({}, () => {
                this.selectedItemOperation(this.props.onCheck);
            });
        }
    };

    handleCheckAll = () => {
        const {items} = this.props;
        let {checkedIndexes} = this.state;
        const checkedAll = checkedIndexes &&
            Object.keys(checkedIndexes).length === items.length &&
            Object.keys(checkedIndexes).every((i) => checkedIndexes[i]);

        checkedIndexes = {};
        items.map((item, index) => checkedIndexes[index] = true);

        this.setState({
            checkedIndexes: checkedAll ? {} : checkedIndexes,
        }, () => this.props.onChangeSelection && this.props.onChangeSelection(Object.keys(checkedIndexes)));
    };

    handleClear = () => {
        this.setState({
            activeIndex: null,
            activeIndexes: null,
            checkedIndexes: null,
        }, () => this.props.onChangeSelection && this.props.onChangeSelection([]));
    };

    render() {
        const {className, items, renderItemContent, multiselect} = this.props;
        const {activeIndex, activeIndexes, checkedIndexes} = this.state;

        var cls = 'listbox-container';
        var wrapperClass = 'listbox-wrapper';
        if (className) {
            wrapperClass += ' ' + className;
        }
        var rows = items.map((item, index) => {
            const active = multiselect
                ? activeIndexes && (activeIndexes[index])
                : (index === activeIndex);
            var draggableProps = {};
            if (this.props.sortable) {
                draggableProps = {
                    draggable: true,
                    onDragEnd: this.dragEnd,
                    onDragStart: this.dragStart.bind(this, index),
                };
            }

            return (
                <div
                    className={'listbox-item' + (active ? ' active' : '')}
                    key={index}
                    data-id={index}
                    onMouseDown={this.handleClick.bind(this, index)}
                    onDoubleClick={this.handleDoubleClick}
                    {...draggableProps}
                >
                    {multiselect &&
                    <FormCheck
                        key="box"
                        className="listbox-item-checkbox"
                        inline
                        checked={checkedIndexes && checkedIndexes[index] || false}
                        onChange={(e) => this.selectedItemOperation(this.props.onCheck)}
                    />
                    }
                    {renderItemContent({item, active, index}, (e) => this.handleCheckItem(e, item, index))}
                </div>
            );
        });

        return (
            <div className="listbox">
                <Shortcuts ref="wrapper" name="ListBox" handler={this.handleShortcuts} tabIndex={0}
                           className={wrapperClass}>
                    <div className={cls} ref='container' onDragOver={this.dragOver}>
                        {rows}
                    </div>
                </Shortcuts>
                {multiselect &&
                <div className="listbox-selection">
                    <Button variant="default" onClick={this.handleCheckAll}>
                        Vybrat vše
                    </Button>
                    <div className="listbox-selection-count">
                        Vybráno: {checkedIndexes && Object.keys(checkedIndexes).length || 0}
                    </div>
                </div>
                }
            </div>
        );
    }
}

export default ListBox;
