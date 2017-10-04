import React from "react";

export function renderUserItem(user, isHighlighted = false, isSelected = false) {
    let cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    const itemStr = user.party.record.record + " (" + user.username + ")";
    return (
        <div
            className={cls}
            key={user.id}
        >{itemStr}</div>
    )
}

export function renderGroupItem(group, isHighlighted = false, isSelected = false) {
    let cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    const itemStr = group.name;
    return (
        <div
            className={cls}
            key={group.id}
        >{itemStr}</div>
    )
}

export function renderFundItem(fund, isHighlighted = false, isSelected = false) {
    let cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    const itemStr = fund.name;
    return (
        <div
            className={cls}
            key={fund.id}
        >{itemStr}</div>
    )
}

export function renderScopeItem(scope, isHighlighted = false, isSelected = false) {
    let cls = 'item';
    if (isHighlighted) {
        cls += ' focus'
    }
    if (isSelected) {
        cls += ' active'
    }

    const itemStr = scope.name;
    return (
        <div
            className={cls}
            key={scope.id}
        >{itemStr}</div>
    )
}
