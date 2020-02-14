/**
 * Data grid komponent - typu tabulka excel.
 */
import React from 'react';
import ReactDOM from 'react-dom';
import AbstractReactComponent from "../../AbstractReactComponent";
import * as Utils from "../../Utils";
import {validateInt, normalizeInt} from 'components/validate.jsx';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './DataGridPaginationKeymap.jsx';

import './DataGridPagination.scss';
import i18n from "../../i18n";

export function getPagesCount(itemsCount, pageSize) {
    let pagesCount = Math.floor(itemsCount / pageSize);
    if (itemsCount % pageSize > 0) {
        pagesCount++
    }
    return pagesCount
}

function getCurrPageDesc(pageIndex, pagesCount) {
    return (pageIndex + 1) + ' z ' + pagesCount
}

class DataGridPagination extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    UNSAFE_componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }
    constructor(props) {
        super(props);

        this.bindMethods('handleCurrPageFocus', 'handleCurrPageBlur', 'handleCurrPageChange',
            'processCurrPageChange', 'renderButton');

        const pagesCount = getPagesCount(props.itemsCount, props.pageSize)

        this.state = this.getStateFromProps(props, false)
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState(this.getStateFromProps(nextProps, this.state.focused))
    }

    getStateFromProps(props, focused) {
        if (focused) {
            return {
                currPageValue: props.pageIndex + 1
            }
        } else {
            const pagesCount = getPagesCount(props.itemsCount, props.pageSize)
            return {
                currPageValue: getCurrPageDesc(props.pageIndex, pagesCount)
            }
        }
    }

    handleCurrPageChange(e) {
        this.setState({
            currPageValue: normalizeInt(e.target.value)
        })
    }

    handleCurrPageFocus() {
        this.setState({
            currPageValue: this.props.pageIndex + 1,
            focused: true,
        })
    }

    processCurrPageChange(clearFocus) {
        const {onSetPageIndex, pageIndex, itemsCount, pageSize} = this.props
        var {currPageValue} = this.state
        currPageValue-- // převedeme na index
        const pagesCount = getPagesCount(itemsCount, pageSize)

        if (validateInt(currPageValue) === null) {   // je validní, ještě musí být ve správném intervalu
            if (currPageValue < 0) {
                if (pageIndex !== 0) {
                    if (clearFocus) {
                        this.setState({focused: false})
                    }

                    onSetPageIndex(0)
                    return true
                }
            } else if (currPageValue >= pagesCount) {
                if (pageIndex !== pagesCount - 1) {
                    if (clearFocus) {
                        this.setState({focused: false})
                    }

                    onSetPageIndex(pagesCount - 1)
                    return true
                }
            } else if (pageIndex !== currPageValue) {
                if (clearFocus) {
                    this.setState({focused: false})
                }

                onSetPageIndex(currPageValue)
                return true
            }
        }

        return false
    }

    handleCurrPageBlur() {
        const {pageIndex, itemsCount, pageSize} = this.props
        const pagesCount = getPagesCount(itemsCount, pageSize)

        if (this.processCurrPageChange(true)) {
        } else {
            this.setState({
                currPageValue: getCurrPageDesc(pageIndex, pagesCount),
                focused: false,
            })
        }
    }

    renderButton(disabed, onClick, content) {
        var cls = disabed ? 'disabled' : ''
        return <a className={cls} onClick={onClick}>{content}</a>
    }
    actionMap ={
        "CONFIRM": () => this.processCurrPageChange(false)
    }
    handleShortcuts = (action,e) => {
        e.stopPropagation();
        e.preventDefault();
        this.actionMap[action] && this.actionMap[action](e);
    }
    render() {
        const {onSetPageIndex, onChangePageSize, itemsCount, pageSize, pageIndex} = this.props;
        const pagesCount = getPagesCount(itemsCount, pageSize);

        const options = [25, 50, 100, 250].map(val => <option key={val} value={val}>{val}</option>);

        const cls = this.props.className ? 'pagination-container ' + this.props.className : 'pagination-container';
        return (
            <Shortcuts name="DataGridPagination" handler={this.handleShortcuts}>
                <nav className={cls} >
                    <ul className="pagination">
                        <li key='start'>{this.renderButton(pageIndex === 0, () => pageIndex > 0 && onSetPageIndex(0), '«')}</li>
                        <li key='prev'>{this.renderButton(pageIndex === 0, () => pageIndex > 0 && onSetPageIndex(pageIndex - 1), '‹')}</li>
                        <li key='goTo' className='input'><span>
                            <input
                                type='text' value={this.state.currPageValue}
                                onChange={this.handleCurrPageChange}
                                onFocus={this.handleCurrPageFocus}
                                onBlur={this.handleCurrPageBlur}
                            />
                        </span></li>
                        <li key='pageSize' className='input'><span><select value={pageSize} onChange={e => onChangePageSize(Number(e.target.value))}>{options}</select></span></li>
                        <li key='next'>{this.renderButton(pageIndex + 1 >= pagesCount, () => pageIndex + 1 < pagesCount && onSetPageIndex(pageIndex + 1), '›')}</li>
                        <li key='end'>{this.renderButton(pageIndex === pagesCount - 1, () => pageIndex < pagesCount - 1 && onSetPageIndex(pagesCount - 1), '»')}</li>
                        <li key='rowsCount'><p>{i18n('fund.grid.rowsCount', itemsCount)}</p></li>
                    </ul>
                </nav>
            </Shortcuts>
        )
    }
}

export default DataGridPagination;
