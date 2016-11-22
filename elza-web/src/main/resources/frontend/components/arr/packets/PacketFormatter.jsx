/**
 *  ------ PacketFormatter ------
 *  Třída pro formátování názvů obalů
 *
*/
import {getMapFromList} from 'stores/app/utils.jsx'
export default class PacketFormatter{
    constructor(packetTypes) {
        this.packetTypesMap = getMapFromList(packetTypes.items)
    }
    getPacketTypesMap(){
        return this.packetTypesMap;
    }
    typeFormat(typeName){
        return(" | " + typeName + "");
    }
    format(packet){
        let name;
        if (typeof packet.packetTypeId !== 'undefined' && packet.packetTypeId !== null) {
            name = packet.storageNumber + this.typeFormat(this.packetTypesMap[packet.packetTypeId].name);
        } else {
            name = packet.storageNumber
        }
        return name;
    }
}
