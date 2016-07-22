/**
 * Komponenta zobrazující seznam vybraných položek např. pomocí tag input s možností jejich odebírání pomocí křížku.
 */

require("./Tags.less")

import React from 'react';
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Icon} from 'components/index.jsx';

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
                            {renderItem(item)}
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
    items: React.PropTypes.array.isRequired,
    renderItem: React.PropTypes.func,
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

module.exports = Tags