package org.maptalks.lineclip;

import static org.hamcrest.CoreMatchers.is;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.junit.Assert;
import org.junit.Test;

public class ClipperTest {
    @Test
    public void testClipPolyline() {
        Clipper clipper = new Clipper();

        CoordinateSequence input;
        Coordinate[] points;
        double[] bbox;
        Coordinate[][] result;
        double[][] expecteds;

        // clips line
        input = new PackedCoordinateSequence.Double(
            new double[]{
                -10, 10,
                10, 10,
                10, -10,
                20, -10,
                20, 10,
                40, 10,
                40, 20,
                20, 20,
                20, 40,
                10, 40,
                10, 20,
                5, 20,
                -10, 20
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{0, 0, 30, 30};
        result = clipper.clipPolyline(points, bbox);
        expecteds = new double[][]{
            new double[]{
                0, 10,
                10, 10,
                10, 0
            },
            new double[]{
                20, 0,
                20, 10,
                30, 10
            },
            new double[]{
                30, 20,
                20, 20,
                20, 30
            },
            new double[]{
                10, 30,
                10, 20,
                5, 20,
                0, 20
            }
        };
        Assert.assertThat(result.length, is(4));
        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(new PackedCoordinateSequence.Double(expecteds[i], 2).toCoordinateArray(), result[i]);
        }

        // clips line crossing through many times
        input = new PackedCoordinateSequence.Double(
            new double[]{
                10, -10,
                10, 30,
                20, 30,
                20, -10
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{0, 0, 20, 20};
        result = clipper.clipPolyline(points, bbox);
        expecteds = new double[][]{
            new double[]{
                10, 0,
                10, 20
            },
            new double[]{
                20, 20,
                20, 0
            }
        };
        Assert.assertThat(result.length, is(2));
        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(new PackedCoordinateSequence.Double(expecteds[i], 2).toCoordinateArray(), result[i]);
        }

        // clips floating point lines
        input = new PackedCoordinateSequence.Double(
            new double[]{
                -86.66015624999999, 42.22851735620852,
                -81.474609375, 38.51378825951165,
                -85.517578125, 37.125286284966776,
                -85.8251953125, 38.95940879245423,
                -90.087890625, 39.53793974517628,
                -91.93359375, 42.32606244456202,
                -86.66015624999999, 42.22851735620852
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{-91.93359375, 42.29356419217009, -91.7578125, 42.42345651793831};
        result = clipper.clipPolyline(points, bbox);
        expecteds = new double[][]{
            new double[]{
                -91.91208030440808, 42.29356419217009,
                -91.93359375, 42.32606244456202,
                -91.7578125, 42.3228109416169
            }
        };
        Assert.assertThat(result.length, is(1));
        for (int i = 0; i < result.length; i++) {
            Assert.assertArrayEquals(new PackedCoordinateSequence.Double(expecteds[i], 2).toCoordinateArray(), result[i]);
        }

        // preserves line if no protrusions exist
        input = new PackedCoordinateSequence.Double(
            new double[]{
                1, 1,
                2, 2,
                3, 3
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{0, 0, 30, 20};
        result = clipper.clipPolyline(points, bbox);
        Assert.assertThat(result.length, is(1));
        Assert.assertArrayEquals(points, result[0]);

        // clips without leaving empty parts
        input = new PackedCoordinateSequence.Double(
            new double[]{
                40, 40,
                50, 50
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{0, 0, 30, 30};
        result = clipper.clipPolyline(points, bbox);
        Assert.assertThat(result.length, is(0));
    }

    @Test
    public void testClipPolygon() {
        Clipper clipper = new Clipper();

        CoordinateSequence input;
        Coordinate[] points;
        double[] bbox;
        Coordinate[] result;
        double[] expecteds;

        // clips polygon
        input = new PackedCoordinateSequence.Double(
            new double[]{
                -10, 10,
                0, 10,
                10, 10,
                10, 5,
                10, -5,
                10, -10,
                20, -10,
                20, 10,
                40, 10,
                40, 20,
                20, 20,
                20, 40,
                10, 40,
                10, 20,
                5, 20,
                -10, 20
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{0, 0, 30, 30};
        result = clipper.clipPolygon(points, bbox);
        expecteds = new double[]{
            0, 10,
            0, 10,
            10, 10,
            10, 5,
            10, 0,
            20, 0,
            20, 10,
            30, 10,
            30, 20,
            20, 20,
            20, 30,
            10, 30,
            10, 20,
            5, 20,
            0, 20
        };
        Assert.assertArrayEquals(new PackedCoordinateSequence.Double(expecteds, 2).toCoordinateArray(), result);

        // still works when polygon never crosses bbox
        input = new PackedCoordinateSequence.Double(
            new double[]{
                3, 3,
                5, 3,
                5, 5,
                3, 5,
                3, 3
            },
            2
        );
        points = input.toCoordinateArray();
        bbox = new double[]{0, 0, 2, 2};
        result = clipper.clipPolygon(points, bbox);
        Assert.assertThat(result.length, is(0));
    }
}
