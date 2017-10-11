// ---
import React from 'react';
import "./AdminRightsContainer.less";
import AbstractReactComponent from "../AbstractReactComponent";

/**
 * Wrapper pro levou a pravou část pro stránky editace oprávnění.
 * Levá část je užší a pravá zabírá zbytek šířky.
 * Na výšku je kontejner 100%.
 */
class AdminRightsContainer extends AbstractReactComponent {
    render() {
        const {className, header, left, children} = this.props;

        return <div className={"admin-rights-container " + (className ? " " + className : "")}>
            {header && <div className="admin-rights-header">
                {header}
            </div>}
            <div className="admin-rights-content">
                {left && <div className="admin-rights-left">
                    {left}
                </div>}
                <div className="admin-rights-right">
                    {children}
                </div>
            </div>
        </div>;
    }
}

export default AdminRightsContainer;
