import { GeometryCollection, Point, Polygon, LineString, Geometry, MultiPoint, MultiLineString, MultiPolygon } from 'ol/geom';
import WKT from 'ol/format/WKT';

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

export function getGeometryFormatData(geometry: Geometry): { coordinateCount: number; objectCount: number } {
  const formatData = { coordinateCount: 0, objectCount: 0 };

  if (isGeometryCollection(geometry)) {
    geometry.getGeometries().forEach((part) => {
      const { coordinateCount, objectCount } = getGeometryFormatData(part);
      formatData.coordinateCount += coordinateCount;
      formatData.objectCount += objectCount;
    });
  } else if (isPoint(geometry)) {
    formatData.coordinateCount += 1;
    formatData.objectCount += 1;
  } else if (isMultiPoint(geometry)) {
    const coordinates = geometry.getCoordinates();
    formatData.coordinateCount += coordinates.length;
    formatData.objectCount += coordinates.length;
  } else if (isLineString(geometry)) {
    formatData.coordinateCount += geometry.getCoordinates().length;
    formatData.objectCount += 1;
  } else if (isMultiLineString(geometry)) {
    geometry.getCoordinates().forEach((coordinate) => {
      formatData.coordinateCount += coordinate.length;
      formatData.objectCount += 1;
    });
  } else if (isPolygon(geometry)) {
    geometry.getCoordinates().forEach((polygonPart) => {
      formatData.coordinateCount += polygonPart.length;
    });
    formatData.objectCount += 1;
  } else if (isMultiPolygon(geometry)) {
    geometry.getCoordinates().forEach((polygon) => {
      polygon.forEach((polygonPart) => {
        formatData.coordinateCount += polygonPart.length;
      });
      formatData.objectCount += 1;
    });
  }

  return formatData;
}

export function parseCoordinateSummary(value: string): { geometryType: string; coordinateCount: number; objectCount: number } | null {
  try {
    const geometry = new WKT().readGeometry(value);
    return { geometryType: geometry.getType(), ...getGeometryFormatData(geometry) };
  } catch {
    return null;
  }
}
