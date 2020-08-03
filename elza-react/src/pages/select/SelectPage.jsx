import React from 'react';

import {AbstractReactComponent, Icon, RibbonGroup} from 'components/shared';
import {Button} from '../../components/ui';

import './SelectPage.scss';

/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */
class SelectPage extends AbstractReactComponent {
    handleConfirm() {
        throw new Error('You have to override this method!!!');
    }

    handleClose = () => {
        this.props.onClose();
    };

    static renderTitles = titles => {
        let itemss = [];
        titles.forEach((i, index, self) => {
            itemss.push(<div key={index}>{i}</div>);
            index + 1 < self.length && itemss.push(<span key={index + '-spacer'}>&nbsp;>&nbsp;</span>);
        });
        return <div className="titles-header">{itemss}</div>;
    };

    getPageProps() {
        const {titles} = this.props;

        return {
            customRibbon: this.buildRibbonParts(),
            module: true,
            status: titles ? SelectPage.renderTitles(titles) : null,
        };
    }

    buildRibbonParts() {
        return {
            altActions: [],
            itemActions: [],
            primarySection: [
                <RibbonGroup key="ribbon-group-main" className="large big-icon">
                    <Button onClick={this.handleClose} className="cancel">
                        <Icon glyph="fa-times-circle" />
                    </Button>
                    <Button onClick={this.handleConfirm} className="confirm">
                        <Icon glyph="fa-check-circle" />
                    </Button>
                </RibbonGroup>,
            ],
        };
    }
}

export default SelectPage;
