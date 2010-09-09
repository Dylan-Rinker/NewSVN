/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit.appear;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cburch.draw.canvas.CanvasObject;
import com.cburch.draw.model.DrawAttr;
import com.cburch.draw.model.Oval;
import com.cburch.draw.model.Rectangle;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;

class DefaultAppearance {
	private static final int OFFS = 50;
	
	private DefaultAppearance() { }
	
	private static class CompareLocations implements Comparator<Instance> {
		public int compare(Instance a, Instance b) {
			return a.getLocation().compareTo(b.getLocation());
		}
	}
	
	public static List<CanvasObject> build(Collection<Instance> pins) {
		Map<Direction,List<Instance>> edge;
		edge = new HashMap<Direction,List<Instance>>();
		edge.put(Direction.NORTH, new ArrayList<Instance>());
		edge.put(Direction.SOUTH, new ArrayList<Instance>());
		edge.put(Direction.EAST, new ArrayList<Instance>());
		edge.put(Direction.WEST, new ArrayList<Instance>());
		for (Instance pin : pins) {
			Direction pinFacing = pin.getAttributeValue(StdAttr.FACING);
			Direction pinEdge = pinFacing.reverse();
			List<Instance> e = edge.get(pinEdge);
			e.add(pin);
		}
		for (List<Instance> e : edge.values()) {
			Collections.sort(e, new CompareLocations());
		}

		int numNorth = edge.get(Direction.NORTH).size();
		int numSouth = edge.get(Direction.SOUTH).size();
		int numEast = edge.get(Direction.EAST).size();
		int numWest = edge.get(Direction.WEST).size();
		int maxVert = Math.max(numNorth, numSouth);
		int maxHorz = Math.max(numEast, numWest);

		int offsNorth = computeOffset(numNorth, numSouth, maxHorz);
		int offsSouth = computeOffset(numSouth, numNorth, maxHorz);
		int offsEast = computeOffset(numEast, numWest, maxVert);
		int offsWest = computeOffset(numWest, numEast, maxVert);
		
		int width = computeDimension(maxVert, maxHorz);
		int height = computeDimension(maxHorz, maxVert);

		// compute position of origin relative to top left corner of box
		int ox;
		int oy;
		if (numEast > 0) { // anchor is on east side
			ox = width;
			oy = offsEast;
		} else if (numNorth > 0) { // anchor is on north side
			ox = offsNorth;
			oy = 0;
		} else if (numWest > 0) { // anchor is on west side
			ox = 0;
			oy = offsWest;
		} else if (numSouth > 0) { // anchor is on south side
			ox = offsSouth;
			oy = height;
		} else { // anchor is top left corner
			ox = 0;
			oy = 0;
		}
		
		// place rectangle so origin is on the grid
		int rx = OFFS + (9 - (ox + 9) % 10);
		int ry = OFFS + (9 - (oy + 9) % 10);
		
		Rectangle rect = new Rectangle(rx, ry, width, height);
		rect.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(2));
		Oval notch = new Oval(rx + (width - 8) / 2, ry + 2, 8, 8);
		notch.setValue(DrawAttr.STROKE_WIDTH, Integer.valueOf(2));
		notch.setValue(DrawAttr.STROKE_COLOR, Color.GRAY);

		List<CanvasObject> ret = new ArrayList<CanvasObject>();
		ret.add(rect);
		ret.add(notch);
		placePins(ret, edge.get(Direction.WEST),
				rx,             ry + offsWest,  0, 10);
		placePins(ret, edge.get(Direction.EAST),
				rx + width,     ry + offsEast,  0, 10);
		placePins(ret, edge.get(Direction.NORTH),
				rx + offsNorth, ry,            10,  0);
		placePins(ret, edge.get(Direction.SOUTH),
				rx + offsSouth, ry + height,   10,  0);
		ret.add(new AppearanceOrigin(Location.create(rx + ox, ry + oy)));
		return ret;
	}
	
	private static int computeDimension(int maxThis, int maxOthers) {
		if (maxThis < 3) {
			return 30;
		} else if (maxOthers == 0) {
			return 10 * maxThis;
		} else {
			return 10 * maxThis + 10;
		}
	}

	private static int computeOffset(int numFacing, int numOpposite, int maxOthers) {
		int maxThis = Math.max(numFacing, numOpposite);
		int maxOffs;
		switch (maxThis) {
		case 0:
		case 1:
			maxOffs = (maxOthers == 0 ? 15 : 10);
			break;
		case 2:
			maxOffs = 10;
			break;
		default:
			maxOffs = (maxOthers == 0 ? 5 : 10);
		}
		return maxOffs + 10 * ((maxThis - numFacing) / 2);
	}
	
	private static void placePins(List<CanvasObject> dest, List<Instance> pins,
			int x, int y, int dx, int dy) {
		for (Instance pin : pins) {
			dest.add(new AppearancePort(Location.create(x, y), pin));
			x += dx;
			y += dy;
		}
	}
}
