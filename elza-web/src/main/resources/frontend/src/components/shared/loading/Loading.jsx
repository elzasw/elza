import React from 'react';

import './Loading.less';
import i18n from "../../i18n";

const Loading = (
    {
        value,
    },
) => {
    return <div className="loading">{value ? value : i18n('global.data.loading')}</div>;
};

export default Loading;
