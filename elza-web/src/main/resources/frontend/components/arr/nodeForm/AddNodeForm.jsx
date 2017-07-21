/**
 * Dialog pro přidání nové JP
 *
 * @author Tomáš Pytelka
 * @since 26.8.2016
 */
import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import {
  Modal,
  Button,
  Radio,
  FormGroup,
  ControlLabel,
  Form,
  Col,
  Row,
  Grid,
  FormControl,
  Checkbox
} from 'react-bootstrap';
import { WebApi } from 'actions/index.jsx';
import { addNode } from 'actions/arr/node.jsx';
import {
  AbstractReactComponent,
  i18n,
  FormInput,
  Loading,
  Autocomplete
} from 'components/shared';
import { isFundRootId, getOneSettings } from 'components/arr/ArrUtils.jsx';
import { indexById } from 'stores/app/utils.jsx';
import './AddNodeForm.less';
import { getSetFromIdsList } from 'stores/app/utils.jsx';

import FundTreeCopy from '../FundTreeCopy';
import FundField from '../../admin/FundField';
import { FUND_TREE_AREA_COPY } from '../../../actions/constants/ActionTypes';

class AddNodeForm extends AbstractReactComponent {
  static PropTypes = {
    node: React.PropTypes.object.isRequired, // node pro který se akce volá
    parentNode: React.PropTypes.object.isRequired, // nadřený node pro vstupní node
    initDirection: React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND']),
    nodeSettings: React.PropTypes.object.isRequired,
    onSubmit: React.PropTypes.func.isRequired,
    allowedDirections: React.PropTypes.arrayOf(
      React.PropTypes.oneOf(['BEFORE', 'AFTER', 'CHILD', 'ATEND'])
    )
  };

  state = {
    // initial states
    scenarios: undefined,
    loading: false,
    selectedDirection: this.props.initDirection,
    selectedScenario: undefined,
    allowedDirections: ['BEFORE', 'AFTER', 'CHILD', 'ATEND'],
    selectedType: 'NEW',
    selectedSourceAS: 'FILE',
    scopeList: [],
    value: '',
    ignoreRootNodes: false,
    submitting: false
  };

  /**
     * Připravuje hodnoty proměnných rodič, směr pro potřeby API serveru
     * - Přidání JP na konec se mění na přidání dítěte rodiči
     * @param {String} inDirection směr, kterým se má vytvořit nová JP
     * @param {Object} inNode uzel pro který je volána akce
     * @param {Object} inParentNode nadřazený uzel k inNode
     */
  formatDataForServer = (inDirection, inNode, inParentNode) => {
    let direction, node, parentNode;

    switch (inDirection) {
      case 'ATEND':
        direction = 'CHILD';
        node = inParentNode;
        parentNode = inParentNode;
        break;
      case 'CHILD':
        direction = inDirection;
        node = inNode;
        parentNode = inNode;
        break;
      case 'BEFORE':
      case 'AFTER':
        direction = inDirection;
        node = inNode;
        parentNode = inParentNode;
        break;
    }

    return { direction: direction, activeNode: node, parentNode: parentNode };
  };

  /**
     * Načte scénáře a vrátí je pro zobrazení, v průběhu načístání nastaví ve state 'loading'
     * resp. uloží je do state 'scenarios'
     */
  getDirectionScenarios = newDirection => {
    if (newDirection === '') {
      // direction is not selected
      this.setState({
        scenarios: undefined
      });
      return;
    }
    // direction is selected
    this.setState({
      loading: true
    });
    const { node, parentNode, versionId } = this.props;
    // nastavi odpovidajiciho rodice a direction pro dotaz
    var dataServ = this.formatDataForServer(newDirection, node, parentNode);
    // ajax dotaz na scenare
    WebApi.getNodeAddScenarios(
      dataServ.activeNode,
      versionId,
      dataServ.direction
    ).then(
      result => {
        // resolved
        // select first scenario
        var selScn = undefined;
        if (result.length > 0) selScn = result[0].name;
        this.setState({
          scenarios: result,
          loading: false,
          selectedScenario: selScn
        });
      },
      reason => {
        // rejected
        this.setState({
          scenarios: undefined,
          loading: false,
          selectedScenario: undefined
        });
      }
    );
  };

  handleDirectionChange = e => {
    var dr = e.target.value;
    this.setState({ selectedDirection: dr });
    this.getDirectionScenarios(dr);
  };

  handleScenarioChange = e => {
    this.setState({ selectedScenario: e.target.value });
  };

  /**
     * Vrátí prvky popisu ke zkopírování na základě proměnné props.nodeSettings
     */
  getDescItemTypeCopyIds = () => {
    const nodeSettings = this.props.nodeSettings;
    const nodeId = this.props.parentNode.id;

    let itemsToCopy = null;
    if (nodeSettings != undefined) {
      const nodeIndex = indexById(nodeSettings.nodes, nodeId);
      if (nodeIndex != null) {
        itemsToCopy = nodeSettings.nodes[nodeIndex].descItemTypeCopyIds;
      }
    }
    return itemsToCopy;
  };

  /**
     * Zprostředkuje přidání nové JP, dle aktuálně vyplněných dat ve formuláři
     * resp. dat uložených ve state 'selectedDirection', 'selectedScenario'
     */
  handleFormSubmit = e => {
    e.preventDefault();
    const {
      onSubmit,
      node,
      parentNode,
      versionId,
      initDirection,
      globalFundTree: { fundTreeCopy }
    } = this.props;
    const { selectedDirection, selectedScenario } = this.state;

    // nastavi odpovidajiciho rodice a direction pro dotaz
    const dataServ = this.formatDataForServer(
      selectedDirection,
      node,
      parentNode
    );
    if (this.state.selectedType === 'NEW') {
      // Data pro poslání do on submit - obsahují všechny informace pro založení
      const submitData = {
        indexNode: dataServ.activeNode,
        parentNode: dataServ.parentNode,
        versionId: versionId,
        direction: dataServ.direction,
        descItemCopyTypes: this.getDescItemTypeCopyIds(),
        scenarioName: selectedScenario
      };

      onSubmit(submitData, 'NEW');
    } else if (this.state.selectedSourceAS === 'FILE') {
      alert('FILE SUMBTI');
      const sumbitData = {};
      //onSubmit(sumbitData, 'FILE');
    } else if (this.state.selectedSourceAS === 'OTHER') {
      const newNode = {
        id: node.id,
        version: node.id
      };

      const sourceNodes = [];

      for (let nodeItem in fundTreeCopy.selectedIds) {
        const n = fundTreeCopy.nodes.find(i => {
          if (i.id == nodeItem) {
            return i;
          }
        });
        if (n) {
          sourceNodes.push({
            id: n.id,
            version: n.version
          });
        }
      }

      const sumbitData = {
        targetFundVersionId: versionId,
        targetStaticNode: newNode,
        targetStaticNodeParent: getParentNode(parentNode),
        sourceFundVersionId: this.props.globalFundTree.versionId,
        sourceNodes,
        ignoreRootNodes: this.state.ignoreRootNodes
      };
      this.setState({ submitting: true });
      onSubmit(sumbitData, 'OTHER', () => {
        this.setState({ submitting: false });
      });
    }
  };

  componentDidMount() {
    const { initDirection } = this.props;
    this.getDirectionScenarios(initDirection);
    this.fetchScopeList();
  }
  fetchScopeList = () => {
    WebApi.getAllScopes().then(data => {
      this.setState({
        scopeList: data
      });
    });
  };

  render() {
    const {
      allowedDirections,
      onClose,
      node,
      parentNode,
      versionId,
      initDirection,
      arrRegion,
      userDetail
    } = this.props;
    const { scenarios, loading, submitting } = this.state;
    const notRoot = !isFundRootId(parentNode.id);

    var scnRadios = [];
    if (!loading) {
      var i = 0;
      if (scenarios) {
        for (i; i < scenarios.length; i++) {
          scnRadios.push(
            <Radio
              key={'scns-' + i}
              defaultChecked={i === 0}
              autoFocus={i === 0}
              name="scns"
              onChange={this.handleScenarioChange}
              value={scenarios[i].name}
            >
              {scenarios[i].name}
            </Radio>
          );
        }
      }

      let strictMode = false;
      const fund =
        arrRegion.activeIndex != null
          ? arrRegion.funds[arrRegion.activeIndex]
          : null;
      if (fund) {
        strictMode = fund.activeVersion.strictMode;

        let userStrictMode = getOneSettings(
          userDetail.settings,
          'FUND_STRICT_MODE',
          'FUND',
          fund.id
        );
        if (userStrictMode && userStrictMode.value !== null) {
          strictMode = userStrictMode.value === 'true';
        }
      }

      if (!strictMode || i === 0) {
        scnRadios.push(
          <Radio
            key={'scns-' + i}
            defaultChecked={i === 0}
            autoFocus={i === 0}
            name="scns"
            onChange={this.handleScenarioChange}
            value={''}
          >
            {i18n('subNodeForm.add.noScenario')}
          </Radio>
        );
      }
    } else {
      scnRadios.push(
        <div>
          {i18n('arr.fund.addNode.noDirection')}
        </div>
      );
    }

    // Položky v select na směr
    const allowedDirectionsMap = getSetFromIdsList(allowedDirections);
    const directions = {
      BEFORE: 'before',
      AFTER: 'after',
      CHILD: 'child',
      ATEND: 'atEnd'
    };
    var options = [];
    for (let d in directions) {
      if (allowedDirectionsMap[d]) {
        if (isFundRootId(parentNode.id) && d !== 'CHILD') {
          continue;
        }
        options.push(
          <option value={d} key={d}>
            {i18n(`arr.fund.addNode.${directions[d]}`)}
          </option>
        );
      }
    }

    return (
      <Form>
        <Modal.Body>
          <FormGroup>
            <Row>
              <Col xs={2}>
                <Radio
                  inline
                  name="selectType"
                  checked={this.state.selectedType === 'NEW'}
                  onChange={e => {
                    this.setState({ selectedType: 'NEW' });
                  }}
                >
                  {i18n('arr.fund.addNode.type.new')}
                </Radio>
              </Col>
              <Col xs={3}>
                <Radio
                  inline
                  name="selectType"
                  checked={this.state.selectedType === 'EXISTING'}
                  onChange={e => {
                    this.setState({ selectedType: 'EXISTING' });
                  }}
                >
                  {i18n('arr.fund.addNode.type.existing')}
                </Radio>
              </Col>
            </Row>
            <Row>
              <Col xs={12}>
                <FormInput
                  ref="selsel"
                  componentClass="select"
                  disabled={loading || submitting}
                  label={i18n('arr.fund.addNode.direction')}
                  defaultValue={initDirection}
                  onChange={this.handleDirectionChange}
                >
                  {options}
                </FormInput>
              </Col>
            </Row>
          </FormGroup>
          {this.state.selectedType === 'NEW'
            ? this.renderCreateNew(loading, scnRadios)
            : this.renderCreateExisting()}
        </Modal.Body>
        <Modal.Footer>
          <Row>
            {this.state.selectedSourceAS === 'OTHER' &&
              <Col xs={4}>
                <Checkbox
                  disabled={submitting}
                  inline
                  checked={this.state.ignoreRootNodes}
                  onChange={() => {
                    console.log(this.state.ignoreRootNodes);
                    this.setState(() => {
                      return { ignoreRootNodes: !this.state.ignoreRootNodes };
                    });
                  }}
                >
                  {i18n('arr.fund.addNode.ignoreRootNodes')}
                </Checkbox>
              </Col>}
            <Col xs={4} xsOffset={4}>
              <Button
                disabled={submitting}
                type="submit"
                onClick={this.handleFormSubmit}
              >
                {i18n('global.action.store')}
              </Button>
              <Button disabled={submitting} bsStyle="link" onClick={onClose}>
                {i18n('global.action.cancel')}
              </Button>
            </Col>
          </Row>
        </Modal.Footer>
      </Form>
    );
  }

  renderCreateNew(loading, scnRadios) {
    return (
      <div>
        <FormGroup>
          <ControlLabel>
            {i18n('arr.fund.addNode.scenario')}
          </ControlLabel>
          {loading
            ? <Loading />
            : <FormGroup key="Scenarios">
                {scnRadios}
              </FormGroup>}
        </FormGroup>
      </div>
    );
  }

  renderCreateExisting() {
    const { submitting } = this.state;
    return [
      <FormGroup>
        <Row>
          <Col xs={6}>
            <Row>
              <Col xs={5}>
                <Radio
                  disabled={submitting}
                  inline
                  name="selectSource"
                  checked={this.state.selectedSourceAS === 'FILE'}
                  onChange={e => {
                    this.setState({ selectedSourceAS: 'FILE' });
                  }}
                >
                  {i18n('arr.fund.addNode.type.existing.file')}
                </Radio>
              </Col>
              <Col xs={7}>
                <Radio
                  disabled={submitting}
                  inline
                  name="selectSource"
                  checked={this.state.selectedSourceAS === 'OTHER'}
                  onChange={e => {
                    this.setState({ selectedSourceAS: 'OTHER' });
                  }}
                >
                  {i18n('arr.fund.addNode.type.existing.other')}
                </Radio>
              </Col>
            </Row>
          </Col>
        </Row>
      </FormGroup>,
      this.state.selectedSourceAS === 'OTHER'
        ? this.renderCreateFromOther()
        : this.renderCreateFromFile()
    ];
  }

  renderCreateFromFile() {
    const { scopeList, submitting } = this.state;
    return [
      <FormGroup>
        <Autocomplete
          disabled={submitting}
          label={i18n('arr.fund.regScope')}
          items={scopeList}
          getItemId={item => (item ? item.id : null)}
          getItemName={item => {
            return item ? item.name : '';
          }}
          value={this.props.value}
        />
      </FormGroup>,
      <FormGroup>
        <FormControl disabled={submitting} name="soubor" type="file" />
      </FormGroup>
    ];
  }

  renderCreateFromOther() {
    const { value, submitting } = this.state;
    const { fundTreeCopy, fund, node, versionId } = this.props;

    return [
      <FormGroup>
        <ControlLabel>
          {i18n('arr.fund.addNode.type.existing.archiveFile')}
        </ControlLabel>
        <FundField
          excludedId={versionId}
          ref="fundField"
          value={value}
          onChange={item => {
            this.dispatch({
              type: 'SELECT_FUND_GLOBAL',
              area: 'FUND_TREE_AREA_COPY',
              fund: item,
              versionId: item.versions[0].id
            });
            this.setState(() => {
              return { value: item.name };
            });
          }}
        />
      </FormGroup>,
      <div>
        {fund &&
          <ControlLabel>
            {i18n('arr.history.title.nodeChanges')}
          </ControlLabel>}
        {fund &&
          <FundTreeCopy
            disabled={submitting}
            className="fund-tree-container-fixed"
            fund={fund}
            cutLongLabels={true}
            versionId={fund.versions[0].id || 0}
            ref="treeCopy"
            {...fundTreeCopy}
          />}
      </div>
    ];
  }
}
function getParentNode(parentNode) {
  if (parentNode) {
    return null;
  }
  return {
    id: parentNode.id,
    version: parentNode.version
  };
}
function mapStateToProps(state) {
  const { arrRegion, userDetail, registryDetail } = state;

  return {
    fund: arrRegion.globalFundTree.fund,
    fundTreeCopy: arrRegion.globalFundTree.fundTreeCopy,
    globalFundTree: arrRegion.globalFundTree,
    nodeSettings: arrRegion.nodeSettings,
    arrRegion: arrRegion,
    userDetail: userDetail,
    registryDetail
  };
}

export default connect(mapStateToProps)(AddNodeForm);
