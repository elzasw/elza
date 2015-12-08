import airflux from 'airflux';

import React from 'react';

import {FaAppStoreActions} from 'actions';

/**
 * Data pro JP.
 */
class Node {
    constructor(id) {
        this.id = id;
    }
}

/**
 * Data pro AP.
 */
class Fa {
    constructor(id) {
        this.id = id;
        this.mainNodes = {
            nodes: new Array(),
            activeNode: null
        }

        for (var a=0; a<id * 3; a++) {
            var node = new Node(a);
            this.mainNodes.nodes.push(node);
            if (a == 0) {
                this.mainNodes.activeNode = node;
            }
        }
    }

    getMainNodes() {
        return this.mainNodes;
    }

    isNodeSelected(node) {
        return this.mainNodes.activeNode  && this.mainNodes.activeNode.id === node.id;
    }

    getNode(index) {
        if (index >=0 && index < this.mainNodes.nodes.length) {
            return this.mainNodes.nodes[index];
        } else {
            return null;
        }
    }

    getNodeIndex(node) {
        var index = 0;
        return this.mainNodes.nodes.one(n => {
            if (n.id == node.id) {
                return index;
            }
            index++;
        });
    }

    setActiveNode(node) {
        this.mainNodes.activeNode = node;
    }

    removeNode(index) {
        var node = this.getNode(index);
        if (node != null) {
            this.mainNodes.nodes.splice(index, 1);
        }
    }

    findNodeById(nodeId) {
        return this.mainNodes.nodes.one(node => {
            if (node.id == nodeId) {
                return node;
            } else {
                return null;
            }
        });
    }
}

/**
 * Store pro modul AP.
 */
var store = class FaAppStore extends airflux.Store {

    constructor() {
        super();

        this.listenTo(FaAppStoreActions.selectNode, this.onSelectNode);
        this.listenTo(FaAppStoreActions.closeNode, this.onCloseNode);
        this.listenTo(FaAppStoreActions.selectFa, this.onSelectFa);
        this.listenTo(FaAppStoreActions.closeFa, this.onCloseFa);

        this.mainFas = {
            fas: new Array(),
            activeFa: null
        }
        this.mainFas.fas = new Array();
        var fa = new Fa(1);
        this.mainFas.activeFa = fa;
        this.mainFas.fas.push(fa);
        fa = new Fa(2);
        this.mainFas.fas.push(fa);
    }

    findFaById(faId) {
        return this.mainFas.fas.one(fa => {
            if (fa.id == faId) {
                return fa;
            } else {
                return null;
            }
        });
    }

    getFaById(faId) {
        if (!this.checkActiveFa) {
            return null;
        }

        var fa = this.findFaById(faId);
        if (fa == null) {
            console.error("Cannot find fa " + faId + ".");
            return null;
        }

        return fa;
    }

    checkActiveFa() {
        if (!this.mainFas.activeFa) {
            console.error("No active fa exists.");
            return false;
        }
        return true;
    }

    getNodeById(nodeId) {
        if (!this.checkActiveFa) {
            return null;
        }

        var node = this.mainFas.activeFa.findNodeById(nodeId);
        if (node == null) {
            console.error("Cannot find node " + nodeId + " in fa " + this.mainFas.activeFa.id + ".");
            return null;
        }

        return node;
    }

    getMainFas() {
        return this.mainFas;
    }

    onCloseNode(nodeId, newActiveNodeId) {
        var node = this.getNodeById(nodeId);
        if (node == null) {
            return;
        }

        var wasSelected = this.mainFas.activeFa.mainNodes.activeNode && this.mainFas.activeFa.mainNodes.activeNode.id === nodeId;

        var index = this.mainFas.activeFa.getNodeIndex(node);
        this.mainFas.activeFa.removeNode(index);

        if (wasSelected) {
            if (index < this.mainFas.activeFa.mainNodes.nodes.length) {
                this.mainFas.activeFa.setActiveNode(this.mainFas.activeFa.mainNodes.nodes[index]);
            } else if (index - 1 >= 0) {
                this.mainFas.activeFa.setActiveNode(this.mainFas.activeFa.mainNodes.nodes[index - 1]);
            } else {
                this.mainFas.activeFa.setActiveNode(null);
            }
        }

        this.trigger(this);
    }

    onSelectNode(nodeId) {
        var node = this.getNodeById(nodeId);
        if (node == null) {
            return;
        }

        this.mainFas.activeFa.setActiveNode(node);

        this.trigger(this);
    }

    onCloseFa(faId) {
        var fa = this.getFaById(faId);
        if (fa == null) {
            return;
        }

        var wasSelected = this.mainFas.activeFa && this.mainFas.activeFa.id === faId;

        var index = this.getFaIndex(fa);
        this.removeFa(index);

        if (wasSelected) {
            if (index < this.mainFas.fas.length) {
                this.setActiveFa(this.mainFas.fas[index]);
            } else if (index - 1 >= 0) {
                this.setActiveFa(this.mainFas.fas[index - 1]);
            } else {
                this.setActiveFa(null);
            }
        }

        this.trigger(this);
    }

    onSelectFa(faId) {
        var fa = this.getFaById(faId);
        if (fa == null) {
            return;
        }

        this.setActiveFa(fa);

        this.trigger(this);
    }

    setActiveFa(fa) {
        this.mainFas.activeFa = fa;
    }

    getFaIndex(fa) {
        var index = 0;
        return this.mainFas.fas.one(f => {
            if (f.id == fa.id) {
                return index;
            }
            index++;
        });
    }

    removeFa(index) {
        var fa = this.getFa(index);
        if (fa != null) {
            this.mainFas.fas.splice(index, 1);
        }
    }

    getFa(index) {
        if (index >=0 && index < this.mainFas.fas.length) {
            return this.mainFas.fas[index];
        } else {
            return null;
        }
    }
}

module.exports = new store();
