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
        this.nodes = new Array();

        for (var a=0; a<id * 3; a++) {
            var node = new Node(a);
            this.nodes.push(node);
            if (a == 0) {
                this.activeNode = node;
            }
        }
    }

    getAllNodes() {
        return this.nodes;
    }


    getActiveNode() {
        return this.activeNode;
    }

    isNodeSelected(node) {
        return this.activeNode  && this.activeNode.id === node.id;
    }

    getNode(index) {
        if (index >=0 && index < this.nodes.length) {
            return this.nodes[index];
        } else {
            return null;
        }
    }

    getNodeIndex(node) {
        var index = 0;
        return this.nodes.one(n => {
            if (n.id == node.id) {
                return index;
            }
            index++;
        });
    }

    setActiveNode(node) {
        this.activeNode = node;
    }

    removeNode(index) {
        var node = this.getNode(index);
        if (node != null) {
            this.nodes.splice(index, 1);
        }
    }

    findNodeById(nodeId) {
        return this.nodes.one(node => {
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

        this.fas = new Array();
        var fa = new Fa(1);
        this.activeFa = fa;
        this.fas.push(fa);
        fa = new Fa(2);
        this.fas.push(fa);
    }

    findFaById(faId) {
        return this.fas.one(fa => {
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
        if (!this.activeFa) {
            console.error("No active fa exists.");
            return false;
        }
        return true;
    }

    getNodeById(nodeId) {
        if (!this.checkActiveFa) {
            return null;
        }

        var node = this.activeFa.findNodeById(nodeId);
        if (node == null) {
            console.error("Cannot find node " + nodeId + " in fa " + this.activeFa.id + ".");
            return null;
        }

        return node;
    }

    getAllFas() {
        return this.fas;
    }

    getActiveFa() {
        return this.activeFa;
    }

    onCloseNode(nodeId, newActiveNodeId) {
        var node = this.getNodeById(nodeId);
        if (node == null) {
            return;
        }

        var index = this.activeFa.getNodeIndex(node);
        this.activeFa.removeNode(index);

        if (newActiveNodeId != null) {
            var newActiveNode = this.getNodeById(newActiveNodeId);
            if (newActiveNode == null) {
                console.error("Cannot find node " + newActiveNodeId + " in fa " + this.activeFa.id + ".");
            } else {
                this.activeFa.setActiveNode(newActiveNode);
            }
        } else {
            this.activeFa.setActiveNode(null);
        }

        this.trigger(this);
    }

    onSelectNode(nodeId) {
        var node = this.getNodeById(nodeId);
        if (node == null) {
            return;
        }

        this.activeFa.setActiveNode(node);

        this.trigger(this);
    }

    onCloseFa(faId, newActiveFaId) {
        var fa = this.getFaById(faId);
        if (fa == null) {
            return;
        }

        var index = this.getFaIndex(fa);
        this.removeFa(index);

        if (newActiveFaId != null) {
            var newActiveFa = this.getFaById(newActiveFaId);
            if (newActiveFa == null) {
                console.error("Cannot find fa " + newActiveFaIdc+ ".");
            } else {
                this.setActiveFa(newActiveFa);
            }
        } else {
            this.setActiveFa(null);
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
        this.activeFa = fa;
    }

    getFaIndex(fa) {
        var index = 0;
        return this.fas.one(f => {
            if (f.id == fa.id) {
                return index;
            }
            index++;
        });
    }

    removeFa(index) {
        var fa = this.getFa(index);
        if (fa != null) {
            this.fas.splice(index, 1);
        }
    }

    getFa(index) {
        if (index >=0 && index < this.fas.length) {
            return this.fas[index];
        } else {
            return null;
        }
    }
}

module.exports = new store();
