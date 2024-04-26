// --
import React, { useEffect } from 'react';
import { useSelector } from 'react-redux';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as aipActions from '../../actions/aip/aip';
import { HorizontalLoader } from '../shared/index';
import './AipDetail.scss';
import { AppState } from 'typings/store';
import { useThunkDispatch } from 'utils/hooks';
import {dateToDateTimeString} from "../../shared/utils/commons";

export function AipDetail() {

    const aip = useSelector((state: AppState) => storeFromArea(state, aipActions.AREA_AIP))
    const dispatch = useThunkDispatch();

    function fetchData() {
        dispatch(aipActions.aipFetchIfNeeded(aip.id));
    }

    useEffect(() => {
        fetchData()
    }, [aip.id])

    if (!aip.fetched || aip.isFetching) {
        return <HorizontalLoader />;
    }

    return (
        <div className="detail-container">
            <h1>
                <span className="text">{aip.data.code}</span>
            </h1>
            <span className="detail-header">
                <span className="ext-id">id: {aip.data.aipId}</span>
                <span className="state">{aip.data.aipType}</span>
                <span className="date">Vytvořeno: {dateToDateTimeString(new Date(aip.data.createDate))}</span>
                <span className="date">Poslední změna: {dateToDateTimeString(new Date(aip.data.lastChange))}</span>
            </span>
            <div className="detail-body">
                <tr>
                    <span className="text">Verze: {aip.data.aipVersion}</span>
                </tr>
                <tr>
                    <span className="text">Velikost: {aip.data.aipSize}</span>
                </tr>
                <tr>
                    <button className="button-download">Stáhnout soubory</button>
                </tr>
            </div>
        </div>
    );
}

export default AipDetail;
