// --
import PropTypes from 'prop-types';

import React from 'react';
import classNames from 'classnames';
import i18n from "../../i18n";
import './HorizontalLoader.less';

/**
 * Loader pro načítání dat - horizontální, typicky pro dlouhé seznamy položek apod.
 */
export default class HorizontalLoader extends React.Component {

    static propTypes = {
        hover: PropTypes.bool, // pokud je true, jedná se o loader, který není jako standardní div, ale objeví se NAD ostatními elementami, např. pro využití v seznamech apod
        text: PropTypes.string,
        showText: PropTypes.bool,  // má se zobrazovat text
        rerenderProgress: PropTypes.bool,  // poukd je true, tak se po každém přerenderování změní progress tak, že jde od začátku
    };

    static defaultProps = {
        showText: true,
        rerenderProgress: false
    };

    constructor(props) {
        super(props);
        this.renderCount = 0;
    }

    render() {
        const {rerenderProgress, showText, hover, text, className, fetched, ...other} = this.props;

        let useText = text || i18n('global.data.loading');

        const clsCont = classNames(
            "loaderInf-container",
            className, {
            }
        );
        const wrapperCls = classNames(
            "loaderInf-container-wrapper", {
                hover
            }
        );

        if (rerenderProgress) {
            this.renderCount = ++this.renderCount % 10;
        }

        return (
            <div key={this.renderCount} className={wrapperCls} title={useText}>
                <div className="loaderInf-content">
                    <div className={clsCont}>
                        <div className="loaderInf" {...other}>
                        </div>
                    </div>
                    {showText && <div className="loading-info">
                        {useText}
                    </div>}
                </div>
            </div>
        );
    }
}
