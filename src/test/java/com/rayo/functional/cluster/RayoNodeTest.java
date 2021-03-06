package com.rayo.functional.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.functional.base.RayoBasedIntegrationTest;
import com.voxeo.rayo.client.JmxClient;
import com.voxeo.rayo.client.RayoClient;

public class RayoNodeTest extends RayoBasedIntegrationTest {

	Logger log = LoggerFactory.getLogger(RayoNodeTest.class);
	
	@Before
	public void setup() throws Exception {
		
		loadProperties();
	}
		
	// Cluster Docs. Scenario 13
	@Test
	public void testRayoNodesReceiveErrorIfNoClients() throws Exception {

		// We are going to cheat. We are going to connect directly to one of 
		// the rayo nodes and dial to generate an offer. That offer will be 
		// rejected by the gateway as there will not be any client application
		// listening for offers.
		//
		// It is not a perfect test as we should validate the type of error. Feel 
		// free to improve it in the future.
		
		String directUsername = "usera";
		String directPassword = "1";
				
		try {
			String server = handleGatewayClusterAndEC2(getNodeName());
			
			rayoClient = new RayoClient(server, server);
			rayoClient.connect(directUsername, directPassword);
			
			int errors = getTotalNodePresenceErrors();
			assertEquals(getClientsConnected(),0);

			dial();
			Thread.sleep(1000);

			assertEquals(getClientsConnected(),0);
			int errors2 = getTotalNodePresenceErrors();
			
			// Depending on timing we can get two or three errors. There is always an 
			// error that is generated by the OfferEvent that arrives to the gateway
			// without any connected clients. Then there will be two EndEvents, one 
			// for each call leg.
			assertTrue(errors2==errors+2 || errors2==errors+3);

		} finally {
			if (rayoClient != null && rayoClient.getXmppConnection().isConnected()) {
				rayoClient.disconnect();
			}
		}
	}

	// Cluster Docs. Scenario 14
	@Test
	public void testRayoNodesHavePlatform() throws Exception {
	
		JmxClient client = new JmxClient(rayoServer, "8080");

		List<String> nodesList = getNodeNames(client);
		assertTrue(nodesList.size() > 0);
		List<String> platformsList = getPlatformNames(client);
		assertTrue(platformsList.size() > 0);
		
		Set<String> allPlatformNodes = new HashSet();
		for(String platform: platformsList) {
			allPlatformNodes.addAll(getNodesForPlatform(client, platform));
		}	
		
		for(String node: nodesList) {
			assertTrue(allPlatformNodes.contains(node));
		}
	}
	
	// Cluster Docs. Scenario 16
	@Test
	public void testRayoNodeNotAvailableAfterQuiesce() throws Exception {
	
		String node = getNodeName();
		
		int nodes = getNodes();
		
		JmxClient nodeClient = new JmxClient(node, "8080");

		try {
			quiesceNode(nodeClient);
			int nodes2 = getNodes();
			assertTrue(nodes2 == nodes-1);
		} finally {			
			dequiesceNode(nodeClient);
		}			
	}
	
	// Cluster Docs. Scenario 17
	@Test
	public void testRayoNodeBackAvailableAfterDequiesced() throws Exception {
	
		String node = getNodeName();
		
		int nodes = getNodes();
		JmxClient nodeClient = new JmxClient(node, "8080");
		try {			
			quiesceNode(nodeClient);
			dequiesceNode(nodeClient);
			waitForEvents();

			int nodes2 = getNodes();
			assertEquals(nodes2,nodes);
		} finally {
			dequiesceNode(nodeClient);
		}
	}
	
	
	// Cluster Docs. Scenario 18
	@Test
	@Ignore
	public void testRayoNodeLoadBalancing() throws Exception {
	
		String firstNode = getNodeName(0);
		String secondNode = getNodeName(1);
		JmxClient node1Client = new JmxClient(firstNode, "8080");
		JmxClient node2Client = new JmxClient(secondNode, "8080");		
		
		try {
			long initialCalls = getTotalCalls();
			rayoClient = new RayoClient(xmppServer, rayoServer);
			rayoClient.connect(xmppUsername, xmppPassword,"loadbalance");
			
			rayoClient.dial(new URI(sipDialUri)).getCallId();
			waitForEvents();
			assertEquals(getTotalCalls(), initialCalls+2);
			
			quiesceNode(node1Client);
			// we need to dial the second node as we have quiesced the first one
			URI secondURI = new URI("sip:usera@" + secondNode);
			rayoClient.dial(secondURI).getCallId();
			waitForEvents(2000);
			assertEquals(getTotalCalls(), initialCalls+4);
			
			dequiesceNode(node1Client);
			quiesceNode(node2Client);
			// we need to dial the first node as we have quiesced the second one
			URI firstURI = new URI("sip:usera@" + firstNode);
			rayoClient.dial(firstURI).getCallId();
			waitForEvents();
			assertEquals(getTotalCalls(), initialCalls+6);
	
			dequiesceNode(node2Client);
			rayoClient.dial(new URI(sipDialUri)).getCallId();
			waitForEvents();
			assertEquals(getTotalCalls(), initialCalls+8);
		} finally {
			dequiesceNode(node1Client);
			dequiesceNode(node2Client);	
			waitForEvents();
		}
	}
	
	private int getTotalNodePresenceErrors() throws Exception {
		
		int totalNodeErrors = 0;
		List<String> nodesList = new ArrayList<String>();
		JmxClient client = new JmxClient(rayoServer, "8080");
		JSONArray nodes = ((JSONArray)client.jmxValue("com.rayo.gateway:Type=Gateway", "RayoNodes"));
		Iterator<JSONObject> it = nodes.iterator();
		while(it.hasNext()) {
			JSONObject json = it.next();
			String jid = (String)json.get("hostname");
			nodesList.add(jid);
		}
		
		for (String node: nodesList) {
			JmxClient nodeClient = new JmxClient(node, "8080");
			Long errors = (Long)nodeClient.jmxValue("com.rayo:Type=Rayo", "PresenceErrorsReceived");
			if (errors != null) {
				totalNodeErrors+=errors.intValue();
			}
		}
		
		return totalNodeErrors;
	}
	
	private String handleGatewayClusterAndEC2(String hostname) {

		if (System.getProperty("hudson.append.ext") != null && !hostname.contains("-ext")) {
			// Small "hack" needed for Hudson functional tests. Otherwise the 
			// tests run on hudson aren't able to access the nodes JMX interfaces
			// as the gateway will return the internal domains for the rayo nodes.
			String[] parts = StringUtils.split(hostname,".");
			parts[0] = parts[0] + "-ext";
			hostname = StringUtils.join(parts,".");
			log.debug("Using hostname: " + hostname);
		}
		return hostname;
	}
}
