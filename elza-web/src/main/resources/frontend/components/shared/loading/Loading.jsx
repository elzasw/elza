import React from 'react';

import './Loading.less';
import i18n from "../../i18n";

class Loading extends React.Component {
    render() {
        return <div className="loading">{this.props.value ? this.props.value : i18n('global.data.loading')}</div>
    }
}

export default Loading;
