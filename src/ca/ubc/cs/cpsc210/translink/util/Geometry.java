package ca.ubc.cs.cpsc210.translink.util;

/**
 * Compute relationships between points, lines, and rectangles represented by LatLon objects
 */
public class Geometry {
    /**
     * Return true if the point is inside of, or on the boundary of, the rectangle formed by northWest and southeast
     * @param northWest         the coordinate of the north west corner of the rectangle
     * @param southEast         the coordinate of the south east corner of the rectangle
     * @param point             the point in question
     * @return                  true if the point is on the boundary or inside the rectangle
     */
    public static boolean rectangleContainsPoint(LatLon northWest, LatLon southEast, LatLon point) {
        double nwlat, nwlon, selat, selon, lat, lon;
        nwlat = northWest.getLatitude();
        nwlon = northWest.getLongitude();
        selat = southEast.getLatitude();
        selon = southEast.getLongitude();
        lat = point.getLatitude();
        lon = point.getLongitude();
        return between(selat, nwlat, lat) && between(nwlon, selon, lon);
    }

    /**
     * Return true if the rectangle intersects the line
     *
     * @param northWest         the coordinate of the north west corner of the rectangle
     * @param southEast         the coordinate of the south east corner of the rectangle
     * @param src               one end of the line in question
     * @param dst               the other end of the line in question
     * @return                  true if any point on the line is on the boundary or inside the rectangle
     */
    public static boolean rectangleIntersectsLine(LatLon northWest, LatLon southEast, LatLon src, LatLon dst) {
        double rectNorth, rectSouth, rectEast, rectWest;
        double p1NS, p1EW, p2NS, p2EW;

        rectNorth = northWest.getLatitude();
        rectSouth = southEast.getLatitude();
        rectEast = southEast.getLongitude();
        rectWest = northWest.getLongitude();

        p1NS = src.getLatitude();
        p1EW = src.getLongitude();
        p2NS = dst.getLatitude();
        p2EW = dst.getLongitude();

        // Optimization for the common case, where the line is completely to one side
        if ((p1NS > rectNorth && p2NS > rectNorth) ||
                (p1EW > rectEast  && p2EW > rectEast ) ||
                (p1NS < rectSouth && p2NS < rectSouth) ||
                (p1EW < rectWest  && p2EW < rectWest ))
            return false;

        // Is an endpoint inside the rectangle?
        if (Geometry.rectangleContainsPoint(northWest, southEast, src) ||
                Geometry.rectangleContainsPoint(northWest, southEast, dst))
            return true;

        // Degenerate case: line has degenerated to a point
        // Because it wsan't inside the rectangle, we need do no more
        if (p1NS == p2NS && p1EW == p2EW)
            return false;
        Point lineP1 = new Point(p1EW, p1NS);
        Point lineP2 = new Point(p2EW, p2NS);
        LineSegment line = new LineSegment(lineP1, lineP2);

        // Degenerate case: rectangle has degenerated to a point
        if (rectNorth == rectSouth && rectEast == rectWest) {
            Point degen = new Point(rectEast, rectNorth);

            // For intersection to occur, the three points must be colinear
            if (lineP1.turn(lineP2, degen) != 0)
                return false;

            return ((Geometry.between(p1EW, p2EW, rectEast)  || Geometry.between(p2EW, p1EW, rectEast)) &&
                    (Geometry.between(p1NS, p2NS, rectNorth) || Geometry.between(p2NS, p1NS, rectNorth)));
        }

        // Degenerate case: rectangle has degenerated to a line
        if (rectNorth == rectSouth || rectEast == rectWest)
        {
            // Either north and south are equal, or east and west are equal
            LineSegment degen = new LineSegment(new Point(rectEast, rectNorth), new Point(rectWest, rectSouth));
            return line.intersects(degen);
        }

        // Not simple, and not degenerate, so we have to do this the hard way
        LineSegment northBound = new LineSegment(new Point(rectWest, rectNorth), new Point(rectEast, rectNorth));
        LineSegment eastBound = new LineSegment(new Point(rectEast, rectNorth), new Point(rectEast, rectSouth));
        LineSegment southBound = new LineSegment(new Point(rectWest, rectSouth), new Point(rectEast, rectSouth));
        LineSegment westBound = new LineSegment(new Point(rectWest, rectNorth), new Point(rectWest, rectSouth));
        return (line.intersects(northBound) || line.intersects(eastBound) ||
                line.intersects(southBound) || line.intersects(westBound));
    }

    /**
     * A utility method that you might find helpful in implementing the two previous methods
     * Return true if x is >= lwb and <= upb
     * @param lwb      the lower boundary
     * @param upb      the upper boundary
     * @param x         the value in question
     * @return          true if x is >= lwb and <= upb
     */
    private static boolean between(double lwb, double upb, double x) {
        return lwb <= x && x <= upb;
    }

    /**
     * The Point class represents a well-ordered single point in 2D space.
     */
    private static class Point implements Comparable<Point>
    {
        /**
         * The x coordinate.
         */
        private double x;

        /**
         * The y coordinate.
         */
        private double y;

        /**
         * Creates a new Point with the given x and y coordinates.
         *
         * @param x the x coordinate.
         * @param y the y coordinate.
         */
        public Point(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        /**
         * Computes the direction in which you must turn (clockwise or
         * counterclockwise) if you are travelling from this point to dest, and
         * turn in the direction of the point turn.
         *
         * @param dest the destination, moving from this point.
         * @param turn the point to which you are turning.
         * @return 1 if the turn is counterclockwise, -1 if the turn is
         * clockwise, or 0 if the three points are colinear.
         */
        public int turn(Point dest, Point turn)
        {
            /*
	         * Consider vectors v1 = <this to dest>, and v2 = <this to turn>
	         * (i.e., v1 = <dest - this>, and v2 = <turn - this>).
	         * For now, assume both deltaX and deltaY for both v1 and v2 are
	         * greater than zero.
	         *
	         * If the slope of v2 is greater than the slope of v1, we turned
	         * counterclockwise.  If it is less, we turned clockwise; and, if
	         * equal, the three points are colinear.
	         * slope(v1) < slope(v2) <=>
	         * deltaY_v1 / deltaX_v1 < deltaY_v2 / deltaX_v2 <=>
	         * deltaY_v1 * deltaX_v2 < deltaY_v2 * deltaX_v1
	         * Let
	         *      lhs = deltaY_v1 * deltaX_v2
	         *      rhs = deltaY_v2 * deltaX_v1
	         * and we similarly have that the turn was clockwise iff lhs > rhs,
	         * and that the point are colinear iff lhs = rhs.
	         *
	         * The proof that
	         *      lhs < rhs <=> the turn is counterclockwise,
	         *      lhs > rhs <=> the turn is clockwise, and
	         *      lhs = rhs <=> the points are colinear,
	         * even if the deltaX and deltaY may be zero or negative follows
	         * from a brute-force approach to looking at all possible
	         * quadrants/axes on which the vectors could lie, and deciding
	         * whether slope(v1) need be greater or less than slope(v2) in
	         * those cases to result in a counterclockwise turn (the inequality
	         * sign will flip appropriately).
	         */
            double lhs = (dest.y - this.y) * (turn.x - this.x);
            double rhs = (turn.y - this.y) * (dest.x - this.x);
            if (lhs < rhs)
                return 1;
            else if (lhs > rhs)
                return -1;
            else
                return 0;
        }

        /**
         * Tests is this Point is equal to another Point. Two Points are equal
         * if their x and y coordinates are the same.
         *
         * @param o the object for which to test equality.
         * @return true if the two Points are the same, otherwise false.
         */
        public boolean equals(Object o)
        {
            if (o instanceof Point) {
                Point other = (Point)o;
                return ((this.x == other.x) && (this.y == other.y));
            }
            return false;
        }

        /**
         * Returns a hash code for this Point.
         *
         * @return a hash code for this Point, constructed by combining the
         * hash codes of the two coordinates.
         */
        public int hashCode()
        {
            Double x = new Double(this.x);
            Double y = new Double(this.y);
            return ((x.hashCode() & 0xF0F0F0F0) | (y.hashCode() & 0x0F0F0F0F));
        }

        /**
         * Compares this Point with the specified Point for order. These
         * objects are sorted first by their x coordinate, and sub-sorted by
         * their y coordinate.
         *
         * @param o the Point to which to compare this object.
         * @return a negative number if this Point is smaller than the given
         * Point, a positive number if it is larger, and 0 if they are equal.
         */
        public int compareTo(Point o)
        {
            double diff = this.x - o.x;
            if (diff == 0.0)
                diff = this.y - o.y;
            if (diff < 0.0)
                return -1;
            else if (diff > 0.0)
                return 1;
            else
                return 0;
        }
    }

    /**
     * The LineSegment class represents a line segment between two points in
     * 2D space.
     */
    private static class LineSegment
    {
        /**
         * One endpoint of the line.  The endpoints are sorted, so this point
         * has an x coordinate guaranteed to be less than or equal to the x
         * coordinate of the other point.  If the x coordinates are equal, this
         * point has a y coordinate less than the y coordinate of the other
         * point.
         */
        private Point p1;

        /**
         * The other endpoint of the line segment.
         */
        private Point p2;

        /**
         * Creates a new line segment between the two given endpoints.
         *
         * @param a one endpoint for the line segment.
         * @param b the other endpoint, distinct from a.
         * @throws IllegalArgumentException if the Points are equal.
         */
        public LineSegment(Point a, Point b)
        {
	    /*
	     * Assign the further left (or further down, if tied) point as
	     * p1
	     */
            int cmp = a.compareTo(b);
            if (cmp < 0) {
                this.p1 = a;
                this.p2 = b;
            }
            else if (cmp > 0) {
                this.p1 = b;
                this.p2 = a;
            }
            else
                throw new IllegalArgumentException("Equal points");
        }

        /**
         * Returns whether or not there is any intersection between this line
         * segment and the given line segment.
         *
         * @param other the other line segment.
         * @return <code>true</code> if the two line segments intersect at one
         * or more points, <code>false</code> otherwise.
         */
        public boolean intersects(LineSegment other)
        {
	        /*
	         * Consider this segment to be an infinite-length line; ensure
	         * that the two points of other segment lie on opposite sides of
	         * the line by checking the direction you turn while travelling
	         * from this segment's p1 to this segment's p2, then turning
	         * towards the other segment's endpoints.  Repeat the procedure by
	         * extending the other segment to an infinite-length line, and
	         * checking the two points of this segment.
	         */
            return
                    LineSegment.orientation(this.p1, this.p2, other.p1) *
                            LineSegment.orientation(this.p1, this.p2, other.p2) <= 0 &&
                            LineSegment.orientation(other.p1, other.p2, this.p1) *
                                    LineSegment.orientation(other.p1, other.p2, this.p2) <= 0;
        }

        /**
         * Computes the direction in which you must turn (clockwise or
         * counterclockwise) if you are travelling from p0 to p1, and turn in
         * the direction of the point p2. This function expects that p0
         * compares strictly less than p1.
         *
         * @param p0 the origin.
         * @param p1 the original destination.
         * @param p2 the point to which you are turning.
         * @return 1 if the turn is counterclockwise or -1 if the turn is
         * clockwise. If the three points are colinear, return:
         * 0 if p2 is between p0 and p1, or if p2 is equal to either of those
         * points;
         * 1 if p1 is in the middle; or,
         * -1 if p0 is in the middle.
         * @throws IllegalArgumentException if p1 is not strictly greater than
         * p0.
         */
        private static int orientation(Point p0, Point p1, Point p2)
        {
            if (p0.compareTo(p1) >= 0)
                throw new IllegalArgumentException("Invalid orientation " +
                        "points");

            int ret = p0.turn(p1, p2);
            if (ret != 0)
                return ret;

	        /*
	         * The points are colinear.  Arbitrary returns for when points are
	         * colinear correct for degenerate intersection cases, as per
	         * Dr. Donald Simon's notes titled "Computational Geometry"
	         * http://www.mathcs.duq.edu/simon/Fall06/cs300notes3.html
	         * Retrieved 3 May 2010.
	         */
            int p2p0 = p2.compareTo(p0);
            if (p2p0 >= 0 && p2.compareTo(p1) <= 0)
                return 0;
            else if (p2p0 < 0)
                return -1;
            else
                return 1;
        }
    }
}