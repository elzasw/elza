import React from 'react';
import AbstractReactComponent from "../../AbstractReactComponent";
import "./DetailHeader.less";
// ---

/**
 * Komponenta pro zobrazení hlavičky na detail stránce.
 * Hlavička se skládá z nepovinné ikony, nadpisu a dalších údajů (jako children, např. popis).
 */
export default class DetailHeader extends AbstractReactComponent {
    render() {
        const {icon, title, rowFlag, rowFlagColor, children} = this.props;
console.log(4444, children)
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
                        {children ? children : "-"}
                    </div>
                </div>
            </div>
            <div className={`detail-header-bottom dh-color-${rowFlagColor}`}>
                {rowFlag}
            </div>
        </div>
    }
}
