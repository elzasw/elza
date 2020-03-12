import React from 'react';
import AbstractReactComponent from '../../AbstractReactComponent';
import './DetailHeader.scss';
// ---

/**
 * Komponenta pro zobrazení hlavičky na detail stránce.
 * Hlavička se skládá z nepovinné ikony, nadpisu a dalších údajů (jako children, např. popis).
 */
export default class DetailHeader extends AbstractReactComponent {
    render() {
        const {icon, title, flagLeft, flagRight, rowFlagColor, subtitle} = this.props;
        let _flagColor = rowFlagColor ? `dh-color-${rowFlagColor}` : "";
        return <div className="detail-header-container">
            <div className="detail-header-main">
                {icon && <dic className="detail-header-icon">
                    {icon}
                </dic>}
                <div className="detail-header-content">
                    <div className="detail-header-title">
                        {title}
                    </div>
                    <div className="detail-header-desc">
                        {subtitle || "\u00a0"}
                    </div>
                </div>
            </div>
            <div className={`detail-header-bottom ${_flagColor}`}>
                {flagLeft}
                {flagRight && <span className="flag-right">
                    {flagRight}
                </span>}
            </div>
        </div>
    }
}
