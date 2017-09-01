import {Utils} from 'components/shared';
var keyModifier = Utils.getKeyModifier()

var defaultKeymap = {
    Main: {
        home: ['alt+1','g h'],  	//GOTO_HOME_PAGE
        arr: ['alt+2','g a'],       //GOTO_ARR_PAGE
        registry: ['alt+3','g r'],  //GOTO_REGISTRY_PAGE
        party: ['alt+4','g p'],     //GOTO_PARTY_PAGE
        admin: ['alt+5','g n'],     //GOTO_ADMIN_PAGE
    }
}
export default defaultKeymap;
