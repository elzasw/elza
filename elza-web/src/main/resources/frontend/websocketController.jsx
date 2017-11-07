import {WebApi} from "actions/WebApi.jsx";

/**
 *  Trida pro zpracovani pozadavku na upravy v poradani
 */

class NodeRequestController {
    constructor(){
        this.nodes = {};
    }
    updateRequest(fundVersionId, nodeVersionId, nodeId, descItem, onSuccess, onError){
        let request = this.nodes[nodeId];

        if(!request){
            this.nodes[nodeId] = new NodeRequests();
        } 
        this.nodes[nodeId].updateRequest(fundVersionId, nodeVersionId, nodeId, descItem, onSuccess, onError);
    }
    sendNextUpdate(nodeId, descItemObjectId){
        let node = this.nodes[nodeId];

        if(node){
            node.sendNextUpdate(descItemObjectId);

            if(Object.keys(node.descItems).length <= 0){
                delete this.nodes[nodeId];
            }
        }
    }
}

class NodeRequests{
    constructor(){
        this.descItems = {};
        this.skipUpdatesCount = 0;
    }
    updateRequest(fundVersionId, nodeVersionId, nodeId, descItem, onSuccess, onError){
        let descItemObjectId = descItem.descItemObjectId;
        let descItemRequest = this.descItems[descItemObjectId];

        if(!descItemRequest){
            this.descItems[descItemObjectId] = new DescItemUpdateRequest();
        } 
        let requestData = {
            fundVersionId: fundVersionId,
            nodeVersionId: nodeVersionId,
            nodeId: nodeId,
            descItem: descItem,
            onSuccess: onSuccess,
            onError: onError
        };
        this.skipUpdatesCount++;
        this.descItems[descItemObjectId].addRequest(requestData);
    }
    shouldSkipUpdate(){
        if(this.skipUpdatesCount > 0){
            this.skipUpdatesCount--;
            return true;
        }
        return false;
    }
    sendNextUpdate(descItemObjectId){
        let descItem = this.descItems[descItemObjectId];

        if(descItem){
            descItem.sendNextUpdate();

            if(descItem.pendingRequests.length <= 0){
                delete this.descItems[descItemObjectId];
            }
        }
    }
}

class DescItemUpdateRequest {
    constructor(){
        this.running = null;
        this.pendingRequests = [];
    }
    addRequest(requestData){
        if(this.running){
            this.pendingRequests.push(requestData);
        } else {
            this.sendUpdate(requestData);
        }
    }
    sendUpdate(requestData){
        this.running = requestData;
        const {fundVersionId, nodeVersionId, descItem, onSuccess, onError} = requestData;
        let realRequest = WebApi.updateDescItem(fundVersionId,nodeVersionId,descItem);
        realRequest.then((response) => {
            onSuccess && onSuccess(response);
            onUpdateResponse(requestData,response);
        }).catch((reason) => {
            onError && onError(reason);
        });
    }
    sendNextUpdate(){
        this.running = null;

        if(this.pendingRequests.length > 0){
            let request = this.pendingRequests[0];
            this.sendUpdate(request);
            this.pendingRequests.shift();
        }
    }
    onResponse(){

    }
}

function onUpdateResponse(request, response){
    let nodeId = request.nodeId;
    let descItemObjectId = request.descItem.descItemObjectId;
    nodeRequestController.sendNextUpdate(nodeId,descItemObjectId);
}

export function shouldSkipNodeEvent(entityIds, callback){
    let node = nodeRequestController.nodes[entityIds[0]];
    if(!node || !node.shouldSkipUpdate){
        callback();
    }
}

const nodeRequestController = new NodeRequestController();
export default nodeRequestController;
