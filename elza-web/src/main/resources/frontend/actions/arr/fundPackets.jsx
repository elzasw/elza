import {WebApi} from 'actions'
import * as types from 'actions/constants/ActionTypes';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {modalDialogHide} from 'actions/global/modalDialog'

export function isFundPacketsAction(action) {
    switch (action.type) {
        case types.FUND_PACKETS_REQUEST:
        case types.FUND_PACKETS_RECEIVE:
        case types.FUND_PACKETS_FILTER:
        case types.FUND_PACKETS_CHANGE_SELECTION:
            return true
        default:
            return false
    }
}

function _dataGridKey(state) {
    var str = ''
    str += '-filterPrefix' + state.filterPrefix
    str += '-filterState' + state.filterState
    return str
}

export function fetchFundPacketsIfNeeded(versionId, fundId) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId')
        if (!fund) {
            return
        }

        const fundPackets = fund.fundPackets
        const dataKey = _dataGridKey(fundPackets)
        if (fundPackets.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey))

            WebApi.findPackets(fundId, fundPackets.filterState, fundPackets.filterText)
            .then(packets => {
                const newState = getState();
                const newFund = objectById(state.arrRegion.funds, versionId, 'versionId')
                if (newFund !== null) {
                    const newFundPackets = fund.fundPackets
                    const newDataKey = _dataGridKey(newFundPackets)
                    if (newDataKey === dataKey) {
                        dispatch(_dataReceive(versionId, packets))
                    }
                }
            })
        }
    }
}

function _dataRequest(versionId, dataKey) {
    return {
        type: types.FUND_PACKETS_REQUEST,
        versionId,
        dataKey,
    }
}

function _dataReceive(versionId, packets) {
    return {
        type: types.FUND_PACKETS_RECEIVE,
        versionId,
        packets,
    }
}

export function fundPacketsFilterByText(versionId, filterText) {
    return {
        type: types.FUND_PACKETS_FILTER,
        versionId,
        filterType: "TEXT",
        filterText,
    }
}

export function fundPacketsFilterByState(versionId, filterState) {
    return {
        type: types.FUND_PACKETS_FILTER,
        versionId,
        filterType: "STATE",
        filterState,
    }
}

export function fundPacketsChangeSelection(versionId, selectedIds) {
    return {
        type: types.FUND_PACKETS_CHANGE_SELECTION,
        versionId,
        selectedIds,
    }
}

export function fundPacketsCreate(fundId, type, data) {
    return (dispatch, getState) => {
        switch (type) {
            case "SINGLE":
                WebApi.insertPacket(fundId, data.storageNumber, data.packetTypeId, false)
                    .then(() => {
                        dispatch(modalDialogHide())
                    })
                break
            case "MORE":
                WebApi.generatePackets(fundId, data.prefix, data.packetTypeId, data.start, data.size, data.count, null)
                    .then(() => {
                        dispatch(modalDialogHide())
                    })
                break
        }
    }
}

export function fundPacketsChangeNumbers(fundId, data, ids) {
    return (dispatch, getState) => {
        WebApi.generatePackets(fundId, data.prefix, data.packetTypeId, data.start, data.size, data.count, ids)
            .then(() => {
                dispatch(modalDialogHide())
            })
    }
}

export function fundPacketsDelete(fundId, ids) {
    return (dispatch, getState) => {
        WebApi.deletePackets(fundId, ids)
    }
}

export function fundPacketsChangeState(fundId, selectedIds, newState) {
    return (dispatch, getState) => {
        WebApi.setStatePackets(fundId, selectedIds, newState)
    }
}
