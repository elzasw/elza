/**
 * Akce pro dialog přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 17.9.2016
 */

import React from 'react';
import {addNode, fundSelectSubNode} from 'actions/arr/node.jsx';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';
import {i18n} from 'components/shared';
import AddNodeForm from '../../components/arr/nodeForm/AddNodeForm';
import CopyConflictForm from '../../components/arr/nodeForm/CopyConflictForm';

import {importForm} from 'actions/global/global.jsx';
import {WebApi} from '../WebApi';
import {globalFundTreeInvalidate} from './globalFundTree';

/**
 * Vyvolá dialog pro přidání uzlu. Toto vyvolání dialogu slouží pro volání POUZE z pořádání! Po úspěšném volání je vybrán v pořádání přidaný node.
 * @param {Object} direction počáteční směr vytváření, který má být přednastaven v dialogu
 * @param {Object} node uzel pro který je volána akce
 * @param {int} selectedSubNodeIndex index vybraného node v pořádání, pokud nějaký vybraný je
 * @param {Object} versionId verze AS
 */
export function addNodeFormArr(direction, node, selectedSubNodeIndex, versionId) {
    return dispatch => {
        const afterCreateCallback = (versionId, node, parentNode) => {
            dispatch(fundSelectSubNode(versionId, node.id, parentNode));
        };

        const inParentNode = node;
        const inNode = node.childNodes[selectedSubNodeIndex];
        dispatch(addNodeForm(direction, inNode, inParentNode, versionId, afterCreateCallback));
    };
}

/**
 * Vyvolá dialog pro přidání uzlu. Toto vyvolání dialogu slouží pro ostatní volání mimo pořádání!
 * @param {Object} direction počáteční směr vytváření, který má být přednastaven v dialogu
 * @param {Object} node uzel pro který je volána akce
 * @param {Object} parentNode uzel nadřazený předanému node
 * @param {Object} versionId verze AS
 * @param {Function} afterCreateCallback funkce, která je volána po úspěšném vytvoření node, předpis: function (versionId, node, parentNode), node je nově založený node a parentNode je jeho aktualizovaný nadřazený node
 * @param {array} allowedDirections seznam směrů, které chceme povolit na dialogu přidání node
 */
export function addNodeForm(
    direction,
    node,
    parentNode,
    versionId,
    afterCreateCallback,
    allowedDirections = ['BEFORE', 'AFTER', 'CHILD', 'ATEND'],
) {
    return dispatch => {
        const onSubmit = (data, type, cb, emptyItemTypeIds) => {
            switch (type) {
                case 'NEW': {
                    dispatch(
                        addNode(
                            data.indexNode,
                            data.parentNode,
                            data.versionId,
                            data.direction,
                            data.descItemCopyTypes,
                            data.scenarioName,
                            data.createItems,
                            afterCreateCallback,
                            emptyItemTypeIds,
                        ),
                    );
                    dispatch(modalDialogHide());
                    break;
                }
                case 'FILE': {
                    let formData = new FormData();
                    for (const key in data) {
                        if (data.hasOwnProperty(key)) {
                            let value = data[key];
                            formData.append(key, value);
                        }
                    }
                    dispatch(importForm(formData, 'Fund'));
                    break;
                }
                case 'OTHER': {
                    dispatch(handleSubmitOther(data, cb));
                    break;
                }
                default:
                    break;
            }
        };

        dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.addNode'),
                <AddNodeForm
                    initDirection={direction}
                    node={node}
                    parentNode={parentNode}
                    versionId={versionId}
                    onSubmit={onSubmit}
                    allowedDirections={allowedDirections}
                />,
                null,
                dispatch(globalFundTreeInvalidate()),
            ),
        );
    };
}

function handleSubmitOther(data, cb) {
    return dispatch => {
        WebApi.copyNodesValidate(
            data.targetFundVersionId,
            data.sourceFundVersionId,
            data.sourceNodes,
            data.ignoreRootNodes,
            data.selectedDirection,
        )
            .then(json => {
                if (json.scopeError === true || json.fileConflict === true || json.packetConflict === true) {
                    dispatch(modalDialogHide());
                    dispatch(
                        modalDialogShow(
                            this,
                            i18n('arr.fund.addNode.conflict'),
                            <CopyConflictForm
                                {...json}
                                onSubmit={(filesConflictResolve, structuresConflictResolve, cb) =>
                                    dispatch(
                                        handleCopySubmit(data, filesConflictResolve, structuresConflictResolve, cb),
                                    )
                                }
                            />,
                        ),
                    );
                } else {
                    dispatch(handleCopySubmit(data, cb));
                }
            })
            .catch(() => {
                cb();
            });
    };
}

function handleCopySubmit(data, filesConflictResolve = null, structuresConflictResolve = null, cb) {
    return dispatch => {
        WebApi.copyNodes(
            data.targetFundVersionId,
            data.targetStaticNode,
            data.targetStaticNodeParent,
            data.sourceFundVersionId,
            data.sourceNodes,
            data.ignoreRootNodes,
            data.selectedDirection,
            data.templateId,
            filesConflictResolve,
            structuresConflictResolve,
        )
            .then(json => {
                dispatch(modalDialogHide());
                dispatch(globalFundTreeInvalidate());
                cb();
            })
            .catch(() => {
                cb();
            });
    };
}
