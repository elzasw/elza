/**
 * Akce pro dialog přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 17.9.2016
 */

import React from 'react';
import {addNode, fundSelectSubNode} from 'actions/arr/node.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n} from 'components/shared';
import AddNodeForm from "../../components/arr/nodeForm/AddNodeForm";

/**
 * Vyvolá dialog pro přidání uzlu. Toto vyvolání dialogu slouží pro volání POUZE z pořádání! Po úspěšném volání je vybrán v pořádání přidaný node.
 * @param {Object} direction počáteční směr vytváření, který má být přednastaven v dialogu
 * @param {Object} node uzel pro který je volána akce
 * @param {int} selectedSubNodeIndex index vybraného node v pořádání, pokud nějaký vybraný je
 * @param {Object} versionId verze AS
 */
export function addNodeFormArr(direction, node, selectedSubNodeIndex, versionId) {
    return (dispatch) => {
        const afterCreateCallback = (versionId, node, parentNode) => {
            dispatch(fundSelectSubNode(versionId, node.id, parentNode));
        };

        const inParentNode = node;
        const inNode = node.childNodes[selectedSubNodeIndex];
        dispatch(addNodeForm(direction, inNode, inParentNode, versionId, afterCreateCallback));
    }
}

/**
 * Vyvolá dialog pro přidání uzlu. Toto vyvolání dialogu slouží pro ostatní volání mimo pořádání!
 * @param {Object} direction počáteční směr vytváření, který má být přednastaven v dialogu
 * @param {Object} node uzel pro který je volána akce
 * @param {Object} parentNode uzel nadřazený předanému node
 * @param {Object} versionId verze AS
 * @param {func} afterCreateCallback funkce, která je volána po úspěšném vytvoření node, předpis: function (versionId, node, parentNode), node je nově založený node a parentNode je jeho aktualizovaný nadřazený node
 * @param {array} allowedDirections seznam směrů, které chceme povolit na dialogu přidání node
 */
export function addNodeForm(direction, node, parentNode, versionId, afterCreateCallback, allowedDirections = ['BEFORE', 'AFTER', 'CHILD', 'ATEND']) {
    return (dispatch) => {
        const onSubmit = (data) => {
            dispatch(addNode(data.indexNode, data.parentNode, data.versionId, data.direction, data.descItemCopyTypes, data.scenarioName, afterCreateCallback));
            dispatch(modalDialogHide());
        };

        dispatch(modalDialogShow(
            this,
            i18n('arr.fund.addNode'),
            <AddNodeForm
                initDirection={direction}
                node={node}
                parentNode={parentNode}
                versionId={versionId}
                onSubmit={onSubmit}
                allowedDirections={allowedDirections}
            />
        ));
    }
}
