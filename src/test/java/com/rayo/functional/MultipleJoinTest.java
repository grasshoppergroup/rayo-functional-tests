package com.rayo.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

public class MultipleJoinTest extends MohoBasedIntegrationTest {

	@Test
	@Ignore
	public void testOutputIsReceivedByMultipleCalls() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall call1 = getIncomingCall();
	    assertNotNull(call1);
	    call1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall call2 = getIncomingCall();
	    assertNotNull(call2);
	    call2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall call3 = getIncomingCall();
	    assertNotNull(call3);
	    call3.answer();

	    call1.join(call2, JoinType.BRIDGE_SHARED, true, Direction.DUPLEX);
	    call1.join(call3, JoinType.BRIDGE_SHARED, true, Direction.DUPLEX);
	    	    
	    InputCommand input = new InputCommand(new SimpleGrammar("yes,no"));
	    Input<Call> input2 = call2.input(input);
	    Input<Call> input3 = call3.input(input);
	    
	    outgoing1.output("yes");

	    assertReceived(InputCompleteEvent.class, input2);
	    assertReceived(InputCompleteEvent.class, input3);
	}
	

	@Test
	public void testJoinDirectModeFailsIfJoined() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.DIRECT, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    try {
	    	incoming3.join(incoming1, JoinType.DIRECT, false, Direction.DUPLEX);
	    } catch (Exception e) {
	    	// Moho should have better xmpp error handling
	    	assertTrue(e.getMessage().contains("is already joined"));
	    }
	    assertReceived(CallCompleteEvent.class, incoming3);
	    assertReceived(CallCompleteEvent.class, outgoing3);
	    
	    // Assert that incoming1 keeps working
	    Input<Call> input = incoming2.input("yes,no");
	    incoming1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);	    
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	

	@Test
	public void testJoinDirectSucceeds() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.DIRECT, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    incoming3.join(incoming1, JoinType.DIRECT, true, Direction.DUPLEX);

	    assertReceived(JoinCompleteEvent.class, incoming3);
	    assertReceived(UnjoinCompleteEvent.class, incoming2);

	    // Assert that incoming1 now talks to incoming3
	    Input<Call> input = incoming3.input("yes,no");
	    incoming1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);	    
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    outgoing3.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore // Ask Willie
	public void testJoinDirectModeFailsIfJoined2() {
		
	    OutgoingCall outgoing1 = dial();	    
	    waitForEvents();
	    OutgoingCall outgoing2 = dial();
	    waitForEvents();
	    
	    outgoing1.join(outgoing2, JoinType.DIRECT, false, Direction.DUPLEX);
	    waitForEvents();
	    
	    assertReceived(JoinCompleteEvent.class, outgoing1);
	    assertReceived(JoinCompleteEvent.class, outgoing2);
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	

	@Test
	public void testJoinBridgeSharedFailsOnDirect() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.DIRECT, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    try {
	    	incoming3.join(incoming1, JoinType.BRIDGE_SHARED, false, Direction.DUPLEX);
	    } catch (Exception e) {
	    	// Moho should have better xmpp error handling
	    	assertTrue(e.getMessage().contains("is already joined"));
	    }
	    assertReceived(CallCompleteEvent.class, incoming3);
	    assertReceived(CallCompleteEvent.class, outgoing3);
	    
	    // Assert that incoming1 keeps working
	    Input<Call> input = incoming2.input("yes,no");
	    incoming1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);	    
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testJoinBridgeSharedFailsOnBridgeExclusive() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.BRIDGE_EXCLUSIVE, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    try {
	    	incoming3.join(incoming1, JoinType.BRIDGE_SHARED, false, Direction.DUPLEX);
	    } catch (Exception e) {
	    	// Moho should have better xmpp error handling
	    	assertTrue(e.getMessage().contains("is already joined"));
	    }
	    assertReceived(CallCompleteEvent.class, incoming3);
	    assertReceived(CallCompleteEvent.class, outgoing3);
	    
	    // Assert that incoming1 keeps working
	    Input<Call> input = incoming2.input("yes,no");
	    outgoing1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);	    
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}	
	
	
	@Test
	public void testJoinBridgeSharedSuceeds() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    incoming3.join(incoming1, JoinType.BRIDGE_SHARED, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming3);
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    outgoing3.hangup();
	    waitForEvents();
	}
	
	
	@Test
	public void testJoinBridgeSharedSuceedsForceTrue() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    incoming3.join(incoming1, JoinType.BRIDGE_SHARED, true, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming3);
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    outgoing3.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testJoinExclusiveModeFailsIfJoined() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.DIRECT, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    try {
	    	incoming3.join(incoming1, JoinType.BRIDGE_EXCLUSIVE, false, Direction.DUPLEX);
	    } catch (Exception e) {
	    	// Moho should have better xmpp error handling
	    	assertTrue(e.getMessage().contains("is already joined"));
	    }
	    assertReceived(CallCompleteEvent.class, incoming3);
	    assertReceived(CallCompleteEvent.class, outgoing3);
	    
	    // Assert that incoming1 keeps working
	    Input<Call> input = incoming2.input("yes,no");
	    incoming1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);	    
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	

	@Test
	public void testJoinBridgeExclusiveSucceedsForce() {
		
	    OutgoingCall outgoing1 = dial();	    
	    IncomingCall incoming1 = getIncomingCall();
	    assertNotNull(incoming1);
	    incoming1.answer();

	    OutgoingCall outgoing2 =dial();
	    IncomingCall incoming2 = getIncomingCall();
	    assertNotNull(incoming2);
	    incoming2.answer();

	    OutgoingCall outgoing3 =dial();
	    IncomingCall incoming3 = getIncomingCall();
	    assertNotNull(incoming3);
	    incoming3.answer();
	    waitForEvents();

	    incoming1.join(incoming2, JoinType.DIRECT, false, Direction.DUPLEX);
	    waitForEvents();
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    incoming3.join(incoming1, JoinType.BRIDGE_EXCLUSIVE, true, Direction.DUPLEX);

	    assertReceived(JoinCompleteEvent.class, incoming3);
	    assertReceived(UnjoinCompleteEvent.class, incoming2);

	    // Assert that incoming1 now talks to incoming3
	    Input<Call> input = incoming3.input("yes,no");
	    incoming1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);	    
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    outgoing3.hangup();
	    waitForEvents();
	}

}
