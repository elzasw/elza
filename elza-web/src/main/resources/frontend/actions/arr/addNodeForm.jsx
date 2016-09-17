/**
 * Akce pro dialog přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 17.9.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {addNode} from 'actions/arr/node.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n, AddNodeForm} from 'components/index.jsx';

/**
 * Vyvol8 dialog pro přidání uzlu
 * @param {Object} direction počáteční směr vytváření, který má být přednastaven v dialogu
 * @param {Object} node uzel pro který je volána akce
 * @param {Object} versionId verze AS
 */
export function addNodeForm(direction, node, versionId) {
    return (dispatch) => {
        dispatch(modalDialogShow(this, i18n('arr.fund.addNode'),
                <AddNodeForm node={node} versionId={versionId} initDirection={direction} handlePostSubmitActions={addNodeFormSubmit.bind(null, dispatch)}/>
            )
        )
    }
}

function addNodeFormSubmit(dispatch, data) {
    dispatch(modalDialogHide());
}
