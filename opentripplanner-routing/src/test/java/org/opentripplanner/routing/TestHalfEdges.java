/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing;

import static org.opentripplanner.common.IterableLibrary.cast;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.DirectEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.util.TestUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;

public class TestHalfEdges extends TestCase {

    Graph graph;

    private TurnVertex top;

    private TurnVertex bottom;

    private TurnVertex left;

    private TurnVertex right;

    private TurnVertex leftBack;

    private TurnVertex rightBack;

    private Vertex brOut;

    private Vertex trOut;

    private TransitStop station1;
    private TransitStop station2;

    public LineString createGeometry(Vertex a, Vertex b) {

        GeometryFactory factory = new GeometryFactory();
        Coordinate[] cs = new Coordinate[2];
        cs[0] = a.getCoordinate();
        cs[1] = b.getCoordinate();
        return factory.createLineString(cs);
    }

    public void setUp() {
        graph = new Graph();
        // a 0.1 degree x 0.1 degree square
        top = new TurnVertex("top", GeometryUtils.makeLineString(-74.01, 40.01, -74.0, 40.01), "top", 1500, false, null);
        bottom = new TurnVertex("bottom", GeometryUtils.makeLineString(-74.01, 40.0, -74.0, 40.0), "bottom", 1500, false, null);
        left = new TurnVertex("left", GeometryUtils.makeLineString(-74.01, 40.0, -74.01, 40.01), "left", 1500, false, null);
        right = new TurnVertex("right", GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.01), "right", 1500, false, null);
        
        TurnVertex topBack = new TurnVertex("topBack", GeometryUtils.makeLineString(-74.0, 40.01, -74.01, 40.01), "topBack", 1500, true, null);
        TurnVertex bottomBack = new TurnVertex("bottomBack", GeometryUtils.makeLineString(-74.0, 40.0, -74.01, 40.0), "bottomBack", 1500, true, null);
        leftBack = new TurnVertex("leftBack", GeometryUtils.makeLineString(-74.01, 40.01, -74.01, 40.0), "leftBack", 1500, true, null);
        rightBack = new TurnVertex("rightBack", GeometryUtils.makeLineString(-74.0, 40.01, -74.0, 40.0), "rightBack", 1500, true, null);

        right.setPermission(StreetTraversalPermission.PEDESTRIAN);
        
        graph.addVertex(top);
        graph.addVertex(bottom);
        graph.addVertex(left);
        graph.addVertex(right);
        
        graph.addVertex(topBack);
        graph.addVertex(bottomBack);
        graph.addVertex(leftBack);
        graph.addVertex(rightBack);
        
        IntersectionVertex tlIn = (IntersectionVertex) graph.addVertex(new IntersectionVertex("tl in", -74.01, 40.01));
        IntersectionVertex trIn = (IntersectionVertex) graph.addVertex(new IntersectionVertex("tr in", -74.0, 40.01));
        IntersectionVertex blIn = (IntersectionVertex) graph.addVertex(new IntersectionVertex("bl in", -74.0, 40.0));
        IntersectionVertex brIn = (IntersectionVertex) graph.addVertex(new IntersectionVertex("br in", -74.01, 40.0));

        Vertex tlOut = graph.addVertex(new IntersectionVertex("tl out", -74.01, 40.01));
        trOut = graph.addVertex(new IntersectionVertex("tr out", -74.0, 40.01));
        Vertex blOut = graph.addVertex(new IntersectionVertex("bl out", -74.0, 40.0));
        brOut = graph.addVertex(new IntersectionVertex("br out", -74.01, 40.0));
        
        graph.addVerticesFromEdge(new FreeEdge(tlOut, top));
        graph.addVerticesFromEdge(new FreeEdge(tlOut, leftBack));
        
        graph.addVerticesFromEdge(new FreeEdge(trOut, topBack));
        graph.addVerticesFromEdge(new FreeEdge(trOut, rightBack));
        
        graph.addVerticesFromEdge(new FreeEdge(blOut, bottom));
        graph.addVerticesFromEdge(new FreeEdge(blOut, left));
        
        graph.addVerticesFromEdge(new FreeEdge(brOut, bottomBack));
        graph.addVerticesFromEdge(new FreeEdge(brOut, right));
        
        graph.addVerticesFromEdge(new OutEdge(topBack, tlIn));
        graph.addVerticesFromEdge(new OutEdge(left, tlIn));
        
        graph.addVerticesFromEdge(new OutEdge(top, trIn));
        graph.addVerticesFromEdge(new OutEdge(right, trIn));
        
        graph.addVerticesFromEdge(new OutEdge(bottomBack, blIn));
        graph.addVerticesFromEdge(new OutEdge(leftBack, blIn));
        
        graph.addVerticesFromEdge(new OutEdge(bottom, brIn));
        graph.addVerticesFromEdge(new OutEdge(rightBack, brIn));      
        
        graph.addVerticesFromEdge(new TurnEdge(top, rightBack));
        graph.addVerticesFromEdge(new TurnEdge(rightBack, bottomBack));
        graph.addVerticesFromEdge(new TurnEdge(bottomBack, left));
        graph.addVerticesFromEdge(new TurnEdge(left, top));
        
        graph.addVerticesFromEdge(new TurnEdge(topBack, leftBack));
        graph.addVerticesFromEdge(new TurnEdge(leftBack, bottom));
        graph.addVerticesFromEdge(new TurnEdge(bottom, right));
        graph.addVerticesFromEdge(new TurnEdge(right, topBack));
        
        station1 = new TransitStop("transitVertex 1", -74.005, 40.0099999, "transitVertex 1", new AgencyAndId("A", "fleem station"), null);
        graph.addVertex(station1);
        station2 = new TransitStop("transitVertex 2", -74.002, 40.0099999, "transitVertex 2", new AgencyAndId("A", "morx station"), null);
        graph.addVertex(station2);
    }

    public void testHalfEdges() {
        // the shortest half-edge from the start vertex takes you down, but the shortest total path
        // is up and over
    	
    	TraverseOptions options = new TraverseOptions();

        HashSet<Edge> turns = new HashSet<Edge>(left.getOutgoing());
        turns.addAll(leftBack.getOutgoing());
        
        StreetLocation start = StreetLocation.createStreetLocation("start", "start", cast(turns,StreetEdge.class), new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()));

        HashSet<Edge> endTurns = new HashSet<Edge>(right.getOutgoing());
        endTurns.addAll(rightBack.getOutgoing());
        
        StreetLocation end = StreetLocation.createStreetLocation("end", "end", cast(endTurns,StreetEdge.class), new LinearLocation(0, 0.8).getCoordinate(right.getGeometry()));
        
        assertTrue(start.getX() < end.getX());
        assertTrue(start.getY() < end.getY());
        
        List<DirectEdge> extra = end.getExtra();
        
        assertEquals(12, extra.size());
        
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        ShortestPathTree spt1 = AStar.getShortestPathTree(graph, brOut, end, TestUtils.toSeconds(startTime),
                options);

        GraphPath pathBr = spt1.getPath(end, false);
        assertNotNull("There must be a path from br to end", pathBr);
        
        ShortestPathTree spt2 = AStar.getShortestPathTree(graph, trOut, end, TestUtils.toSeconds(startTime),
                options);

        GraphPath pathTr = spt2.getPath(end, false);
        assertNotNull("There must be a path from tr to end", pathTr);
        assertTrue("path from bottom to end must be longer than path from top to end", pathBr.getWeight() > pathTr.getWeight());
        
        ShortestPathTree spt = AStar.getShortestPathTree(graph, start, end, TestUtils.toSeconds(startTime),
                options);

        GraphPath path = spt.getPath(end, false);
        assertNotNull("There must be a path from start to end", path);

        // the bottom is not part of the shortest path
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("bottom"));
            assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
        }

        startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);

        options.setArriveBy(true);
        spt = AStar.getShortestPathTree(graph, start, end, TestUtils.toSeconds(startTime), 
                options);

        path = spt.getPath(start, false);
        assertNotNull("There must be a path from start to end (looking back)", path);

        // the bottom edge is not part of the shortest path
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("bottom"));
            assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
        }

        /* Now, the right edge is not bikeable.  But the user can walk their bike.  So here are some tests
         * that prove (a) that walking bikes works, but that (b) it is not preferred to riding a tiny bit longer.
         */
        
        options = new TraverseOptions(new TraverseModeSet(TraverseMode.BICYCLE));
        start = StreetLocation.createStreetLocation("start1", "start1", cast(turns,StreetEdge.class), new LinearLocation(0, 0.95).getCoordinate(top.getGeometry()));
        end = StreetLocation.createStreetLocation("end1", "end1", cast(turns,StreetEdge.class), new LinearLocation(0, 0.95).getCoordinate(bottom.getGeometry()));
        spt = AStar.getShortestPathTree(graph, start, end, TestUtils.toSeconds(startTime),
                options);

        path = spt.getPath(start, false);
        assertNotNull("There must be a path from top to bottom along the right", path);

        // the left edge is not part of the shortest path (even though the bike must be walked along the right)
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("left"));
            assertNotSame(s.getVertex(), graph.getVertex("leftBack"));
        }
        
        start = StreetLocation.createStreetLocation("start2", "start2", cast(turns,StreetEdge.class), new LinearLocation(0, 0.55).getCoordinate(top.getGeometry()));
        end = StreetLocation.createStreetLocation("end2", "end2", cast(turns,StreetEdge.class), new LinearLocation(0, 0.55).getCoordinate(bottom.getGeometry()));
        spt = AStar.getShortestPathTree(graph, start, end, TestUtils.toSeconds(startTime),
                options);

        path = spt.getPath(start, false);
        assertNotNull("There must be a path from top to bottom", path);

        // the right edge is not part of the shortest path, e
        for (State s : path.states) {
            assertNotSame(s.getVertex(), graph.getVertex("right"));
            assertNotSame(s.getVertex(), graph.getVertex("rightBack"));
        }
    }

    public void testStreetLocationFinder() {
        StreetVertexIndexServiceImpl finder = new StreetVertexIndexServiceImpl(graph);
        finder.setup();
        //test that the local stop finder finds stops
        assertTrue(finder.getLocalTransitStops(new Coordinate(-74.005000001, 40.01), 100).size() > 0);

        //test that the closest vertex finder returns the closest vertex
        StreetLocation some = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.00, 40.00), null, null);
        assertNotNull(some);
        assertTrue("wheelchair accessibility is correctly set (vertices)", some.isWheelchairAccessible());
        
        //test that the closest vertex finder correctly splits streets
        StreetLocation start = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.01, 40.004), null, null);
        assertNotNull(start);
        assertTrue("wheelchair accessibility is correctly set (splitting)", start.isWheelchairAccessible());

        List<DirectEdge> extras = start.getExtra();
        assertEquals(10, extras.size());
        
        TraverseOptions biking = new TraverseOptions(new TraverseModeSet(TraverseMode.BICYCLE));
        StreetLocation end = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.0, 40.008), null, biking);
        assertNotNull(end);
        
        extras = end.getExtra();
        assertEquals(10, extras.size());

        // test that the closest vertex finder also adds an edge to transit
        // stops (if you are really close to the transit stop relative to the
        // street)
        StreetLocation location = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.004999, 40.00999), null, new TraverseOptions());
        assertTrue(location.isWheelchairAccessible());
        boolean found = false;
        for (Edge extra : location.getExtra()) {
        	if (extra instanceof FreeEdge && ((FreeEdge)extra).getToVertex().equals(station1)) {
        		found = true;
        	}
        }
        assertTrue(found);

        // test that it is possible to travel between two splits on the same street
        TraverseOptions walking = new TraverseOptions(TraverseMode.WALK);
        start = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.0, 40.004), null, walking);
        end = (StreetLocation) finder.getClosestVertex(new Coordinate(-74.0, 40.008), null, walking,
                start.getExtra());
        assertNotNull(end);
        ShortestPathTree spt = AStar.getShortestPathTree(graph, start, end, 0, walking);
        GraphPath path = spt.getPath(end, false);
        for (State s : path.states) {
            assertFalse(s.getVertex() == top);
        }
    }
    
    public void testNetworkLinker() {
        int numVerticesBefore = graph.getVertices().size();
        NetworkLinker nl = new NetworkLinker(graph);
        nl.createLinkage();
        int numVerticesAfter = graph.getVertices().size();
        assertEquals (6, numVerticesAfter - numVerticesBefore);
        Collection<Edge> outgoing = station1.getOutgoing();
        assertTrue(outgoing.size() == 2);
        DirectEdge edge = (DirectEdge) outgoing.iterator().next();
        
        Vertex midpoint = edge.getToVertex();
        assertTrue(Math.abs(midpoint.getCoordinate().y - 40.01) < 0.00000001);
        
        outgoing = station2.getOutgoing();
        assertTrue(outgoing.size() == 2);
        edge = (DirectEdge) outgoing.iterator().next();
        
        Vertex station2point = edge.getToVertex();
        assertTrue(Math.abs(station2point.getCoordinate().x - -74.002) < 0.00000001);
        
    }
}
