import React from 'react';

import './Loading.scss';
import i18n from "../../i18n";

const Loading = (
    {
        value = null,
    },
) => {
    return <div className="loading">{value ? value : i18n('global.data.loading')}</div>;
};

export default Loading;
