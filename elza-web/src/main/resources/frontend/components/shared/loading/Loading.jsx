import React from 'react';
import {i18n} from 'components';

var Loading = class Loading extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div>{i18n('global.data.loading')}</div>
        )
    }
}

module.exports = Loading;