/**
 * Interaktivní tlačítko pro určení směru přidání JP
 *
 * @author Jakub Randák
 * @since 31.8.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, Icon, i18n, Loading} from 'components/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {isFundRootId} from './ArrUtils.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {getOneSettings} from 'components/arr/ArrUtils.jsx';

require ('./AddNodeCross.less');

const AddNodeCross = class AddNodeCross extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleAddNode');
    }
    /**
     * Vrátí pole ke zkopírování
     */
    getDescItemTypeCopyIds() {
        let itemsToCopy = null;
        if (this.props.nodeSettings != "undefined") {
            const nodeIndex = indexById(this.props.nodeSettings.nodes, this.props.nodeId);
            if (nodeIndex != null) {
                itemsToCopy = this.props.nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
            }
        }
        return itemsToCopy;
    }
    /**
     * @param event Event selectu
     *
     * Přidání podřízeného záznamu
     */
    handleAddNode(direction) {
        const parentNode = this.props.parentNode;
        const selectedSubNode = this.props.node;
        console.log(direction);
        console.log(selectedSubNode);
        alert(direction);
        //this.dispatch(addNode(selectedSubNode, parentNode, this.props.versionId, "CHILD", this.getDescItemTypeCopyIds(), scenario));
    }

    renderCross(){
      const notRoot = !isFundRootId(this.props.node.id);
      console.log(notRoot);
        return(


                  <div className="hid">
                      {notRoot &&
                      [<div className="but top" onClick={this.handleAddNode.bind(this,'BEFORE')}><span className="ico fa fa-arrow-up"></span><br/>Před
                      </div>,
                      <div className="but bottom" onClick={this.handleAddNode.bind(this,'AFTER')}><span className="ico fa fa-arrow-down"></span><br/>Za
                      </div>,
                      <div className="but bottom2" onClick={this.handleAddNode.bind(this,'END')}><span className="ico fa fa-arrow-down"></span><br/>Na konec</div>]
                      }
                      <div className="but right" onClick={this.handleAddNode.bind(this,'CHILD')}><span className="ico fa fa-level-up fa-rotate-90"></span><br/>Pod
                      </div>
                  </div>

        )
    }

    render(){
      const { userDetail, fundId, closed} = this.props;
      let formActions

      var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fundId);
      console.log("--- Node cross ---");
      console.log(settings);
      var settingsValues = settings.value != 'false';
      const readMode = closed || settingsValues;
      var active = false;

      if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId})) {
          if (!readMode) {
              console.log("*** EDIT MODE ***");
              active = true;
              formActions = this.renderCross();
          }
      }


      console.log("--- END Node cross ---")
      return(
        <div className="con2">
            <div className="cont">
              <div className={active ? "but center blue-but":"but center"}><span className="ico fa fa-plus"></span></div>
              {formActions}
            </div>
        </div>
      )
    }

  }
  AddNodeCross.propTypes = {
      node: React.PropTypes.any.isRequired,
      userDetail: React.PropTypes.object.isRequired
  };
module.exports = AddNodeCross;
