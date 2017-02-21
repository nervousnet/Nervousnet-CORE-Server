/*******************************************************************************
 *     NervousnetCoreServer - A Core Server template which is part of the Nervousnet project
 *     sensor data, text messages and more.
 *
 *     Copyright (C) 2015 ETH Zürich, COSS
 *
 *     This file is part of Nervousnet.
 *
 *     Nervousnet is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nervousnet is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nervousnet. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * 	Author:
 * 	Prasad Pulikal - prasad.pulikal@gess.ethz.ch  - Initial design and implementation
 *******************************************************************************/
package ch.ethz.coss.nervousnet.core.socket;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import ch.ethz.coss.nervousnet.core.Configuration;
import ch.ethz.coss.nervousnet.core.PulseWebSocketServer;
import ch.ethz.coss.nervousnet.core.sql.SqlSetup;
import ch.ethz.coss.nervousnet.core.utils.Log;

public class NetworkRequestWorker extends ConcurrentSocketWorker {

	Connection connection;
	SqlSetup sqlse;

	// TODO move this to constants.
	static final String HTML_START = "<html>" + "<title>" + Configuration.getInstance().getServerName() + "</title>"
			+ "<script type=\"text/javascript\"> var mytextbox = document.getElementById(\'mytext\'); var mydropdown = document.getElementById(\'dropdown\'); mydropdown.onchange = function(){ mytextbox.value = this.value; }</script>"
			+ "<body>";

	static final String HTML_END = "</body>" + "</html>";

	public NetworkRequestWorker(Socket socket, PulseWebSocketServer ps, Connection connection, SqlSetup sqlse) {
		super(socket, ps);
		this.connection = connection;
		this.sqlse = sqlse;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		InputStream in = null;
		OutputStream out = null;
		String request = "";
		Matcher httpGET = null;
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();

			boolean connected = true;
			while (connected) {
				connected &= !socket.isClosed();

				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				String line = br.readLine();
				while (line != null && line.length() > 0) {
					request += line;
					line = br.readLine();

				}

				br.close();

				handleRequest(request);

			}

		} catch (EOFException e) {
			e.printStackTrace();
			Log.getInstance().append(Log.FLAG_WARNING, "EOFException occurred, but ignored it for now.");
		} catch (NoSuchElementException nsee) {
			nsee.printStackTrace();
			Log.getInstance().append(Log.FLAG_WARNING,
					"NoSuchElementException occurred for Scanner, since its a TCP request. So handleTcpRequest called.");

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
				out.close();
				out = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
		}
	}

	private void handleRequest(String json) {

		if (json.length() > 0) {
			JsonObject jsonObj = null;
			JsonElement readings = null;
			try {
				jsonObj = new JsonParser().parse(json.trim()).getAsJsonObject();

				if (jsonObj.has("Readings")) {
					readings = jsonObj.get("Readings");
					parseReadingsArray(readings);
				} else
					parseSaveAndPushReading(jsonObj);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private void parseSaveAndPushReading(JsonObject jsonObj) {

		JsonObject featureCollection = new JsonObject();
		JsonArray features = new JsonArray();
		JsonObject feature = null;

		try {
			feature = new JsonObject();

			feature.addProperty("type", "Feature");
			JsonObject point = new JsonObject();
			point.addProperty("type", "Point");
			JsonArray coord = new JsonArray();

			if (jsonObj != null) {

				JsonPrimitive jp = new JsonPrimitive(jsonObj.get("lat").toString());

				coord.add(jp);
				coord.add(new JsonPrimitive(jsonObj.get("long").toString()));

				point.add("coordinates", coord);
				feature.add("geometry", point);

				JsonObject properties = new JsonObject();
				long id = Long.parseLong(jsonObj.get("id").toString());
				long timestamp = Long.parseLong(jsonObj.get("timestamp").toString());
				long volatility = Long.parseLong(jsonObj.get("volatility").toString());
				String uuid = jsonObj.get("uuid").toString();

				if (id == 1) { // Accelerometer

					properties.addProperty("readingType", "" + id);
					properties.addProperty("x", "" + Float.parseFloat(jsonObj.get("x").toString()));
					properties.addProperty("y", "" + Float.parseFloat(jsonObj.get("y").toString()));
					properties.addProperty("z", "" + Float.parseFloat(jsonObj.get("z").toString()));
					properties.addProperty("mercalli", "" + Float.parseFloat(jsonObj.get("mercalli").toString()));
				} else if (id == 2) { // Battery

				} else if (id == 3) { // Light
					properties.addProperty("readingType", "" + id);
					properties.addProperty("level", "" + Float.parseFloat(jsonObj.get("lux").toString()));
				} else if (id == 4) { // Gyroscope

				} else if (id == 5) { // Noise
					properties.addProperty("readingType", "" + id);
					properties.addProperty("message", "" + Float.parseFloat(jsonObj.get("dB").toString()));
				} else if (id == 6) { // Proximity
					properties.addProperty("readingType", "" + id);
					properties.addProperty("message", "" + Float.parseFloat(jsonObj.get("cms").toString()));
				} else if (id == 7) { // Temperature
					properties.addProperty("readingType", "" + id);
					properties.addProperty("message", "" + Float.parseFloat(jsonObj.get("celsius").toString()));
				} else if (id == 8) { // Text
					properties.addProperty("readingType", "" + id);
					properties.addProperty("message", "" + jsonObj.get("msg").toString());
				} else {

				}
				properties.addProperty("recordTime", timestamp);
				properties.addProperty("uuid", uuid);

				properties.addProperty("volatility", volatility);
				feature.add("properties", properties);
				features.add(feature);
				featureCollection.add("features", features);

				if (volatility != 0) {
					/***** SQL insert ********/
					// Insert data

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
						} else if (id == 3) {
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

		} catch (Exception e) {

		}
		// output the result
		// System.out.println("featureCollection=" +
		// featureCollection.toString());

		String message = featureCollection.toString();

		if (message.length() > 0)
			pSocketServer.sendToAll(message);

	}

	private void parseReadingsArray(JsonElement readings) {
		JsonArray readingsArray = readings.getAsJsonArray();

		for (int i = 0; i < readingsArray.size(); i++) {
			JsonObject jsonobject = readingsArray.get(i).getAsJsonObject();

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
