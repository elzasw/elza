// --

/**
 * Field který zaobaluje našeptávací field a reprezentuje jej jako tag field s možností vybrat více záznamů.
 */
// ---
import PropTypes from 'prop-types';

import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import getMapFromList from '../shared/utils/getMapFromList';
import Icon from './shared/icon/Icon';
import {Button} from './ui';

/**
 * Field pro vybrání skupiny nebo uživatele.
 */
class TagsField extends AbstractReactComponent {
    static defaultProps = {
        itemIdName: 'id',
        renderTagItem: x => x.name,
    };

    static propTypes = {
        itemIdName: PropTypes.func,    // název atributu s idčkem pro porovnání dvou položek, stdanrdně "id"
        renderTagItem: PropTypes.func, // funkce pro renderování názvu položky v tag input, standardně se renderuje atribut name, předpis: x => x.name
    };

    constructor(props) {
        super(props);
    }

    getFieldValue = () => {
        const {value} = this.props;
        return Array.isArray(value) ? value : [];
    };

    handleChange = (value) => {
        const {onChange, itemIdName} = this.props;

        if (onChange) {
            const useValue = this.getFieldValue();
            const itemsMap = getMapFromList(useValue, itemIdName);
            if (!itemsMap[value[itemIdName]]) {
                onChange([
                    ...useValue,
                    value,
                ]);
            }
        }
    };
    handleBlur = (value) => {
        const {onBlur} = this.props;
        onBlur && onBlur(this.getFieldValue());
    };

    handleRemove = (index) => {
        const {onChange} = this.props;

        if (onChange) {
            const useValue = this.getFieldValue();

            const newValue = [
                ...useValue.slice(0, index),
                ...useValue.slice(index + 1),
            ];
            onChange(newValue);
        }
    };

    render() {
        const {renderTagItem, fieldComponent, fieldComponentProps, error, label, touched, ...otherProps} = this.props;

        const {onChange, onBlur, onFocus, onUpdate, onDragStart} = otherProps;

        const field = React.createElement(fieldComponent, {
            tags: true,
            ...fieldComponentProps,
            ...{onFocus, onUpdate, onDragStart},
            onChange: this.handleChange,
            onBlur: this.handleBlur,
        });

        const fieldValue = this.getFieldValue();

        return (
            <div>
                {field}
                <div className="selected-data-container">
                    {fieldValue.map((item, index) => {
                        return <div className="selected-data" key={index}>
                            <span>{renderTagItem(item)}</span><Button onClick={() => {
                            this.handleRemove(index);
                        }}>
                            <Icon glyph="fa-times"/>
                        </Button>
                        </div>;
                    })}
                </div>
            </div>
        );
    }
}

export default TagsField;
