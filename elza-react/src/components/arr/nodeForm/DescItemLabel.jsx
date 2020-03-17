import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import classNames from 'classnames';
import './DescItemLabel.scss';

class DescItemLabel extends AbstractReactComponent {
    render() {
        const {value, onClick, cal, notIdentified} = this.props;

        let cls = ['desc-item-label-value'];
        if (cal) {
            cls.push('calculable');
        }

        // Sestavení hodnoty - změna znaku < na entitu, nahrazení enterů <br/>
        var updatedValue = value ? ('' + value).replace(/</g, '&lt;').replace(/(?:\r\n|\r|\n)/g, '<br />') : '';

        let renderItem;
        if (onClick == null) {
            renderItem = <div dangerouslySetInnerHTML={{__html: updatedValue}}></div>;
        } else {
            // eslint-disable-next-line jsx-a11y/anchor-is-valid
            renderItem = (
                <a style={{cursor: 'pointer'}} onClick={onClick} dangerouslySetInnerHTML={{__html: updatedValue}}></a>
            );
        }

        if (notIdentified) {
            renderItem = <i>{i18n('subNodeForm.descItemType.notIdentified')}</i>;
        }

        return (
            <div title={value} className={classNames(cls)}>
                {renderItem}
            </div>
        );
    }
}

export default DescItemLabel;
