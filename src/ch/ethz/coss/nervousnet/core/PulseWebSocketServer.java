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
package ch.ethz.coss.nervousnet.core;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import ch.ethz.coss.nervousnet.core.socket.PulseRequestHandlingServer;

public class PulseWebSocketServer extends WebSocketServer {

	// private ArrayList<WebSocket> timeMachineConnectionsList = new
	// ArrayList<WebSocket>();

	private PulseRequestHandlingServer prhServer;

	public PulseWebSocketServer(int port, PulseRequestHandlingServer prhServer) throws UnknownHostException {
		super(new InetSocketAddress(port));
		this.prhServer = prhServer;
		this.prhServer.setPulseServer(this);

	}

	public PulseWebSocketServer(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {

		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress()
				+ " has joined in to received the pulse pushes!");

	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {

		//

		if (prhServer.hTimeMachineConnectionList.containsKey(conn))
			prhServer.hTimeMachineConnectionList.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {

		//

		if (message.contains("type=")) {
			int type = Integer.parseInt(message.substring(message.indexOf("=") + 1, message.indexOf("=") + 2));

			//

			switch (type) {
			case 0:
				prhServer.hTimeMachineConnectionList.remove(conn);
				// System.out.println("hTimeMachineConnectionList size =
				// "+prhServer.hTimeMachineConnectionList.size());

				break;
			case 1:
				// System.out.println("hTimeMachineConnectionList size =
				// "+prhServer.hTimeMachineConnectionList.size());

				String request = message.substring(message.indexOf("=") + 1);
				if (request.length() > 1) {
					PulseTimeMachineRequest pulseTimeMachineRequest = new PulseTimeMachineRequest(request, conn, 0);
					prhServer.addToRequestList(pulseTimeMachineRequest);
					prhServer.hTimeMachineConnectionList.put(conn, pulseTimeMachineRequest);
					Thread reqServerThread = new Thread(prhServer);
					reqServerThread.start();

				} else if (request.length() == 1) {
					prhServer.hTimeMachineConnectionList.put(conn, new PulseTimeMachineRequest(true));

				}

				break;
			case 2:
				// System.out.println("Switched conn to Time Machine.");
				// System.out.println("hTimeMachineConnectionList size =
				// "+prhServer.hTimeMachineConnectionList.size());

				String requestValue = message.substring(message.indexOf("=") + 1);
				// System.out.println("Request -- "+request);
				if (requestValue.length() > 1) {
					PulseTimeMachineRequest pulseTimeMachineRequest = new PulseTimeMachineRequest(requestValue, conn, 1);
					prhServer.addToRequestList(pulseTimeMachineRequest);
					prhServer.hTimeMachineConnectionList.put(conn, pulseTimeMachineRequest);
					Thread reqServerThread = new Thread(prhServer);
					reqServerThread.start();

				} else if (requestValue.length() == 1) {
					prhServer.hTimeMachineConnectionList.put(conn, new PulseTimeMachineRequest(true));

				}

				break;

			}

		}
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		//
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if (conn != null) {
			// some errors like port binding failed may not be assignable to a
			// specific websocket
		}
	}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToAll(String text) {
		// System.out.println("prhServer.hTimeMachineConnectionList size
		// "+prhServer.hTimeMachineConnectionList.size());

		Collection<WebSocket> con = connections();
		synchronized (con) {
			for (WebSocket c : con) {
				if (!prhServer.hTimeMachineConnectionList.containsKey(c)) {
					c.send(text);
					// //
				} else {

				}

			}

		}
	}

	/**
	 * Sends data to specific Websocket connection. Used for Time-machine
	 * implementation
	 * 
	 * @param text
	 *            The String to send across the network.
	 * @throws InterruptedException
	 *             When socket related I/O errors occur.
	 */
	public void sendToSocket(WebSocket conn, long requestID, String text, boolean isComplete) {
		// System.out.println("prhServer.hTimeMachineConnectionList size
		// "+prhServer.hTimeMachineConnectionList.size());
		if (prhServer.hTimeMachineConnectionList.containsKey(conn)) {

			PulseTimeMachineRequest ptmRequest = prhServer.hTimeMachineConnectionList.get(conn);
			if (ptmRequest.requestID == requestID && !ptmRequest.isNull) {
				conn.send(text);
			}

			if (isComplete) {
				prhServer.hTimeMachineConnectionList.put(conn, new PulseTimeMachineRequest(true));
			}

		}

		if (prhServer.hTimeMachineConnectionList.size() == 0)
			PulseTimeMachineRequest.ID_COUNTER = 0;
	}

}