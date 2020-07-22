import React from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import {Form} from 'react-bootstrap';
import AbstractReactComponent from 'components/AbstractReactComponent';
import * as Utils from 'components/Utils';
import Icon from 'components/shared/icon/Icon';
import {getBootstrapInputComponentInfo} from 'components/form/FormUtils.jsx';
import './Autocomplete.scss';
import {propsEquals} from 'components/Utils.jsx';
import {Shortcuts} from 'react-shortcuts';
import defaultKeymap from './AutocompleteKeymap.jsx';
import ListItem from 'components/shared/tree-list/list-item/ListItem.jsx';
import List from 'components/shared/tree-list/TreeList.jsx';
import flattenItems, {cleanItem, filterItems} from 'components/shared/utils/itemFilter.jsx';
import FloatingMenu from 'components/shared/floating-menu/FloatingMenu.jsx';
import classNames from 'classnames';
import i18n from 'components/i18n.jsx';

let _debugStates = false;

/**
 * Komponenta pro text input - definována pro překrytí a kontrolu shouldComponentUpdate.
 * Pokud se v autocomplete dovyplní a označí zbytek textu a input se překreslil,
 * zmizel daný text. Tato komponenta tomu zabrání - testuje změnu value.
 */
class TextInput extends AbstractReactComponent {
    shouldComponentUpdate(nextProps, nextState) {
        return !propsEquals(this.props, nextProps);
    }

    render() {
        return <input {...this.props} />;
    }
}

// toggleable button meant for dropdown menus
const DropdownButton = props => {
    const {toggled, ...otherProps} = props;
    let glyph = toggled ? 'fa-angle-up' : 'fa-angle-down';
    let className = classNames({
        opened: toggled,
        closed: !toggled,
    });

    return <SimpleButton className={className} glyph={glyph} {...otherProps} />;
};

// simple button component with icon
const SimpleButton = props => {
    const {disabled, onMouseDown, glyph, onMouseUp} = props;
    let className = classNames({
        btn: true,
        'btn-default': true,
        [props.className]: props.className,
        disabled: disabled,
    });

    return (
        <div
            disabled={disabled}
            className={className}
            onMouseDown={() => {
                if (!disabled) {
                    onMouseDown && onMouseDown();
                }
            }}
            onMouseUp={() => {
                if (!disabled) {
                    onMouseUp && onMouseUp();
                }
            }}
        >
            <Icon glyph={glyph} />
        </div>
    );
};

export default class Autocomplete extends AbstractReactComponent {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    static propTypes = {
        value: PropTypes.oneOfType([PropTypes.object, PropTypes.string, PropTypes.number]),
        onFocus: PropTypes.func,
        onBlur: PropTypes.func,
        onSearchChange: PropTypes.func,
        onChange: PropTypes.func,
        renderItem: PropTypes.func,
        getItemRenderClass: PropTypes.func, // načtení doplňující class pro položku - aby nebylo nutné měnit kvůli class celý renderItem
        alwaysExpanded: PropTypes.bool,
        inputProps: PropTypes.object,
        actions: PropTypes.array,
        tags: PropTypes.bool,
        inline: PropTypes.bool,
        touched: PropTypes.bool,
        error: PropTypes.string,
        allowSelectItem: PropTypes.func, // vrací true, pokud je možné řádek vybrat jako hodnotu
        allowFocusItem: PropTypes.func, // vrací true, pokud se na řádek dá najet focusem, např. přes klávesnici
        getItemId: PropTypes.func,
        getItemName: PropTypes.func,
        useIdAsValue: PropTypes.bool, // pokud je true, pracuje navenek komponenta tak, že jeko hodnotu nemá objekt, ale jeho id - stejně jako html select
        placeholder: PropTypes.string,
    };

    static defaultProps = {
        useIdAsValue: false,
        value: {},
        onSearchChange(text) {},
        onChange(item) {},
        renderItem: props => {
            return <ListItem {...props} />;
        },
        getItemRenderClass: item => null,
        alwaysExpanded: false,
        inputProps: {},
        inline: false,
        touched: false,
        error: null,
        allowSelectItem(item) {
            return true;
        }, // vrati true, když může být daná položka vybrána
        allowFocusItem(item) {
            return true;
        }, // vrati true, když může na danou položku najet klávesnicí nebo myší a může být focusovatelná
        getItemId: item => (item ? item.id : null),
        getItemName: item => (item ? item.name : ''),
        itemFilter: (filterText, items, props) => {
            return filterItems(filterText, items, props);
        },
        actions: [],
        selectOnlyValue: false,
    };

    defaultState = {
        inputStrValue: '',
        isOpen: false,
        hasFocus: false,
        changed: false,
        items: {ids: []},
        value: {},
    };

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    constructor(props) {
        super(props);

        this.itemCount = 0;
        this._ignoreBlur = false;
        this.state = {
            ...this.defaultState,
            ...this.getStateFromProps({}, props, {inputStrValue: '', changed: false, items: []}),
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const newState = this.getStateFromProps(this.props, nextProps, this.state);
        this.setState(newState);
    }

    getStateFromProps = (props, nextProps, state) => {
        let shouldItemRender;
        let changed = state.changed;
        if (nextProps.shouldItemRender) {
            shouldItemRender = nextProps.shouldItemRender;
        } else {
            shouldItemRender = (item, value) => {
                return (
                    nextProps
                        .getItemName(item)
                        .toLowerCase()
                        .indexOf(value.toLowerCase()) !== -1
                );
            };
        }

        // ---
        let inputStrValue;
        const prevRealValue = this.getRealValue(props.value, props);
        const nextRealValue = this.getRealValue(nextProps.value, nextProps);
        const prevId = props.getItemId ? props.getItemId(prevRealValue) : nextProps.getItemId(prevRealValue);
        const newId = nextProps.getItemId(nextRealValue);
        _debugStates && console.log('getStateFromProps', 'prevId', prevId, 'newId', newId, 'state', state);
        if (prevId != newId) {
            // Změna stavu, při které byla vybrána jiná položka.
            changed = false;
            inputStrValue = nextProps.getItemName(nextRealValue);
            if (typeof inputStrValue === 'undefined') {
                inputStrValue = '';
            }
        } else if (prevId === newId && !state.changed) {
            /* pokud došlo ke změně stavu, při které zůstala vybrána stejná položka
             a nebyl změněn vyhledávaný text ručně.*/
            inputStrValue = nextProps.getItemName(nextRealValue);
        } else {
            inputStrValue = state.inputStrValue;
        }

        // ---

        const result = {
            shouldItemRender: shouldItemRender,
            value: nextRealValue,
            inputStrValue: inputStrValue,
            changed: changed,
            items: this.flattenItems(nextProps),
        };

        _debugStates && console.log('getStateFromProps', result);

        return result;
    };

    getRealValue = (value, props = this.props) => {
        const {useIdAsValue} = props;
        if (!useIdAsValue) {
            return value;
        }

        if (value == null || value == '') {
            return value;
        }

        const {getItemId} = props;
        for (let a = 0; a < props.items.length; a++) {
            const i = props.items[a];
            if (getItemId(i) === value) {
                return i;
            }
        }

        return null;
    };

    focus = () => {
        const {inputStrValue} = this.state;
        const input = ReactDOM.findDOMNode(this.input);

        input.focus();
        // select whole input value
        if (inputStrValue) {
            input.setSelectionRange(0, inputStrValue.length);
        }
    };

    blur = () => {
        const input = ReactDOM.findDOMNode(this.input);
        input.blur();
    };

    componentDidMount = () => {
        this.props.selectOnlyValue && this.selectOnlyValue();
    };

    /*
    componentWillUpdate = (nextProps, nextState) => {
        console.log("### AC will update", nextState.hasFocus);
    }
     */

    selectOnlyValue = () => {
        const {items} = this.state;
        // filter result has only one selectable item
        if (items.filteredCount === 1) {
            // select the only selectable item
            this.selectItem(items[items.firstItemId]);
        }
    };

    // converts the items from given props to the proper format (deep hierarchy tree to flat tree)
    flattenItems = props => {
        const {selectOnlyValue, allowSelectItem, getItemName, items, getItemId} = props;

        let result = flattenItems(items, {getItemId});

        // get info about item availability
        // (only when custom filter is not used and we need the info to pre-select value)
        if (!props.customFilter && props.selectOnlyValue) {
            result = props.itemFilter('', result, {allowSelectItem, getItemName});
        }

        return result;
    };

    UNSAFE_componentWillMount = () => {
        Utils.addShortcutManager(this, defaultKeymap);
    };

    handleChange = event => {
        const {onSearchChange} = this.props;
        const value = event.target.value;
        const prevValue = this.state.inputStrValue;

        this.setState({inputStrValue: value, changed: true});
        // input value changed > number of items may have changed > recalculate floating menu size
        this.recalculateFloatingMenu();
        this.openMenu();

        onSearchChange && onSearchChange(value);
    };

    handleInputBlur = e => {
        const {value} = this.state;

        // ignore the blur event if set and reset the ignoreBlur value
        if (this._ignoreBlur) {
            this._ignoreBlur = false;
            this.focus();
            return false;
        }

        this.setState({hasFocus: false});
        this.callOnBlur(value);
        this.closeMenu();
        return true;
    };

    handleInputFocus = () => {
        const {onFocus} = this.props;
        if (this.state.hasFocus) {
            return false;
        }
        this.setState({hasFocus: true});
        onFocus && onFocus();

        return true;
    };

    openMenu = () => {
        const {isOpen} = this.state;

        if (isOpen) {
            return;
        }

        this.setState({
            isOpen: true,
        });
    };

    closeMenu = () => {
        const {getItemName, value} = this.props;
        const {isOpen} = this.state;

        if (isOpen) {
            this.setState({
                isOpen: false,
                inputStrValue: getItemName(value) || '', // reset input value
                changed: false,
            });
        }
    };
    handleEmptySelect = () => {
        const {onEmptySelect} = this.props;
        const {inputStrValue} = this.state;

        onEmptySelect && onEmptySelect(inputStrValue);
    };

    callOnChange = value => {
        const {onChange, getItemId, useIdAsValue} = this.props;
        if (useIdAsValue) {
            onChange(getItemId(value));
        } else {
            onChange(value);
        }
    };

    callOnBlur = value => {
        const {onBlur, getItemId, useIdAsValue} = this.props;
        if (onBlur) {
            if (useIdAsValue) {
                onBlur(getItemId(value));
            } else {
                onBlur(value);
            }
        }
    };

    selectItem = item => {
        const {getItemName, getItemId, allowSelectItem, tags} = this.props;
        const {isOpen, changed, inputStrValue} = this.state;

        // delete redundant props from item
        item && cleanItem(item);

        let newState = {
            inputStrValue: '',
            value: {},
            isOpen: false,
            changed: false,
        };

        if (true || isOpen || changed) {
            if (!item) {
                this.handleEmptySelect();
            }
            if (tags) {
                if (!item) {
                    item = {
                        name: inputStrValue,
                    };
                }
                this.setState(newState, () => {
                    this.callOnChange(item);
                });
            } else {
                if (!item) {
                    // hit enter after focus but before typing anything
                    // so no autocomplete attempt yet
                    this.setState(newState, () => {
                        ReactDOM.findDOMNode(this.input).select();
                        this.callOnChange(null);
                    });
                } else {
                    const name = getItemName(item);
                    this.setState(
                        {
                            ...newState,
                            inputStrValue: name,
                            value: item,
                        },
                        () => {
                            this.callOnChange(item);
                        },
                    );
                }
            }
        }
    };
    actionMap = {
        MOVE_UP: e => {
            e.preventDefault();
            this.list && this.list.selectorMoveUp();
        },
        MOVE_DOWN: e => {
            e.preventDefault();
            this.list && this.list.selectorMoveDown();
        },
        MOVE_TO_PARENT_OR_CLOSE: e => {
            e.preventDefault();
            this.list && this.list.selectorMoveToParentOrClose();
        },
        MOVE_TO_CHILD_OR_OPEN: e => {
            e.preventDefault();
            this.list && this.list.selectorMoveToChildOrOpen();
        },
        SELECT_ITEM: e => {
            if (this.list && this.state.isOpen) {
                e.preventDefault();
                this.list.selectHighlightedItem();
            }
        },
        OPEN_MENU: e => {
            e.preventDefault();
            !this.props.customFilter && this.openMenu();
        },
        CLOSE_MENU: e => {
            if (this.state.isOpen) {
                e.preventDefault();
                this.closeMenu();
            }
        },
    };
    handleShortcuts = (action, e) => {
        this.actionMap[action](e);
    };

    recalculateFloatingMenu = () => {
        this.menu && this.menu.setMenuPositions();
    };

    renderMenu() {
        const {items, changed, value} = this.state;
        const {
            header,
            footer,
            allowSelectItem,
            allowFocusItem,
            favoriteItems,
            renderItem,
            itemFilter,
            customFilter,
            alwaysExpanded,
        } = this.props;
        const filterText = this.state.inputStrValue;
        let filterActive = false;
        let selectedItemId = value && value.id;
        let highlightedItemId = selectedItemId || (items ? items.ids[0] : null); // if highlighted item does not exist, highlights first value
        let otherProps = {};
        let filteredItems = items;

        if (!customFilter && filterText && filterText.length > 0 && changed && items) {
            filteredItems = itemFilter(this.state.inputStrValue, items, {allowSelectItem});
            highlightedItemId = filteredItems.firstItemId && filteredItems.firstItemId;
            filterActive = true;
        }

        let menuShouldUpdate = false;
        // if the last saved item count is not the same as the new item count
        // the floating menu shoud be updated
        if (items && this.itemCount !== filteredItems.ids.length) {
            menuShouldUpdate = true;
            this.itemCount = filteredItems.ids.length; // save the new item count
        }

        // add the favorites group if it exists and the input value
        // wasn't changed (the items are not filtered)
        if (favoriteItems && favoriteItems.length > 0 && !changed) {
            otherProps.groups = [
                {
                    name: 'fav',
                    title: i18n('autocomplete.list.favoriteItems'),
                    hideWhenEmpty: true,
                    hideTitle: false,
                    ignoreDepth: true,
                    ids: favoriteItems,
                },
                {
                    name: 'all',
                    title: i18n('autocomplete.list.allItems'),
                    hideTitle: false,
                    priority: 1,
                },
            ];
        }
        let headerComp;
        if (header) {
            headerComp = (
                <div ref="autocompleteHeader" className="autocomplete-menu-header">
                    {header}
                </div>
            );
        }
        let footerComp;
        if (footer) {
            footerComp = (
                <div ref="autocompleteFooter" className="autocomplete-menu-footer">
                    {footer}
                </div>
            );
        }

        return (
            <FloatingMenu
                target={this.wrap}
                closeMenu={this.handleInputBlur}
                ref={menu => {
                    this.menu = menu;
                }}
                onMouseDown={() => {
                    this.focus();
                    this._ignoreBlur = true;
                }}
                onMouseUp={() => {
                    this.focus();
                }}
                //shouldUpdate={menuShouldUpdate}
                // sets ignoreBlur to true to ignore the next input blur when the menu is clicked
            >
                {headerComp}
                <List
                    onContentChange={this.recalculateFloatingMenu}
                    items={filteredItems}
                    onChange={this.selectItem}
                    allowSelectItem={allowSelectItem}
                    allowFocusItem={allowFocusItem}
                    expandAll={filterActive || alwaysExpanded}
                    selectedItemId={selectedItemId}
                    highlightedItemId={highlightedItemId}
                    ref={list => {
                        this.list = list;
                    }}
                    //disableShortcuts
                    renderItem={renderItem}
                    includeId={selectedItemId}
                    {...otherProps}
                />
                {footerComp}
            </FloatingMenu>
        );
    }

    buildMainClass() {
        const {error, touched, className} = this.props;
        const hasError = touched && error;

        let newClassName = classNames({
            'autocomplete-control-container': true,
            'form-group': true,
            'has-error': hasError,
            'is-invalid': hasError,
            [className]: className,
        });

        return newClassName;
    }

    buildWrapperClass() {
        const {error, touched} = this.props;
        const {hasFocus} = this.state;
        const hasError = touched && error;

        return classNames({
            'autocomplete-input-container': true,
            'form-control': true,
            'has-error': hasError,
            'is-invalid': hasError,
            active: hasFocus,
        });
    }

    // renders the additional actions next to the autocomplete input
    renderActions() {
        const {actions, customFilter, disabled} = this.props;
        let renderedActions = [...actions];
        // add the dropdown button to open menu if custom filter is not set
        if (!customFilter) {
            renderedActions.push(
                <DropdownButton
                    key="open"
                    toggled={this.state.isOpen}
                    disabled={disabled}
                    onMouseDown={() => {
                        this._ignoreBlur = true;
                        this.handleInputFocus();
                        this.state.isOpen ? this.closeMenu() : this.openMenu();
                    }}
                    onMouseUp={() => {
                        this.focus();
                    }}
                />,
            );
        }

        return renderedActions;
    }

    render() {
        _debugStates && console.log('RENDER', 'props', this.props, 'state', this.state);
        const {customFilter, error, title, touched, inline} = this.props;
        const {inputStrValue, hasFocus} = this.state;

        const hasError = touched && error;
        let inlineProps = {};
        if (inline) {
            error && (inlineProps.title = title ? title + '/' + error : error);
        }

        const bootInfo = getBootstrapInputComponentInfo(this.props);
        const cls = bootInfo.cls;

        return (
            <Shortcuts
                handler={this.handleShortcuts}
                name="Autocomplete"
                className={this.buildMainClass()}
                stopPropagation={this.state.isOpen}
            >
                <div>
                    {this.props.label && <label className="control-label">{this.props.label}</label>}
                    <div
                        key="inputWrapper"
                        className={this.buildWrapperClass()}
                        ref={wrap => {
                            this.wrap = wrap;
                        }}
                    >
                        <TextInput
                            ref={input => {
                                this.input = input;
                            }}
                            key="input"
                            className={'input'}
                            type="text"
                            onFocus={this.handleInputFocus}
                            //onFocusout={(e)=>{console.log("### onfocus out", document.activeElement, e);}}
                            onBlur={this.handleInputBlur}
                            {...inlineProps}
                            {...this.props.inputProps}
                            label={this.props.label}
                            disabled={this.props.disabled}
                            //role='combobox'
                            aria-autocomplete="both"
                            autoComplete="off"
                            onChange={this.handleChange}
                            value={inputStrValue || ''}
                            placeholder={this.props.placeholder}
                        />
                        <div ref={ref => (this.actionsRef = ref)} className="actions">
                            {this.renderActions()}
                        </div>
                    </div>
                    {this.state.isOpen && this.renderMenu()}
                    {this.props.hasFeedback && (
                        <span className={'glyphicon form-control-feedback glyphicon-' + bootInfo.feedbackIcon}></span>
                    )}
                    {this.props.help && <span className="help-block">{this.props.help}</span>}
                    {!inline && hasError && <Form.Control.Feedback type={'invalid'}>{error}</Form.Control.Feedback>}
                </div>
            </Shortcuts>
        );
    }
}
