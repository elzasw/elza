import React from 'react';
import {i18n} from 'components/index.jsx';

require ('./Loading.less');

var Loading = class Loading extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        var text = this.props.value ? this.props.value : i18n('global.data.loading');
        return (
            <div className="loading">{text}</div>
        )
    }
}

export default Loading;
