import PropTypes from 'prop-types';
import React from 'react';
import {AbstractReactComponent, i18n, Icon} from 'components/shared';
import classNames from 'classnames'
import './AdminExtSystemListItem.less';

/**
 * Komponenta item externího systému
 */
class AdminExtSystemListItem extends AbstractReactComponent {

    static propTypes = {
        onClick: PropTypes.func,
        record: PropTypes.object.isRequired,
    };


    render() {
        const {id, name, code, className, ...otherProps} = this.props;

        let icon = "fa-server";

        return <div classID={id} className={classNames('ext-system-list-item', className)} {...otherProps}>
            <div>
                <Icon glyph={icon} />
                <span className="name">{name}</span>
            </div>
        </div>
    };
}

export default AdminExtSystemListItem;
