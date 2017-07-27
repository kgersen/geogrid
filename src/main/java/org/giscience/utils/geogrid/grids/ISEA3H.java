/*
 * Copyright (C) 2017 Franz-Benjamin Mocnik, Heidelberg University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.giscience.utils.geogrid.grids;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.giscience.utils.geogrid.generic.Tuple;
import org.giscience.utils.geogrid.geo.WGS84;
import org.giscience.utils.geogrid.geometry.FaceCoordinates;
import org.giscience.utils.geogrid.geometry.GeoCoordinates;
import org.giscience.utils.geogrid.geometry.GridCell;
import org.giscience.utils.geogrid.projections.ISEAProjection;

import java.util.*;

/**
 * ISEA Aperture 3 Hexagon (ISEA3H) Discrete Global Grid System (DGGS)
 *
 * The ISEA3H grid is constructed by using the icosahedron Snyder equal-area (ISEA) projection to map the surface of the
 * Earth to the icosahedron. Thereby, the orientation of the icosahedron is chosen such that the north and the south
 * poles are mapped to the edge midpoints of the icosahedron. The equator is thus mapped symmetrically. A grid
 * (aperture 3) is constructed on the icosahedron, and this grid is mapped back by the inverse projection to the Earth.
 *
 * The cells of the grid are identified by the resolution and their center points.
 *
 * The ISEA3H has been proposed by:
 *
 * Kevin Sahr, Denis White, and A. Jon Kimerling: Geodesic Discrete Global Grid Systems. Cartography and Geographic
 * Information Science, 30(2), 121–134, 2003.
 *
 * @author Franz-Benjamin Mocnik
 */
public class ISEA3H {
    private final ISEAProjection _projection = new ISEAProjection();
    private final int _resolution; // resolution - 1
    private final int _numberOfHexagonCells;
    private final int _numberOfPentagonCells = 12;
    private final double _l0; // length of the triangle base at resolution 0
    private final double _l; // length of the triangle base at the given resolution
    private final double _l2; // l / 2
    private final double _l3; // l / 3
    private final double _l6; // l / 6
    private final double _l23; // l * 2 / 3
    private final double _lsquare3; // l^2 / 3
    private final double _inverseSqrt3 = 1 / Math.sqrt(3);
    private final double _inverseSqrt3l;
    private final double _inverseSqrt3l2;
    private final double _triangleA; // l0 / 2 // half base
    private final double _triangleB; // 1/4 * (2 \sqrt{3} - 1) * l0 // distance center point to tip
    private final double _triangleC; // 1/4 * l0 // distance base to center point
    private final double _triangleBC; // \sqrt{3} / 2 * l0 // height

    public ISEA3H(int resolution) {
        this._projection.setOrientationSymmetricEquator();
        this._resolution = resolution - 1;
        int numberOfHexagonCells = 1;
        for (int i = 0; i < this._resolution; i++) {
            numberOfHexagonCells = 3 * numberOfHexagonCells + 1;
        }
        this._numberOfHexagonCells = 20 * numberOfHexagonCells;
        this._l0 = this._projection.lengthOfTriangleBase();
        this._l = Math.pow(this._inverseSqrt3, this._resolution) * this._l0;
        this._l2 = this._l / 2.;
        this._l3 = this._l / 3.;
        this._l6 = this._l / 6.;
        this._l23 = this._l * 2 / 3.;
        this._lsquare3 = Math.pow(this._l, 2) / 3.;
        this._inverseSqrt3l = this._inverseSqrt3 * this._l;
        this._inverseSqrt3l2 = this._inverseSqrt3l / 2.;
        this._triangleA = this._l0 / 2.;
        this._triangleB = 1 / 4. * (2 * Math.sqrt(3) - 1) * this._l0;
        this._triangleC = 1 / 4. * this._l0;
        this._triangleBC = Math.sqrt(3) / 2. * this._l0;
    }

    /**
     * @return diameter of a cell
     */
    public double diameterOfCellOnIcosahedron() {
        return this._l23;
    }

    /**
     * Returns the area of a hexagon cell. The cells should all have the same area by construction, because the ISEA
     * projection is equal-area.
     *
     * @return area of a hexagon cell
     */
    public double areaOfHexagonCell() {
        return WGS84.areaOfEarth / (this._numberOfHexagonCells + 5 / 6. * this._numberOfPentagonCells);
    }

    /**
     * Returns the area of a pentagon cell. The cells should all have the same area by construction, because the ISEA
     * projection is equal-area.
     *
     * @return area of a pentagoncell
     */
    public double areaOfPentagonCell() {
        return 5 / 6. * this.areaOfHexagonCell();
    }

    /**
     * @return number of hexagon cells
     */
    public int numberOfHexagonCells() {
        return this._numberOfHexagonCells;
    }

    /**
     * @return number of pentagon cells
     */
    public int numberOfPentagonCells() {
        return this._numberOfPentagonCells;
    }

    /**
     * Returns the grid cell for a given location
     *
     * @param lat latitude
     * @param lon longitude
     * @return corresponding grid cell
     * @throws Exception
     */
    public GridCell cellForLocation(double lat, double lon) throws Exception {
        return this.cellForLocation(new GeoCoordinates(lat, lon));
    }

    /**
     * Returns the grid cell for a given location
     *
     * @param c geographic coordinates
     * @return corresponding grid cell
     * @throws Exception
     */
    public GridCell cellForLocation(GeoCoordinates c) throws Exception {
        return new GridCell(this._resolution, this._projection.icosahedronToSphere(this.cellForLocation(this._projection.sphereToIcosahedron(c))));
    }

    /**
     * Returns the coordinates of the center of the corresponding grid cell for given coordinates in the face
     *
     * @param c face coordinates
     * @return face coordinates of the center of the corresponding grid cell
     * @throws Exception
     */
    public FaceCoordinates cellForLocation(FaceCoordinates c) throws Exception {
        double x = (this._resolution % 2 == 0) ? c.getX() : c.getY();
        double y = (this._resolution % 2 == 0) ? c.getY() : c.getX();
        double nxCenter = Math.round(x / (this._l2));
        double xCenter = nxCenter * this._l2;
        double nyCenter = Math.round(y / this._inverseSqrt3l);
        double yCenter = nyCenter * this._inverseSqrt3l;
        if (Math.abs(x - xCenter) <= this._l6) return this._faceCoordinatesSwapByResolution(c.getFace(), xCenter, yCenter);
        if (Math.abs(x - xCenter) > this._l3) return this._faceCoordinatesSwapByResolution(c.getFace(), (x > xCenter) ? xCenter + this._l2 : xCenter - this._l2, (y > yCenter) ? yCenter + this._inverseSqrt3l2 : yCenter - this._inverseSqrt3l2);
        else {
            FaceCoordinates cCandidate1 = this._faceCoordinatesSwapByResolution(c.getFace(), xCenter, yCenter);
            FaceCoordinates cCandidate2 = this._faceCoordinatesSwapByResolution(c.getFace(), (x > xCenter) ? xCenter + this._l2 : xCenter - this._l2, (y > yCenter) ? yCenter + this._inverseSqrt3l2 : yCenter - this._inverseSqrt3l2);
            return (c.distanceTo(cCandidate1) < c.distanceTo(cCandidate2)) ? cCandidate1 : cCandidate2;
        }
    }

    /**
     * Returns the grid cell for the centroid of a given geometry
     *
     * @param g geometry
     * @return corresponding grid cell
     * @throws Exception
     */
    public GridCell cellForCentroid(Geometry g) throws Exception {
        Coordinate c = g.getCentroid().getCoordinate();
        return this.cellForLocation(c.y, c.x);
    }

    /**
     * Returns cells that are inside the bounds, or at least very near. Note that, in fact, all cells are included,
     * whose center points are less than
     *
     * @param lat0
     * @param lat1
     * @param lon0
     * @param lon1
     * @return cells inside the bounds
     */
    public Collection<GridCell> cellsForBound(double lat0, double lat1, double lon0, double lon1) throws Exception {
        Set<GridCell> cells = new HashSet<>();
        for (int f = 1; f <= this._projection.numberOfFaces(); f++) cells.addAll(_cellsForBound(f, lat0, lat1, lon0, lon1));
        return cells;
    }

    private Tuple<Integer, Integer> _integerForFaceCoordinates(FaceCoordinates c) {
        double x = (this._coordinatesNotSwapped()) ? c.getX() : c.getY();
        double y = (this._coordinatesNotSwapped()) ? c.getY() : c.getX();
        int nx = (int)Math.round(x / this._l2);
        int ny = (int)Math.round(y / this._inverseSqrt3l - ((nx % 2 == 0) ? 0 : .5));
        return new Tuple(nx, ny);
    }

    private FaceCoordinates _cellCoordinatesForLocationAndFace(int face, GeoCoordinates c) throws Exception {
        return this.cellForLocation(this._projection.sphereToPlanesOfTheFacesOfTheIcosahedron(face, c));
    }

    private Collection<GridCell> _cellsForBound(int face, double lat0, double lat1, double lon0, double lon1) throws Exception {
        Set<GridCell> cells = new HashSet<>();

        // bounding box of face (triangle)
        double size1 = this._l0 / 3.;
        double size2 = this._l0 * this._inverseSqrt3 / 2.;
        double sizeX = this._coordinatesNotSwapped() ? size1 : size2;
        double sizeY = this._coordinatesNotSwapped() ? size2 : size1;

        // coordinates for vertices of bbox
        FaceCoordinates fc1 = this._cellCoordinatesForLocationAndFace(face, new GeoCoordinates(lat0, lon0));
        FaceCoordinates fc2 = this._cellCoordinatesForLocationAndFace(face, new GeoCoordinates(lat0, lon1));
        FaceCoordinates fc3 = this._cellCoordinatesForLocationAndFace(face, new GeoCoordinates(lat1, lon0));
        FaceCoordinates fc4 = this._cellCoordinatesForLocationAndFace(face, new GeoCoordinates(lat1, lon1));

        // find minimum and maximum values
        double xMin = Math.min(fc1.getX(), Math.min(fc2.getX(), Math.min(fc3.getX(), fc4.getX())));
        double yMin = Math.min(fc1.getY(), Math.min(fc2.getY(), Math.min(fc3.getY(), fc4.getY())));
        double xMax = Math.max(fc1.getX(), Math.max(fc2.getX(), Math.max(fc3.getX(), fc4.getX())));
        double yMax = Math.max(fc1.getY(), Math.max(fc2.getY(), Math.max(fc3.getY(), fc4.getY())));

        // check whether bbox intersects face
        double buffer = this._l;
        if (xMin - buffer > sizeX || xMax + buffer < -sizeX || yMin - buffer > sizeY || yMax + buffer < -sizeY) return cells;

        // compute cells
        Tuple<Integer, Integer> fcMinN = this._integerForFaceCoordinates(this.cellForLocation(new FaceCoordinates(face, xMin, yMin)));
        Tuple<Integer, Integer> fcMaxN = this._integerForFaceCoordinates(this.cellForLocation(new FaceCoordinates(face, xMax, yMax)));
        for (int nx = fcMinN._1 - 1; nx <= fcMaxN._1 + 1; nx++) {
            for (int ny = fcMinN._2 - 1; ny <= fcMaxN._2 + 1; ny++) {
                FaceCoordinates fc = this._getCoordinatesOfCenter(face, nx, ny);
                if (this._isCoordinatesInFace(fc)) {
                    cells.add(new GridCell(this._resolution, this._projection.icosahedronToSphere(fc)));
                }
            }
        }

        return cells;
    }

    /**
     * @param face face to compute the coordinates on
     * @param nx steps into the direction of the vertex of the hexagon
     * @param ny steps into the direction of the edge of the hexagon
     * @return coordinates on the face
     */
    private FaceCoordinates _getCoordinatesOfCenter(int face, int nx, int ny) {
        double x = nx * this._l2;
        double y = (ny + ((nx % 2 == 0) ? 0 : .5)) * this._inverseSqrt3l;
        return this._faceCoordinatesSwapByResolution(face, x, y);
    }

    private boolean _isCoordinatesInFace(FaceCoordinates fc) {
        double x = this._coordinatesNotSwapped() ? fc.getX() : fc.getY();
        double y = this._coordinatesNotSwapped() ? fc.getY() : fc.getX();

        // cell orientation
        short d;
        if (fc.getFace() <= 5 || fc.getFace() >= 11 && fc.getFace() <= 15) d = 1;
        else d = -1;

        // test whether coordinate is left of the triangle, right of the triangle, or below the triangle
        if (x * this._triangleBC / this._triangleA + this._triangleB < d * y) return false;
        if (- x * this._triangleBC / this._triangleA + this._triangleB < d * y) return false;
        if (d * y < - this._triangleC) return false;

        return true;
    }

    private FaceCoordinates _faceCoordinatesSwapByResolution(int face, double x, double y) {
        return new FaceCoordinates(face, this._coordinatesNotSwapped() ? x : y, this._coordinatesNotSwapped() ? y : x);
    }

    private boolean _coordinatesNotSwapped() {
        return this._resolution % 2 == 0;
    }
}