import { PowerGrid } from '@/components/Topology/topology.constants'
import API from '@/services'
import { IdLabelProps, QueryParameters, VuexContext } from '@/types'
import { SZLRequest, TopologyGraphList, VerticesAndEdges } from '@/types/topology'
import { Edges, Nodes } from 'v-network-graph'
import { State } from './state'
import getters from './getters'

interface ContextWithState extends VuexContext {
  state: State
}

const parseVerticesAndEdges = (resp: VerticesAndEdges, context: VuexContext) => {
  const edges: Edges = {}
  const vertices: Nodes = {}

  for (const edge of resp.edges) {
    edges[edge.label] = { source: edge.source.id, target: edge.target.id }
  }

  for (const vertex of resp.vertices) {
    vertices[vertex.id] = {
      name: vertex.label,
      id: vertex.id,
      tooltip: vertex.tooltipText,
      label: vertex.label,
      icon: 'generic_icon'
    }
  }

  if (resp.defaultFocus && resp.defaultFocus.vertexIds.length) {
    const defaultIds = resp.defaultFocus.vertexIds.map((obj) => obj.id)

    if (defaultIds.length) {
      const defaultObjects = defaultIds.map((id) => vertices[id])
      context.commit('SAVE_DEFAULT_OBJECTS', defaultObjects)
    }
  }

  if (resp.focus) {
    const defaultIds = resp.focus.vertices

    if (defaultIds.length) {
      const defaultObjects = defaultIds.map((id) => vertices[id])
      context.commit('SAVE_DEFAULT_OBJECTS', defaultObjects)
    }
  }

  context.commit('SAVE_EDGES', edges)
  context.commit('SAVE_VERTICES', vertices)
  context.dispatch('updateObjectFocusedProperty')
  context.dispatch('updateVerticesIconPaths')
  context.dispatch('updateSubLayerIndicator')
}

const getVerticesAndEdges = async (context: VuexContext, queryParameters?: QueryParameters) => {
  const resp = await API.getVerticesAndEdges(queryParameters)
  if (resp) {
    parseVerticesAndEdges(resp, context)
  }
}

const getTopologyGraphs = async (context: VuexContext) => {
  const topologyGraphs = await API.getTopologyGraphs()
  context.commit('SAVE_TOPOLOGY_GRAPHS', topologyGraphs)
}

const getTopologyGraphByContainerAndNamespace = async (
  context: ContextWithState,
  { containerId, namespace }: Record<string, string>
) => {
  const topologyGraph = await API.getTopologyGraphByContainerAndNamespace(containerId, namespace)
  if (topologyGraph) {
    context.commit('SET_CONTAINER_AND_NAMESPACE', { container: containerId, namespace })
    parseVerticesAndEdges(topologyGraph, context)

    // save which ids have sublayers, to show indicator
    const idsWithSubLayers = topologyGraph.edges.map((edge) => edge.id.split('.')[0])
    context.commit('SAVE_IDS_WITH_SUBLAYERS', idsWithSubLayers)

    // set focus to the defaults
    context.dispatch('addFocusObjects', context.state.defaultObjects)
  }
}

const setSemanticZoomLevel = (context: ContextWithState, SML: number) => {
  context.commit('SET_SEMANTIC_ZOOM_LEVEL', SML)
  context.dispatch('getObjectDataByLevelAndFocus')
}

const getObjectDataByLevelAndFocus = async (context: ContextWithState) => {
  let resp: false | VerticesAndEdges

  const SZLRequest: SZLRequest = {
    semanticZoomLevel: context.state.semanticZoomLevel,
    verticesInFocus: context.state.focusObjects.map((obj) => obj.id)
  }

  if (context.state.selectedDisplay !== PowerGrid) {
    resp = await API.getNodesTopologyDataByLevelAndFocus(SZLRequest)
  } else {
    resp = await API.getPowerGridTopologyDataByLevelAndFocus(
      context.state.container,
      context.state.namespace,
      SZLRequest
    )
  }

  if (resp) {
    parseVerticesAndEdges(resp, context)
  }
}

const changeIcon = (context: ContextWithState, nodeIdIconKey: Record<string, string>) => {
  context.commit('UPDATE_NODE_ICONS', nodeIdIconKey)
  context.dispatch('updateVerticesIconPaths')
}

/**
 * Saves menu selections
 */

// d3, circle, etc.
const setSelectedView = (context: VuexContext, view: string) => {
  context.commit('SET_SELECTED_VIEW', view)
}

// linkd, powerdrid, etc.
const setSelectedDisplay = async (context: ContextWithState, display: string) => {
  context.commit('SET_SELECTED_DISPLAY', display)

  if (display === PowerGrid) {
    // get first available namespace items
    const powergridGraphs: TopologyGraphList = getters.getPowerGridGraphs(context.state)
    if (powergridGraphs.graphs && powergridGraphs.graphs.length) {
      const containerId = powergridGraphs.id
      const namespace = powergridGraphs.graphs[0].namespace
      await context.dispatch('getTopologyGraphByContainerAndNamespace', { containerId, namespace })
    }
  } else {
    await context.dispatch('getVerticesAndEdges')
    context.dispatch('addFocusObjects', context.state.defaultObjects)
  }
}

/**
 * Focus
 */
const addFocusObjects = (context: ContextWithState, objects: IdLabelProps[]) => {
  context.commit('ADD_FOCUS_OBJECTS', objects)
  context.dispatch('getObjectDataByLevelAndFocus')
}

const addFocusObject = (context: VuexContext, object: IdLabelProps) => {
  context.commit('ADD_FOCUS_OBJECT', object)
  context.dispatch('getObjectDataByLevelAndFocus')
}

const removeFocusObject = (context: VuexContext, nodeId: string) => {
  context.commit('REMOVE_FOCUS_OBJECT', nodeId)
  context.dispatch('getObjectDataByLevelAndFocus')
}

const highlightFocusedObjects = (context: ContextWithState, bool: boolean) => {
  context.commit('SET_HIGHLIGHT_FOCUSED_OBJECTS', bool)
}

/**
 * Left and right drawer states
 */
const openLeftDrawer = (context: VuexContext) => context.commit('SET_LEFT_DRAWER_OPEN', true)
const closeLeftDrawer = (context: VuexContext) => context.commit('SET_LEFT_DRAWER_OPEN', false)
const setRightDrawerState = (context: VuexContext, bool: boolean) => context.commit('SET_RIGHT_DRAWER_OPEN', bool)

/**
 * Modal state
 */
const setModalState = (context: VuexContext, bool: boolean) => context.commit('SET_MODAL_STATE', bool)

/**
 * Network graph custom property updates.
 * Run every time after parsing the vertices and edges.
 */

// prop for whether object is focused or not
const updateObjectFocusedProperty = (context: ContextWithState) => {
  const vertices = context.state.vertices
  const edges = context.state.edges
  const focusedIds = context.state.focusObjects.map((obj) => obj.id)

  for (const vertex of Object.values(vertices)) {
    if (focusedIds.includes(vertex.id)) {
      vertex.focused = true
    } else {
      vertex.focused = false
    }
  }

  for (const edge of Object.values(edges)) {
    if (focusedIds.includes(edge.target) && focusedIds.includes(edge.source)) {
      edge.focused = true
    } else {
      edge.focused = false
    }
  }

  context.commit('SAVE_VERTICES', vertices)
  context.commit('SAVE_EDGES', edges)
}

// icon path prop
const updateVerticesIconPaths = (context: ContextWithState) => {
  const vertices = context.state.vertices
  const nodeIcons = context.state.nodeIcons

  for (const [id, iconKey] of Object.entries(nodeIcons)) {
    if (vertices[id]) {
      vertices[id]['icon'] = iconKey
    }
  }

  context.commit('SAVE_VERTICES', vertices)
}

// prop for whether object has links to sublayer objects
const updateSubLayerIndicator = (context: ContextWithState) => {
  const idsWithSubLayers = context.state.idsWithSubLayers
  const vertices = context.state.vertices

  for (const id of idsWithSubLayers) {
    if (vertices[id]) {
      vertices[id]['hasSubLayer'] = true
    }
  }

  context.commit('SAVE_VERTICES', vertices)
}

export default {
  getVerticesAndEdges,
  setSemanticZoomLevel,
  setSelectedView,
  setSelectedDisplay,
  openLeftDrawer,
  closeLeftDrawer,
  setRightDrawerState,
  addFocusObject,
  addFocusObjects,
  getObjectDataByLevelAndFocus,
  removeFocusObject,
  highlightFocusedObjects,
  updateObjectFocusedProperty,
  setModalState,
  changeIcon,
  updateVerticesIconPaths,
  updateSubLayerIndicator,
  getTopologyGraphs,
  getTopologyGraphByContainerAndNamespace
}
