// --
import React from 'react';
import ListBox from "./ListBox";
import AbstractReactComponent from "../../AbstractReactComponent";
import NoFocusButton from "../button/NoFocusButton";
import i18n from "../../i18n";
import Icon from "../icon/Icon";
import "./AddRemoveListBox.less";

/**
 * Listbox s možností přidávat a odebírat položky.
 */

class AddRemoveListBox extends AbstractReactComponent {
    static propTypes = {
        items: React.PropTypes.array.isRequired,
        onAdd: React.PropTypes.func.isRequired,
        onRemove: React.PropTypes.func.isRequired,
        renderItemContent: React.PropTypes.func,
        addTitle: React.PropTypes.string,
        removeTitle: React.PropTypes.string,
        readOnly: React.PropTypes.bool,
        canDeleteItem: React.PropTypes.func,
    };

    static defaultProps = {
        readOnly: false,
        addTitle: "global.action.add",
        removeTitle: "global.action.remove",
        renderItemContent: (item, isActive, index, onCheckItem) => {
            return (
                <div>{item.name}</div>
            )
        },
        canDeleteItem: (item, index) => true
    };

    constructor(props) {
        super(props);

        this.state = {
            items: this.buildItems({}, props)
        };
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            items: this.buildItems(this.props, nextProps)
        });
    }

    buildItems = (props, nextProps) => {
        if (nextProps.items !== props.items) {
            const useItems = nextProps.items || [];
            const items = [...useItems, {
                _addItem: true,
            }];
            return items;
        } else {
            return this.state.items;
        }
    };

    renderItem = (props, onCheckItem) => {
        const {readOnly, canDeleteItem, renderItemContent, addTitle, removeTitle, onRemove, onAdd} = this.props;
        const {item, index} = props; 

        if (item._addItem) {
            return <div className="arlb-plus-item">
                {!readOnly && <NoFocusButton onClick={onAdd} title={i18n(addTitle)}>
                    <Icon glyph="fa-plus"/>
                </NoFocusButton>}
            </div>;
        } else {
            return <div className="arlb-item">
                <div className="item-label">
                    {renderItemContent(props, onCheckItem)}
                </div>
                {!readOnly && canDeleteItem(item, index) && <NoFocusButton className="item-action" onClick={() => onRemove(item, index)} title={i18n(removeTitle)}>
                    <Icon glyph="fa-remove"/>
                </NoFocusButton>}
            </div>;
        }
    };

    canSelectItem = (item, index) => {
        const {canSelectItem} = this.props;
        if (!item._addItem && (!canSelectItem || canSelectItem(item, index))) {
            return true;
        } else {
            return false;
        }
    };

    render() {
        const {className, canSelectItem, renderItemContent, items, ...rest} = this.props;

        return <ListBox
            className={"add-remove-listbox" + (className ? " " + className : "")}
            {...rest}
            items={this.state.items}
            renderItemContent={this.renderItem}
            canSelectItem={this.canSelectItem}
        />
    }
}


export default AddRemoveListBox;
