import { makeStyles } from "@fluentui/react-components";

export const useStyles = makeStyles({
  gridContainer: {
    minWidth: "300px",
    gridTemplateColumns: "1fr",
    "@container group-container (width > 500px)": {
      gridTemplateColumns: "repeat(2 ,1fr)",
    },
    "@container group-container (width > 750px)": {
      gridTemplateColumns: "repeat(3 ,1fr)",
    },
    "@container group-container (width > 1000px)": {
      gridTemplateColumns: "repeat(4 ,1fr)",
    },
    "@container group-container (width > 1250px)": {
      gridTemplateColumns: "repeat(5 ,1fr)",
    },
    "@container group-container (width > 1500px)": {
      gridTemplateColumns: "repeat(6 ,1fr)",
    },
    "@container group-container (width > 1750px)": {
      gridTemplateColumns: "repeat(7 ,1fr)",
    },
    "@container group-container (width > 2000px)": {
      gridTemplateColumns: "repeat(8 ,1fr)",
    },
  },
  gridContainerCompact: {
    minWidth: "300px",
    gridTemplateColumns: "1fr",
    "@container group-container (width > 400px)": {
      gridTemplateColumns: "repeat(2 ,1fr)",
    },
    "@container group-container (width > 600px)": {
      gridTemplateColumns: "repeat(3 ,1fr)",
    },
    "@container group-container (width > 800px)": {
      gridTemplateColumns: "repeat(4 ,1fr)",
    },
    "@container group-container (width > 1000px)": {
      gridTemplateColumns: "repeat(5 ,1fr)",
    },
    "@container group-container (width > 1200px)": {
      gridTemplateColumns: "repeat(6 ,1fr)",
    },
    "@container group-container (width > 1400px)": {
      gridTemplateColumns: "repeat(7 ,1fr)",
    },
    "@container group-container (width > 1600px)": {
      gridTemplateColumns: "repeat(8 ,1fr)",
    },
    "@container group-container (width > 1800px)": {
      gridTemplateColumns: "repeat(9 ,1fr)",
    },
    "@container group-container (width > 2000px)": {
      gridTemplateColumns: "repeat(10 ,1fr)",
    },
  },
  gridItem: {
    verticalAlign: "top",
    display: "flex",
    flexWrap: "wrap",
    margin: "8px",
    marginTop: "0",
    flexDirection: "column",
  },
  gridItemCompact: {
    verticalAlign: "top",
    display: "flex",
    flexWrap: "wrap",
    margin: "4px",
    marginTop: "0",
    flexDirection: "column",
  },
  gridItem_0: {
    gridColumn: "1 / -1",
  },
  gridItem_1: {
    gridColumn: "span 1",
  },
  gridItem_2: {
    gridColumn: "span 1",
    "@container group-container (width > 500px)": {
      gridColumn: "span 2",
    },
  },
  gridItem_3: {
    gridColumn: "span 1",
    "@container group-container (width > 500px)": {
      gridColumn: "span 2",
    },
    "@container group-container (width > 750px)": {
      gridColumn: "span 3",
    },
  },
  gridItem_4: {
    gridColumn: "span 1",
    "@container group-container (width > 500px)": {
      gridColumn: "span 2",
    },
    "@container group-container (width > 750px)": {
      gridColumn: "span 3",
    },
    "@container group-container (width > 1000px)": {
      gridColumn: "span 4",
    },
  },
  groupWrapper: {
    breakInside: "avoid",
  },
  descItemTypeTitle: {
    "& .actions .hidable-button": {
      visibility: "hidden",
    },
    "&:hover .actions .hidable-button": {
      visibility: "visible",
    },
  },
});
