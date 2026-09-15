import PropTypes from 'prop-types';
import React from 'react';

import './AddRemoveList.scss';
import AbstractReactComponent from '../../AbstractReactComponent';
import NoFocusButton from '../button/NoFocusButton';
import Icon from '../icon/Icon';
import { injectIntl } from 'react-intl';
import { globalMessages } from 'components/shared/lang/messages';

class AddRemoveList extends AbstractReactComponent {
    static propTypes = {
        items: PropTypes.array.isRequired,
        label: PropTypes.node, // pokud je uvedeno, zobrazí se jako nadpis celé sekce
        addInLabel: PropTypes.bool, // pokud je true, je akce přidání zobrazena u labelu - tedy nahoře
        onAdd: PropTypes.func,
        onRemove: PropTypes.func.isRequired,
        renderItem: PropTypes.func.isRequired,
        addTitle: PropTypes.object,
        addLabel: PropTypes.object,
        removeTitle: PropTypes.object,
        readOnly: PropTypes.bool.isRequired,
    };

    static defaultProps = {
        addTitle: globalMessages.add,
        removeTitle: globalMessages.remove,
        readOnly: false,
        renderItem: props => <div key={'rendered-item-' + props.index}>{props.item.name}</div>,
    };

    handleRemove = (item, index) => {
        const {onRemove} = this.props;
        onRemove(item, index);
    };

    render() {
        const {
            addInLabel,
            label,
            items,
            readOnly,
            className,
            onAdd,
            renderItem,
            addTitle,
            removeTitle,
            addLabel,
            intl,
        } = this.props;

        const groups =
            items == null
                ? []
                : items.map((item, index) => {
                      return (
                          <div className="item-container" key={'item-' + index}>
                              {renderItem({item, index})}
                              {!readOnly && onAdd && (
                                  <div className="item-actions-container">
                                      <NoFocusButton
                                          className="remove"
                                          onClick={this.handleRemove.bind(this, item, index)}
                                          title={intl.formatMessage(removeTitle)}
                                      >
                                          <Icon glyph="fa-remove" />
                                      </NoFocusButton>
                                  </div>
                              )}
                          </div>
                      );
                  });

        let addAction;
        if (!readOnly && onAdd) {
            addAction = (
                <div className="actions-container">
                    <NoFocusButton onClick={onAdd} title={intl.formatMessage(addTitle)}>
                        <Icon glyph="fa-plus" /> {addLabel && intl.formatMessage(addLabel)}
                    </NoFocusButton>
                </div>
            );
        }

        return (
            <div className={className ? 'list-add-remove-container ' + className : 'list-add-remove-container'}>
                {(label || addInLabel) && (
                    <div className="top-label">
                        <div className="list-label">{label}</div>
                        <div className="list-action">{addInLabel && addAction}</div>
                    </div>
                )}
                <div className="item-list-container">{groups}</div>
                {!addInLabel && addAction}
            </div>
        );
    }
}

export default injectIntl(AddRemoveList);
