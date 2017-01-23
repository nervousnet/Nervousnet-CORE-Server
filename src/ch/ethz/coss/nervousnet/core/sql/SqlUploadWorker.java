/*******************************************************************************
 *     SwarmPulse - A service for collective visualization and sharing of mobile 
 *     sensor data, text messages and more.
 *
 *     Copyright (C) 2015 ETH Zürich, COSS
 *
 *     This file is part of SwarmPulse.
 *
 *     SwarmPulse is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SwarmPulse is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SwarmPulse. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * 	Author:
 * 	Prasad Pulikal - prasad.pulikal@gess.ethz.ch  - Initial design and implementation
 *******************************************************************************/
package ch.ethz.coss.nervousnet.core.sql;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import ch.ethz.coss.nervousnet.core.PulseWebSocketServer;
import ch.ethz.coss.nervousnet.core.socket.ConcurrentSocketWorker;
import ch.ethz.coss.nervousnet.core.utils.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

public class SqlUploadWorker extends ConcurrentSocketWorker {

	Connection connection;
	SqlSetup sqlse;

	public SqlUploadWorker(Socket socket, PulseWebSocketServer ps, Connection connection, SqlSetup sqlse) {
		super(socket, ps);
		this.connection = connection;
		this.sqlse = sqlse;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		// InputStream is;
		InputStream in = null;

		try {
			in = socket.getInputStream();
			boolean connected = true;
			while (connected) {
				connected &= !socket.isClosed();
				
					String json = null;
					try {
						
						 json = IOUtils.toString(in, "UTF-8"); 
						 
						in.close();
					} catch (MalformedURLException me) {
						System.out.println("MalformedURLException: " + me);
					} catch (IOException ioe) {
						System.out.println("IOException: " + ioe);
					}
					System.out.println("JSON STRING = " + json);
					System.out.println("JSON Length = " + json.length());

					if (json.length() <= 0)
						continue;
					
				
					JsonObject jsonObj = null;
					JsonElement readings = null;
					try {
						jsonObj = new JsonParser().parse(json.trim()).getAsJsonObject();
						
						if(jsonObj.has("Readings")) {
							readings = jsonObj.get("Readings");
							parseReadingsArray(readings);
						}
						else
							parseSaveAndPushReading(jsonObj);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
					
//					if(readings.isJsonArray()) 
//						parseReadingsArray(readings);
//					 else 
//						parseSaveAndPushReading(json);

			}

		} catch (EOFException e) {
			e.printStackTrace();
			Log.getInstance().append(Log.FLAG_WARNING, "EOFException occurred, but ignored it for now.");
		} catch (IOException e) {
			e.printStackTrace();
			Log.getInstance().append(Log.FLAG_WARNING, "Opening data stream from socket failed");
		} catch (Exception e) {
			e.printStackTrace();
			Log.getInstance().append(Log.FLAG_WARNING, "Generic error");
		} finally {
			cleanup();
			try {
				in.close();
				in = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			}
		}
	}
	
	
	
	private void parseSaveAndPushReading(JsonObject jsonObj) {
		System.out.println("1");

		JsonObject featureCollection = new JsonObject();
		JsonArray features = new JsonArray();
		JsonObject feature = null;
		
		try {
		feature = new JsonObject();
		System.out.println("2");
		feature.addProperty("type", "Feature");
		// JsonArray featureList = new JsonArray();
		// iterate through your list
		// for (ListElement obj : list) {
		// {"geometry": {"type": "Point", "coordinates":
		// [-94.149, 36.33]}
		JsonObject point = new JsonObject();
		point.addProperty("type", "Point");
		// construct a JSONArray from a string; can also use an
		// array or list
		JsonArray coord = new JsonArray();
//		if (reading == null || reading.location == null)
//			continue;
//		else if (reading.location.latnLong[0] == 0 && reading.location.latnLong[1] == 0)
//			continue;
		System.out.println("3 "+jsonObj);
		
		if(jsonObj != null) {
		
		JsonPrimitive jp = new JsonPrimitive(jsonObj.get("lat").toString());
		System.out.println("33 "+jp.getAsString());
		coord.add(jp);
		coord.add(new JsonPrimitive(jsonObj.get("long").toString()));
		System.out.println("4");
		point.add("coordinates", coord);
		feature.add("geometry", point);

		JsonObject properties = new JsonObject();
		long id = Long.parseLong(jsonObj.get("id").toString());
		long timestamp = Long.parseLong(jsonObj.get("timestamp").toString());
		long volatility = Long.parseLong(jsonObj.get("volatility").toString());
		String uuid = jsonObj.get("uuid").toString();
		System.out.println("5");
		if (id == 1) { //Accelerometer
			 System.out.println("Reading instance of Accelerometer");
			properties.addProperty("readingType", "" + id);
			properties.addProperty("x", "" + Float.parseFloat(jsonObj.get("x").toString()));
			properties.addProperty("y", "" + Float.parseFloat(jsonObj.get("y").toString()));
			properties.addProperty("z", "" + Float.parseFloat(jsonObj.get("z").toString()));
			properties.addProperty("mercalli", "" + Float.parseFloat(jsonObj.get("mercalli").toString()));
		} else if(id == 2) { // Battery
			
		} else if (id == 3) { //Light
			properties.addProperty("readingType", "" + id);
			properties.addProperty("level", "" + Float.parseFloat(jsonObj.get("lux").toString()));
		} else if(id == 4) { // Gyroscope
			
		}  else if (id == 5) { // Noise
			properties.addProperty("readingType", "" + id);
			properties.addProperty("message", "" + Float.parseFloat(jsonObj.get("dB").toString()));
		}  else if(id == 6) { // Proximity
			properties.addProperty("readingType", "" + id);
			properties.addProperty("message", "" + Float.parseFloat(jsonObj.get("cms").toString()));
		}  else if (id == 7) { // Temperature
			properties.addProperty("readingType", "" + id);
			properties.addProperty("message", "" + Float.parseFloat(jsonObj.get("celsius").toString()));
		} else if (id == 8) { // Text
			properties.addProperty("readingType", "" + id);
			properties.addProperty("message", "" + jsonObj.get("msg").toString());
		} else {
			 System.out.println("Reading instance not known");
		}
		properties.addProperty("recordTime", timestamp);
		System.out.println("6");
		properties.addProperty("volatility",  volatility);
		feature.add("properties", properties);
		features.add(feature);
		featureCollection.add("features", features);
		System.out.println("7");
		if (volatility != 0) {
			/***** SQL insert ********/
			// Insert data
			 System.out.println("before uploading SQL - reading uuid = "+uuid);
			 System.out.println("Reading volatility = "+volatility);
			PreparedStatement datastmt = sqlse.getSensorInsertStatement(connection, id);
			if (datastmt != null) {
			
				List<Integer> types = sqlse.getArgumentExpectation(id);
				datastmt.setString(1, uuid);
				datastmt.setLong(2, timestamp);
				datastmt.setLong(3, volatility);
				datastmt.setDouble(4, Double.parseDouble(jsonObj.get("lat").toString()));
				datastmt.setDouble(5, Double.parseDouble(jsonObj.get("long").toString()));
				if (id == 1) {
					datastmt.setDouble(6, Double.parseDouble(jsonObj.get("x").toString()));
					datastmt.setDouble(7, Double.parseDouble(jsonObj.get("y").toString()));
					datastmt.setDouble(8, Double.parseDouble(jsonObj.get("z").toString()));
					datastmt.setDouble(9, Double.parseDouble(jsonObj.get("mercalli").toString()));
				}else 
				if (id == 3) {
					datastmt.setDouble(6, Double.parseDouble(jsonObj.get("lux").toString()));
				} else if (id == 5) {
					datastmt.setDouble(6, Double.parseDouble(jsonObj.get("dB").toString()));
				} else if (id == 6) {
					datastmt.setDouble(6, Double.parseDouble(jsonObj.get("cms").toString()));
				} else if (id == 7) {
					datastmt.setDouble(6, Double.parseDouble(jsonObj.get("celsius").toString()));
				} else if (id == 8) {
					datastmt.setString(6, jsonObj.get("msg").toString());
				}
				
				// System.out.println("datastmt after populating - "
				// + datastmt.toString());

				datastmt.addBatch();
				datastmt.executeBatch();
				datastmt.close();
			}
			/*************/

		}
		}
	} catch (JsonParseException e) {
		System.out.println("can't save json object: " + e.toString());
	} catch (Exception e) {
		System.out.println("General Exception: " + e.toString());
	}
	// output the result
	// System.out.println("featureCollection=" +
	// featureCollection.toString());

	String message = featureCollection.toString();
	
	if(message.length() > 0)
	pSocketServer.sendToAll(message);
	
	
	}
	
	
	private void parseReadingsArray(JsonElement readings) {
		JsonArray readingsArray = readings.getAsJsonArray();
		
		
		for(int i = 0; i < readingsArray.size(); i++)
		{
		      JsonObject jsonobject = readingsArray.get(i).getAsJsonObject();

		      System.out.println("Object -- "+jsonobject.toString());
		      parseSaveAndPushReading(jsonobject);
		}
		
	}

	@Override
	protected void cleanup() {
		super.cleanup();
		try {
			connection.close();
		} catch (SQLException e) {
			Log.getInstance().append(Log.FLAG_ERROR, " Error in closing connection.");
		}
	}
}
