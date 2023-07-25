import { Api } from 'api';
import { i18n } from 'components';
import { AdminInfo } from 'elza-api';
import React, { useEffect, useState } from 'react';
import './Statistics.scss';

export const StatsSimple = () => {
    const [stats, setStats] = useState<AdminInfo>();

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            setStats(data);
        });
    }, []);

    if (stats == undefined) {
        return <></>;
    }

    return (
        <div className="stats-flex-container">
            <div className="stats-box">
                <div>
                    <span>{i18n('stats.funds.title')}:</span> {stats.funds}
                </div>
                <div>
                    <span>{i18n('stats.levels.title')}:</span> {stats.levels}
                </div>
                <div>
                    <span>{i18n('stats.accessPoints.title')}:</span> {stats.accessPoints}
                </div>
            </div>
        </div>
    );
};

export const StatsAdmin = () => {
    const [stats, setStats] = useState<AdminInfo>();

    useEffect(() => {
        Api.admin.adminInfo({ overrideErrorHandler: true }).then(({ data }) => {
            setStats(data);
        });
    }, []);

    if (stats == undefined) {
        return <></>;
    }

    return (
        <div className="stats-box">
            <div>
                <span>{i18n('stats.funds.title')}:</span> {stats.funds}
            </div>
            <div>
                <span>{i18n('stats.levels.title')}:</span> {stats.levels}
            </div>
            <div>
                <span>{i18n('stats.accessPoints.title')}:</span> {stats.accessPoints}
            </div>
            <div>
                <span>{i18n('stats.users.title')}:</span> {stats.users}
            </div>
            <div>
                <span>{i18n('stats.loggedUsers.title')}:</span> {stats.loggedUsers}
            </div>
        </div>
    );
};
