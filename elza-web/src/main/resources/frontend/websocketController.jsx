import {WebApi} from "actions/WebApi.jsx";

/**
 *  Trida pro zpracovani pozadavku na upravy v poradani
 */

class NodeRequestController {
    constructor(){
        this.nodes = {};
    }
    updateRequest(fundVersionId, nodeVersionId, nodeId, descItem, onSuccess, onError){
        console.log("WSController", "updateRequest",this.nodes)
        let request = this.nodes[nodeId];

        if(!request){
            this.nodes[nodeId] = new NodeRequests();
        } 
        this.nodes[nodeId].updateRequest(fundVersionId, nodeVersionId, nodeId, descItem, onSuccess, onError);
    }
    sendNextUpdate(nodeId, descItemObjectId){
        console.log("WSController", "sendNextUpdate nodes",this.nodes)
        let node = this.nodes[nodeId];
        console.log(node);

        if(node){
            node.sendNextUpdate(descItemObjectId);

            if(Object.keys(node.descItems).length <= 0){
                console.log("WSController delete node");
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
        console.log("WSController", "sendNextUpdate descItems",this.descItems)
        let descItem = this.descItems[descItemObjectId];

        if(descItem){
            descItem.sendNextUpdate();

            if(descItem.pendingRequests.length <= 0){
                console.log("WSController delete desc item");
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
            console.log("WSController", "added pending", this.pendingRequests, "running", this.running)
        } else {
            console.log("WSController", "should send", requestData)
            this.sendUpdate(requestData);
        }
    }
    sendUpdate(requestData){
        this.running = requestData;
        const {fundVersionId, nodeVersionId, descItem, onSuccess, onError} = requestData;
        console.log("WSController", "send");
        /*let requestPromise = new Promise((resolve,reject) => {
            setTimeout(() => {
                //resolve({});
                reject("reason");
            }, 10000);
        });*/
        let realRequest = WebApi.updateDescItem(fundVersionId,nodeVersionId,descItem);
        realRequest.then((response) => {
            onSuccess && onSuccess(response);
            onUpdateResponse(requestData,response);
            console.log("WSController","response in promise",this.pendingRequests);
        }).catch((reason) => {
            onError && onError(reason);
            console.error(reason);
        });
        // fake send
    }
    sendNextUpdate(){
        this.running = null;

        if(this.pendingRequests.length > 0){
            let request = this.pendingRequests[0];
            this.sendUpdate(request);
            this.pendingRequests.shift();
            console.log("WSController", "sending next", request, "remaining", this.pendingRequests);
        }
    }
    onResponse(){

    }
}

function onUpdateResponse(request, response){
    console.log("WSController", "response",request,response);
    let nodeId = request.nodeId;
    let descItemObjectId = request.descItem.descItemObjectId;
    nodeRequestController.sendNextUpdate(nodeId,descItemObjectId);
}

export function shouldSkipNodeEvent(entityIds, callback){
    let node = nodeRequestController.nodes[entityIds[0]];
    if(!node || !node.shouldSkipUpdate){
        callback();
    } else {
        console.log("WSController skipping event")
    }
}

class NodeRequestController2 {
    constructor(){
        this.nodes={
            "1":{
                descItems: [3,4,5]
            }
        };
        this.descItems={
            "3":{
                pendingRequests: [1,2]
            },
            "4":{
                pendingRequests: [1]
            },
            "5":{}
        };
    }
    updateRequest(fundVersionId, nodeVersionId, nodeId, descItem){
        let requestData = {
            fundVersionId: fundVersionId,
            nodeVersionId: nodeVersionId,
            nodeId: nodeId,
            descItem: descItem
        };
        this.nodes[nodeId].descItems.push(descItem.descItemTypeId);
        this.descItems[descItem.descItemTypeId].pendingRequests.push(requestData);
    }

    addRequest(request){

    }
    sendUpdate(request){

    }
}

let NodeRequestControllerObj = {
    nodes: {},
    updateRequest: (descItem) => {},
    sendNextUpdate: (nodeId, descItemObjectId) => {}
}

let nodes = {
    "1": {
        "24": {
            pendingRequests: [1,2,3],
            running: null
        }
    }
}
const nodeRequestController = new NodeRequestController();
export default nodeRequestController;
