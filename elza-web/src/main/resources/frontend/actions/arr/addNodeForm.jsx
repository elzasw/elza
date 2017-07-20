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
import CopyConflictForm from "../../components/arr/nodeForm/CopyConflictForm";

import {WebApi} from "../WebApi"
import {FUND_TREE_AREA_COPY} from "../constants/ActionTypes";
import {INVALIDATE} from "../../shared/detail/DetailActions";
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
        const onSubmit = (data, type) => {
            switch (type) {
                case "NEW": {
                    dispatch(addNode(data.indexNode, data.parentNode, data.versionId, data.direction, data.descItemCopyTypes, data.scenarioName, afterCreateCallback));
                }
                case "FILE": {
                    //dispatch(addNode(data.indexNode, data.parentNode, data.versionId, data.direction, data.descItemCopyTypes, data.scenarioName, afterCreateCallback));
                }
                case "OTHER": {
                    WebApi.copyNodesValidate(data.targetFundVersionId, data.targetStaticNode, data.targetStaticNodeParent, data.sourceFundVersionId, data.sourceNodes, data.ignoreRootNodes = false)
                        .then(json => {
                            if (json.scopeError) {
                                dispatch(modalDialogShow(
                                    this,
                                    i18n('Při kopírování došlo k chybě'),
                                    <CopyConflictForm
                                        scopeError={json.scopeError}
                                    />
                                ));
                            }
                            if (json.fileConflict === true || json.packetConflict === true) {
                                 dispatch(modalDialogShow(
                                    this,
                                    i18n("Jak se mají řešit konflikty?"),
                                    <CopyConflictForm
                                        fileConflict={json.fileConflict}
                                        packetConflict={json.packetConflict}
                                        onSubmit={
                                            (filesConflictResolve,
                                             packetsConflictResolve
                                            ) => dispatch(handleCopySubmit(data,filesConflictResolve,packetsConflictResolve))
                                        }

                                    />
                                ));
                            }
                            dispatch(handleCopySubmit(data))
                        });
                }
                    dispatch(modalDialogHide());
            }
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

function handleCopySubmit(data, filesConflictResolve = null,packetsConflictResolve = null){
    console.log(111,"COPY");
    console.log(data);
    return (dispatch) => {
        WebApi.copyNodes(
            data.targetFundVersionId,
            data.targetStaticNode,
            data.targetStaticNodeParent,
            data.sourceFundVersionId,
            data.sourceNodes,
            data.ignoreRootNodes = false,
            filesConflictResolve,
            packetsConflictResolve
        ).then((json)=>{
            console.log(json);
            dispatch({
                area: FUND_TREE_AREA_COPY,
                type: INVALIDATE
            });
            dispatch(modalDialogHide());
        })
    }
}
