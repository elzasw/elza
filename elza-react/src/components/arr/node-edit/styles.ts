import { makeStyles } from "@fluentui/react-components";

export const useStyles = makeStyles({
  gridContainer: {
    minWidth: "300px",
    gridTemplateColumns: "1fr",
    "@container form-container (width > 500px)": {
      gridTemplateColumns: "repeat(2 ,1fr)",
    },
    "@container form-container (width > 750px)": {
      gridTemplateColumns: "repeat(3 ,1fr)",
    },
    "@container form-container (width > 1000px)": {
      gridTemplateColumns: "repeat(4 ,1fr)",
    },
    "@container form-container (width > 1250px)": {
      gridTemplateColumns: "repeat(5 ,1fr)",
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
  gridItem_0: {
    gridColumn: "1 / -1",
  },
  gridItem_1: {
    gridColumn: "span 1",
  },
  gridItem_2: {
    gridColumn: "span 1",
    "@container form-container (width > 500px)": {
      gridColumn: "span 2",
    },
  },
  gridItem_3: {
    gridColumn: "span 1",
    "@container form-container (width > 500px)": {
      gridColumn: "span 2",
    },
    "@container form-container (width > 750px)": {
      gridColumn: "span 3",
    },
  },
  gridItem_4: {
    gridColumn: "span 1",
    "@container form-container (width > 500px)": {
      gridColumn: "span 2",
    },
    "@container form-container (width > 750px)": {
      gridColumn: "span 3",
    },
    "@container form-container (width > 1000px)": {
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
});
