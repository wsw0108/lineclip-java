package org.maptalks.lineclip;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.List;

public class Clipper {

    // intersect a segment against one of the 4 lines that make up the bbox
    private static Coordinate intersect(Coordinate a, Coordinate b, int edge, double[] bbox) {
        double ax = a.getOrdinate(Coordinate.X);
        double ay = a.getOrdinate(Coordinate.Y);
        double bx = b.getOrdinate(Coordinate.X);
        double by = b.getOrdinate(Coordinate.Y);
        if ((edge & 8) != 0) {
            return new Coordinate(ax + (bx - ax) * (bbox[3] - ay) / (by - ay), bbox[3]);
        } else if ((edge & 4) != 0) {
            return new Coordinate(ax + (bx - ax) * (bbox[1] - ay) / (by - ay), bbox[1]);
        } else if ((edge & 2) != 0) {
            return new Coordinate(bbox[2], ay + (by - ay) * (bbox[2] - ax) / (bx - ax));
        } else if ((edge & 1) != 0) {
            return new Coordinate(bbox[0], ay + (by - ay) * (bbox[0] - ax) / (bx - ax));
        }
        return null;
    }

    // bit code reflects the point position relative to the bbox:
    //         left  mid  right
    //    top  1001  1000  1010
    //    mid  0001  0000  0010
    // bottom  0101  0100  0110
    private static int bitCode(Coordinate p, double[] bbox) {
        int code = 0;

        double x = p.getOrdinate(Coordinate.X);
        double y = p.getOrdinate(Coordinate.Y);

        if (x < bbox[0]) {
            // left
            code |= 1;
        } else if (x > bbox[2]) {
            // right
            code |= 2;
        }

        if (y < bbox[1]) {
            // bottom
            code |= 4;
        } else if (y > bbox[3]) {
            // top
            code |= 8;
        }

        return code;
    }

    // Cohen-Sutherland line clipping algorithm, adapted to efficiently
    // handle polylines rather than just segments
    public Coordinate[][] clipPolyline(Coordinate[] points, double[] bbox) {
        int len = points.length;
        int codeA = bitCode(points[0], bbox);
        List<Coordinate> part = new ArrayList<>();
        int i;
        Coordinate a, b;
        int codeB, lastCode;
        List<List<Coordinate>> result = new ArrayList<>();

        for (i = 1; i < len; i++) {
            a = points[i - 1];
            b = points[i];
            codeB = lastCode = bitCode(b, bbox);

            while (true) {

                if ((codeA | codeB) == 0) {
                    // accept
                    part.add(a);

                    if (codeB != lastCode) {
                        // segment went outside
                        part.add(b);

                        if (i < len - 1) {
                            // start a new line
                            result.add(part);
                            part = new ArrayList<>();
                        }
                    } else if (i == len - 1) {
                        part.add(b);
                    }
                    break;

                } else if ((codeA & codeB) != 0) {
                    // trivial reject
                    break;

                } else if (codeA > 0) {
                    // a outside, intersect with clip edge
                    a = intersect(a, b, codeA, bbox);
                    codeA = bitCode(a, bbox);

                } else {
                    // b outside
                    b = intersect(a, b, codeB, bbox);
                    codeB = bitCode(b, bbox);
                }
            }

            codeA = lastCode;
        }

        if (!part.isEmpty()) {
            result.add(part);
        }

        Coordinate[][] lines = new Coordinate[result.size()][];
        for (i = 0; i < result.size(); i++) {
            List<Coordinate> coordinates = result.get(i);
            lines[i] = new Coordinate[coordinates.size()];
            coordinates.toArray(lines[i]);
        }
        return lines;
    }

    // Sutherland-Hodgeman polygon clipping algorithm
    public Coordinate[] clipPolygon(Coordinate[] points, double[] bbox) {
        List<Coordinate> result = new ArrayList<>();
        int edge;
        Coordinate prev;
        boolean prevInside;
        int i;
        Coordinate p;
        boolean inside;

        // clip against each side of the clip rectangle
        for (edge = 1; edge <= 8; edge *= 2) {
            result = new ArrayList<>();
            prev = points[points.length - 1];
            prevInside = ((bitCode(prev, bbox) & edge) == 0);

            for (i = 0; i < points.length; i++) {
                p = points[i];
                inside = ((bitCode(p, bbox) & edge) == 0);

                // if segment goes through the clip window, add an intersection
                if (inside != prevInside) {
                    result.add(intersect(prev, p, edge, bbox));
                }

                // add a point if it's inside
                if (inside) {
                    result.add(p);
                }

                prev = p;
                prevInside = inside;
            }

            points = result.toArray(new Coordinate[0]);

            if (points.length <= 0) {
                break;
            }
        }

        return result.toArray(new Coordinate[0]);
    }
}
