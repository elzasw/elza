import { GeometryCollection, Point, Polygon, LineString, Geometry, MultiPoint, MultiLineString, MultiPolygon } from 'ol/geom';

export function isPoint(geometry: Geometry): geometry is Point {
  return geometry.getType() === "Point";
}

export function isMultiPoint(geometry: Geometry): geometry is MultiPoint {
  return geometry.getType() === "MultiPoint";
}

export function isLineString(geometry: Geometry): geometry is LineString {
  return geometry.getType() === "LineString";
}

export function isMultiLineString(geometry: Geometry): geometry is MultiLineString {
  return geometry.getType() === "MultiLineString";
}

export function isPolygon(geometry: Geometry): geometry is Polygon {
  return geometry.getType() === "Polygon";
}

export function isMultiPolygon(geometry: Geometry): geometry is MultiPolygon {
  return geometry.getType() === "MultiPolygon";
}

export function isGeometryCollection(geometry: Geometry): geometry is GeometryCollection {
  return geometry.getType() === "GeometryCollection";
}
