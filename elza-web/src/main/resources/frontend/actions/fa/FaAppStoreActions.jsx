import airflux from 'airflux';

var actions = {
    selectFa: new airflux.Action(),
    closeFa: new airflux.Action(),
    selectNode: new airflux.Action(),
    closeNode: new airflux.Action()
}

actions.selectFa.asFunction.listen((faId)=>{
    console.log('selectFa: faId=' + faId);
});
actions.closeFa.asFunction.listen((faId)=>{
    console.log('closeFa: faId=' + faId);
});
actions.selectNode.asFunction.listen((nodeId)=>{
    console.log('selectNode: nodeId=' + nodeId);
});
actions.closeNode.asFunction.listen((nodeId)=>{
    console.log('closeNode: nodeId=' + nodeId);
});

module.exports = actions;
