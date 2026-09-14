// --
import PropTypes from 'prop-types';

import React from 'react';
import ListBox from './ListBox';
import AbstractReactComponent from '../../AbstractReactComponent';
import NoFocusButton from '../button/NoFocusButton';
import { injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';
import Icon from '../icon/Icon';
import './AddRemoveListBox.scss';

/**
 * Listbox s možností přidávat a odebírat položky.
 */

class AddRemoveListBox extends AbstractReactComponent {
    static propTypes = {
        items: PropTypes.array.isRequired,
        onAdd: PropTypes.func.isRequired,
        onRemove: PropTypes.func.isRequired,
        renderItemContent: PropTypes.func,
        addTitle: PropTypes.object,
        removeTitle: PropTypes.object,
        readOnly: PropTypes.bool,
        canDeleteItem: PropTypes.func,
    };

    static defaultProps = {
        readOnly: false,
        addTitle: globalMessages.add,
        removeTitle: globalMessages.remove,
        renderItemContent: (item, isActive, index, onCheckItem) => {
            return <div>{item.name}</div>;
        },
        canDeleteItem: (item, index) => true,
    };

    constructor(props) {
        super(props);

        this.state = {
            items: this.buildItems({}, props),
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState({
            items: this.buildItems(this.props, nextProps),
        });
    }

    buildItems = (props, nextProps) => {
        if (nextProps.items !== props.items) {
            const useItems = nextProps.items || [];
            const items = [
                ...useItems,
                {
                    _addItem: true,
                },
            ];
            return items;
        } else {
            return this.state.items;
        }
    };

    renderItem = (props, onCheckItem) => {
        const {readOnly, canDeleteItem, renderItemContent, addTitle, removeTitle, onRemove, onAdd} = this.props;
        const {item, index} = props;

        if (item._addItem) {
            return (
                <div className="arlb-plus-item">
                    {!readOnly && (
                        <NoFocusButton onClick={onAdd} title={this.props.intl.formatMessage(addTitle)}>
                            <Icon glyph="fa-plus" />
                        </NoFocusButton>
                    )}
                </div>
            );
        } else {
            return (
                <div className="arlb-item">
                    <div className="item-label">{renderItemContent(props, onCheckItem)}</div>
                    {!readOnly && canDeleteItem(item, index) && (
                        <NoFocusButton
                            className="item-action"
                            onClick={() => onRemove(item, index)}
                            title={this.props.intl.formatMessage(removeTitle)}
                        >
                            <Icon glyph="fa-remove" />
                        </NoFocusButton>
                    )}
                </div>
            );
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

        return (
            <ListBox
                className={'add-remove-listbox' + (className ? ' ' + className : '')}
                {...rest}
                items={this.state.items}
                renderItemContent={this.renderItem}
                canSelectItem={this.canSelectItem}
            />
        );
    }
}

export default injectIntl(AddRemoveListBox);
