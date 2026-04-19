import React from 'react';
import {AbstractReactComponent, Icon} from 'components/shared';
import classNames from 'classnames';
import './AdminExtSystemListItem.scss';

/**
 * Komponenta item externího systému
 */
class AdminExtSystemListItem extends AbstractReactComponent {
    render() {
        const {id, name, className} = this.props;

        let icon = 'fa-server';

        return (
            <div classID={id} className={classNames('ext-system-list-item', className)}>
                <div>
                    <Icon glyph={icon} />
                    <span className="name">{name}</span>
                </div>
            </div>
        );
    }
}

export default AdminExtSystemListItem;
