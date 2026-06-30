import { makeStyles } from "@fluentui/react-components";
import { GRID_BREAKPOINTS, gridBreakpointsToStyles } from "./gridBreakpoints";

export const useStyles = makeStyles({
  groupWrapper: {
    padding: "4px",
    containerName: "group-container",
    containerType: "inline-size",
  },
  groupWrapperCompact: {
    padding: "4px 4px",
    containerName: "group-container",
    containerType: "inline-size",
  },
  groupLabel: {
    opacity: 0.5,
    fontWeight: "bold",
    fontSize: "0.6rem",
    padding: "0 4px",
  },
  gridContainer: {
    display: "grid",
    minWidth: "300px",
    gridTemplateColumns: "1fr",
    ...gridBreakpointsToStyles(GRID_BREAKPOINTS),
  },
  gridContainerCard: {
    padding: "8px",
    background: "var(--shade-0)",
    borderRadius: "8px",
    boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
  },
  gridContainerCompact: {
    padding: "4px 8px",
    background: "var(--shade-0)",
    borderRadius: "4px",
    boxShadow: "0 1px 5px #0003, 0px 5px 5px #0001",
  },
  gridItem: {
    verticalAlign: "top",
    display: "flex",
    flexWrap: "wrap",
    margin: "8px",
    marginTop: "0",
    flexDirection: "column",
    outlineColor: "transparent",
    outlineOffset: "4px",
    borderRadius: "1px",
    transition: "outline-color 300ms ease-out",
  },
  gridItemCompact: {
    verticalAlign: "top",
    display: "flex",
    flexWrap: "wrap",
    margin: "4px",
    marginTop: "0",
    flexDirection: "column",
    outlineColor: "transparent",
    outlineOffset: "4px",
    borderRadius: "1px",
    transition: "outline-color 300ms ease-out",
  },
  gridItem_0: {
    gridColumn: "1 / -1",
  },
  gridItem_1: {
    gridColumn: "span 1",
  },
  gridItem_2: {
    gridColumn: "span 1",
    "@container group-container (width > 600px)": {
      gridColumn: "span 2",
    },
  },
  gridItem_3: {
    gridColumn: "span 1",
    "@container group-container (width > 600px)": {
      gridColumn: "span 2",
    },
    "@container group-container (width > 1000px)": {
      gridColumn: "span 3",
    },
  },
  gridItem_4: {
    gridColumn: "span 1",
    "@container group-container (width > 600px)": {
      gridColumn: "span 2",
    },
    "@container group-container (width > 1000px)": {
      gridColumn: "span 4",
    },
  },
  descItemTypeTitle: {
    "& .actions .hidable-button": {
      visibility: "hidden",
    },
    "&:hover .actions .hidable-button": {
      visibility: "visible",
    },
  },
  nodeEditForm: {
    background: "var(--shade-1)",
    containerName: "form-container",
    containerType: "inline-size",
    position: "relative",
  },
  spinnerPadding: {
    padding: "50px",
  },
  toolbarSticky: {
    position: "sticky",
    top: 0,
    zIndex: 100,
    padding: "8px",
    background: "var(--shade-1)",
    display: "flex",
    alignItems: "center",
  },
  toolbarMain: {
    flex: 1,
    minWidth: 0,
    overflow: "hidden",
    paddingRight: "8px",
  },
  toolbarFlexShrink: {
    flexShrink: 0,
  },
  toolbarScenarioButton: {
    whiteSpace: "nowrap",
    marginRight: "4px",
    flexShrink: 0,
  },
  toolbarOverflowButton: {
    flexShrink: 0,
    whiteSpace: "nowrap",
  },
  toolbarOverflowInner: {
    display: "flex",
    flex: 0,
  },
  fundDataGridPopover: {
    minWidth: "300px",
    maxWidth: "600px",
    padding: "8px",
  },
  addDescItemButton: {
    borderTopStyle: "dashed",
    borderRightStyle: "dashed",
    borderBottomStyle: "dashed",
    borderLeftStyle: "dashed",
    color: "#666",
    marginTop: "2px",
    marginBottom: "2px",
  },
});
