require("./Tags.less");

/**
 * Komponenta zobrazující seznam vybraných položek např. pomocí tag input s možností jejich odebírání pomocí křížku.
 */

import PropTypes from 'prop-types';

import React from 'react';
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Icon} from 'components/shared';

const Tags = class Tags extends AbstractReactComponent {
    constructor(props) {
        super(props);

    }

    render() {
        const {items, onRemove, renderItem} = this.props;

        return (
            <div className="selected-data-container">
                {items.map((item, index) => (
                    <div className="selected-data" key={index}>
                        <div className="data-label">
                          {renderItem({item})}
                        </div>
                        <Button onClick={() => {onRemove(item, index)}}>
                            <Icon glyph="fa-times"/>
                        </Button>
                    </div>))}
            </div>
        )
    }
}

Tags.propTypes = {
    items: PropTypes.array.isRequired,
    renderItem: PropTypes.func,
}
Tags.defaultProps = {
    renderItem: (item) => {
        return (
            <div>
                {item.name}
            </div>
        )
    }
}

export default Tags
