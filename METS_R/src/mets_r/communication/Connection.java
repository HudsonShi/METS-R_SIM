package mets_r.communication;

import java.io.IOException;
import java.net.InetAddress;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import mets_r.ContextCreator;
import mets_r.GlobalVariables;

/**
 * When a request from a remote program for a network connection is received,
 * the network connection manager will produce a new WebSocket connection to be
 * managed by a new instance of this class. This class provides both the
 * WebSocket annotated methods the Jetty library will use to handle processing
 * control of this particular socket and the delivery of messages received from
 * the remote program and the interface methods of a data consumer which uses
 * the data collection buffer built into the simulation. 
 * 
 * @author: Christopher Thompson, Zengxiang Lei
**/

@WebSocket
public class Connection{
	private int id;
	private static int COUNTER = 0; // Number of instances of the connection created so far

	private Session session; // The websocket session object for the connection. 
	private InetAddress ip; // The address of the remote host. 
	
	private QueryMessageHandler queryHandler;
	private StepMessageSender stepSender;
	private AnswerMessageSender answerSender;
	
	public static int prevTick = -1; // for tracking the step function
	public static int prevHour = -1; // for tracking the latest hour that the bus schedule is processed

	public Connection() {
		// increment our instance counter and save this object's number
		this.id = ++Connection.COUNTER;
		
		this.queryHandler = new QueryMessageHandler();
		this.stepSender = new StepMessageSender();
		this.answerSender = new AnswerMessageSender();

		ContextCreator.logger.info("Connection object created.");
	}
	
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		ContextCreator.logger.info(statusCode + ": " + reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		ContextCreator.logger.info(this.id + "-ERR " + t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		if (session == null) {
			return;
		}

		this.ip = session.getRemoteAddress().getAddress();

		ContextCreator.logger.info("Connected to " + this.ip.toString() + ".");

		// Save a reference to the session for this socket connection
		this.session = session;

		// Register the connection in ContextCreator
		ContextCreator.connection = this;

		try {
			if (GlobalVariables.ENABLE_ECO_ROUTING_EV) {
				answerSender.sendCandidateRoutesForTaxi(this.session);
			}
			if (GlobalVariables.ENABLE_ECO_ROUTING_BUS) {
				answerSender.sendCandidateRoutesForBus(this.session);
			}
			// Send the first tick message, important for getting the reply 
			// the bus schedules if dynamic bus planning is on 
			this.stepSender.sendMessage(this.session, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		// Transfer to JSON here
		JSONObject jsonMsg = new JSONObject();
		try {
			JSONParser parser = new JSONParser();
			jsonMsg = (JSONObject) parser.parse(message);
			String[] msgType = jsonMsg.get("MSG_TYPE").toString().split("_");
			if (msgType[0] == "STEP") {
				ContextCreator.stepHandler.handleMessage(msgType[0], jsonMsg);
			}
			else if (msgType[0].equals("CTRL")) {
				ContextCreator.controlHandler.handleMessage(msgType[1], jsonMsg);;
			}
			else if(msgType[0].equals("QUERY")){
				String answer = this.queryHandler.handleMessage(msgType[1], jsonMsg);
				this.answerSender.sendMessage(session, answer);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendStepMessage(int tick) {
		try {
			stepSender.sendMessage(session, tick);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
