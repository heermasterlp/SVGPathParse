package com.um.svg.path;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * convert the svg path to coordinate and save data to file
 * 
 * @author peterliu
 *
 */
public class PathParse {
	
	/**
	 * Parse svg file to get path coordinates
	 * @param path
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static List<String> parseSVGFile(String path) throws ParserConfigurationException, SAXException, IOException{
		if ("".equals(path)) {
			return null;
		}
		List<String> coordinates = new ArrayList<>(); // All coordinates of path converted from svg path
		
		// load svg file
		File inputFile = new File(path);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(inputFile);
		doc.getDocumentElement().normalize();
		System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
		
		// parse svg file
		NodeList nList = doc.getElementsByTagName("path");
		int nLength = nList.getLength();
//		System.out.println("path num: " + nLength);
		for(int temp = 0; temp < nLength; temp++){
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				if ("".equals(eElement.getAttribute("d")) || eElement.getAttribute("d").contains("z")) {
					continue;
				}
				String pathValue = eElement.getAttribute("d");
				pathValue = parsePath(pathValue);
				coordinates.add(pathValue);
			}
		}
		
		return coordinates;
	}
	
	/**
	 * Parse path of SVG and save data to file
	 * 
	 * @param path
	 */
	public static String parsePath(String path) {
		if ("".equals(path)) {
			return "";
		}
		// current coordinates
		double curt_x = 0.0, curt_y = 0.0;
		String[] splits = path.trim().split(" ");
//		System.out.println(splits.length);
		int len = splits.length;
		// format the path data
		List<HashMap<String, ArrayList<String>>> data = new ArrayList<HashMap<String, ArrayList<String>>>();
		
		for (int i = 0; i < len; i++) {
			String content = splits[i];
			// m parse
			if ("m".equals(content)) {
				ArrayList<String> d = new ArrayList<>();
				d.add(splits[i + 1]);
				HashMap<String, ArrayList<String>> m = new HashMap<>();
				m.put(content, d);
				data.add(m);
				
				i++;
				continue;
			}
			// c parse
			if ("c".equals(content)) {
				int c_end = i + 1;
				for(; c_end < len; c_end++){
					if (!splits[c_end].contains(",")) {
						break;
					}
				}
				
				ArrayList<String> d = new ArrayList<>();
				HashMap<String, ArrayList<String>> m = new HashMap<>();
				for(int index = i+1; index < c_end; index++){
					d.add(splits[index]);
				}
				m.put(content, d);
				data.add(m);
				
				i = c_end - 1;
				continue;
			}
			// l parse
			if ("l".equals(content)) {
				int l_end = i + 1;
				for(; l_end < len; l_end++){
					if (!splits[l_end].contains(",")) {
						break;
					}
				}
				
				ArrayList<String> d = new ArrayList<>();
				HashMap<String, ArrayList<String>> m = new HashMap<>();
				for (int index = i+1; index < l_end; index++) {
					d.add(splits[index]);
				}
				m.put(content, d);
				data.add(m);
				
				i = l_end - 1;
				continue;
			}
		}
		
		
		String result = "";
		
		// calculate the data of coordinates
		for (HashMap<String, ArrayList<String>> hashMap : data) {
			// m data
			if (hashMap.keySet().contains("m")) {
				if (hashMap.get("m") != null && !hashMap.get("m").get(0).equals("")) {
					String cortString = hashMap.get("m").get(0);
					String[] corts = cortString.split(",");
					curt_x = Double.valueOf(corts[0]);
					curt_y = Double.valueOf(corts[1]);
					result += cortString + " ";
					continue;
				}
			}
			
			// c data
			if (hashMap.keySet().contains("c")) {
				String cString = calculateBezierCurve(curt_x, curt_y, hashMap.get("c"));
				// calculate new current coordinates
				String newCoordinate = getCurrentCoordinate(cString);
				if (!newCoordinate.contains(",")) {
					continue;
				}
				String[] newSplits = newCoordinate.split(",");
				curt_x = Double.valueOf(newSplits[0]);
				curt_y = Double.valueOf(newSplits[1]);
				result += cString;
				continue;
			}
			
			// l data 
			if (hashMap.keySet().contains("l")) {
				String lString = calculateLine(curt_x, curt_y, hashMap.get("l"));
				
				// calculate new current coordinates
				String newCoordinate = getCurrentCoordinate(lString);
				if (!newCoordinate.contains(",")) {
					continue;
				}
				String[] newSplits = newCoordinate.split(",");
				curt_x = Double.valueOf(newSplits[0]);
				curt_y = Double.valueOf(newSplits[1]);
				result += lString;
				continue;
			}
		}
//		System.out.println(result);
		return result;
	}
	
	/**
	 *  Calculate the Bezier Curve data
	 * @param x
	 * @param y
	 * @param list
	 * @return
	 */
	public static String calculateBezierCurve(double x0, double y0, ArrayList<String> list){
		if (list.isEmpty()) {
//			System.out.println("c list is null");
			return "";
		}
		String result = "";
		// calculate the Bezier curve coordinates
		int len = list.size();
		for (int i = 0; i < len; i++) {
			String[] splits = list.get(i).split(",");
			double ctr1_x = Double.valueOf(splits[0]) + x0;
			double ctr1_y = Double.valueOf(splits[1]) + y0;
			splits = list.get(i + 1).split(",");
			double ctr2_x = Double.valueOf(splits[0]) + x0;
			double ctr2_y = Double.valueOf(splits[1]) + y0;
			splits = list.get(i + 2).split(",");
			double end_x = Double.valueOf(splits[0]) + x0;
			double end_y = Double.valueOf(splits[1]) + y0;
			
			String slice = getCoordinateOfBezierCurve(x0, y0, ctr1_x, ctr1_y, ctr2_x, ctr2_y, end_x, end_y);
			
			result += slice + " ";
			
			x0 = end_x;
			y0 = end_y;
			i += 2;
			
		}
		
		
		return result;
	}
	
	/**
	 * calculate coordinates of bezier based on the three control points 
	 * @param x
	 * @param y
	 * @param list
	 * @return
	 */
	public static String getCoordinateOfBezierCurve(double x0, double y0, double ctr1x, double ctr1y, double ctr2x, double ctr2y, double endx, double endy){
		
		String result = "";
		
		for(double t = 0.0; t <= 1.0; t+= 0.01){
			double x = (1-t)*(1-t)*(1-t) * x0 + 3*(1-t)*(1-t)*t*ctr1x + 3*(1-t)*t*t*ctr2x + t*t*t*endx;
			double y = (1-t)*(1-t)*(1-t) * y0 + 3*(1-t)*(1-t)*t*ctr1y + 3*(1-t)*t*t*ctr2y + t*t*t*endy;
			result += x + "," + y + " ";
		}
		
		return result;
	}
	
	/**
	 * Calculate the Line data
	 * @param x
	 * @param y
	 * @param list
	 * @return
	 */
	public static String calculateLine(double x,double y, ArrayList<String> list){
		if (list.isEmpty()) {
			return "";
		}
		
		String result = "";
		
		double end_x = x;
		double end_y = y;
		
		for (String l : list) {
			String[] splits = l.split(",");
			end_x = Double.valueOf(splits[0]) + x;
			end_y = Double.valueOf(splits[1]) + y;
			
			result += getStartEndLine(x, y, end_x, end_y);
			
			x = end_x;
			y = end_y;
		}
		
		return result;
	}
	
	/**
	 *  Get all coordinates on line
	 * @param start_x
	 * @param start_y
	 * @param end_x
	 * @param end_y
	 * @return
	 */
	public static String getStartEndLine(double start_x, double start_y, double end_x, double end_y){
		String result = "";
		if (start_x == end_x) {
			// straight line
			for(double index = start_y; index <= end_y; index+= 0.1){
				result += start_x +"," + end_y + " ";
			}
		}else {
			for(double index = start_x; index <= end_x; index+= 0.1){
				double y = (index - start_x) * (end_y - start_y) / (end_x - start_x) + start_y;
				result += index + "," + y + " ";
			}
		}
		
		return result;
	}
	
	/**
	 * Get the current coordinate at the last of string
	 * @param string
	 * @return
	 */
	public static String getCurrentCoordinate(String string){
		if ("".equals(string)) {
			return "";
		}
		String[] splits = string.split(" ");
		return splits[splits.length - 1];
	}
	
	/**
	 *  Save coordinates to file
	 * @param list
	 */
	public static void saveToFile(List<String> list, String path){
		if (list.isEmpty()) {
			return;
		}
		// write to file
		
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		// TODO Auto-generated method stub
//		String path = "m 128.81356,55.097986 c -2.54664,-2.553626 -5.28319,-4.822381 -6.54131,-9.308792 -1.1168,-2.539147 0.24429,-2.910139 1.76111,-3.144862 4.56791,0.735115 8.38217,1.53257 10.94413,3.270657 2.69217,1.253671 5.47123,2.212302 7.54767,3.773835 3.22345,1.01088 3.62306,2.914687 4.5286,5.912341 0,0 0.50056,2.438321 -0.50318,3.773834 -0.80644,1.632148 -2.57709,4.207596 -4.6544,6.792903 -0.98218,3.35452 -0.41188,6.70904 0.2516,10.06356 l 3.52224,21.385066 3.77384,24.152532 2.76748,17.10806";
//		parsePath(path);
		String path = "/Users/peterliu/Documents/openDevelopment/tee.svg";
		System.out.println("---begin----");
		List<String> paths = parseSVGFile(path);
		System.out.println(paths.size());
//		System.out.println(paths.toStriníg());
		System.out.println("----end-----");
	}

}
