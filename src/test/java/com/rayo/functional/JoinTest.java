package com.rayo.functional;

import static org.junit.Assert.assertTrue;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.MohoInputCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;

public class JoinTest extends MohoBasedIntegrationTest {

	@Test
	@Ignore
	// does not work on gateway. Old prism version?
	public void testJoinBridge() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.join(incoming1, JoinType.BRIDGE, Direction.DUPLEX);
	    
	    assertReceived(MohoJoinCompleteEvent.class, incoming1);
	    assertReceived(MohoJoinCompleteEvent.class, incoming2);
	    
	    incoming1.input(new InputCommand(new SimpleGrammar("yes,no")));
	    incoming2.input(new InputCommand(new SimpleGrammar("yes,no")));
	    
	    outgoing2.output("yes"); 
	    outgoing1.output("no"); 
	    
	    MohoInputCompleteEvent<?> complete = assertReceived(MohoInputCompleteEvent.class, incoming1);
	    assertTrue(complete.getUtterance().equals("no"));
	    
	    complete = assertReceived(MohoInputCompleteEvent.class, incoming2);
	    assertTrue(complete.getUtterance().equals("yes"));
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore
	// Timing issue
	public void testJoinBridgeFailsIfAnotherJoinIsInProgress() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    waitForEvents(3000);
	    try {
	    	incoming1.join(outgoing2, JoinType.BRIDGE, Direction.DUPLEX);
	    } catch (Exception e) {
	    	assertTrue(e.getMessage().contains("other join operation in process"));
	    }
	}
	
	@Test
	@Ignore
	//TODO: #1579867
	public void testJoinOutgoingCallsFails() {

		// This test tries to joion two outgoing calls that haven't been answered. 
		// This should not be allowed
		
	    OutgoingCall outgoing1 = dial();	    	    
	    OutgoingCall outgoing2 = dial();
	    outgoing1.join(outgoing2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, outgoing1);
	    assertReceived(JoinCompleteEvent.class, outgoing2);
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore
	// timing issue
	public void testJoinOutgoingCalls() {
		
		// This test tries two join two outgoing calls that already have been answered. 	
	    
		OutgoingCall outgoing1 = dial();	    	    
		IncomingCall incoming1 = getIncomingCall(); 
	    incoming1.answer(); 
	    OutgoingCall outgoing2 = dial();
		IncomingCall incoming2 = getIncomingCall(); 
	    incoming2.answer(); 
	    waitForEvents();
	    
	    outgoing1.join(outgoing2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, outgoing1);
	    assertReceived(JoinCompleteEvent.class, outgoing2);
	    
	    outgoing1.hangup();
	    outgoing2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void joinCallsRecvMode() {
		
	    dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.RECV);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testMediaWithCallsJoinedOnRecvMode() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.RECV);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    // Assert leg does receive media
	    Input<Call> input1 = outgoing1.input("yes,no");
	    outgoing2.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input1);	 
	    
	    // Asserts the other leg does not receive media
	    Input<Call> input2 = outgoing2.input("yes,no");
	    outgoing1.output("yes");
	    waitForEvents();
	    assertNotReceived(InputCompleteEvent.class, input2);   
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}	
	
	
	@Test
	public void joinCallsSendMode() {
		
	    dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.SEND);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testMediaWithCallsJoinedOnSendMode() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.SEND);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);

	    // Asserts the other leg does not receive media
	    Input<Call> input2 = outgoing2.input("yes,no");
	    outgoing1.output("yes");
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input2);   

	    // Assert leg does receive media
	    Input<Call> input1 = outgoing1.input("yes,no");
	    outgoing2.output("yes");
	    waitForEvents();
	    assertNotReceived(InputCompleteEvent.class, input1);	 
	    	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void joinCallsDuplexMode() {
		
	    dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	@Ignore
	// timing issue
	public void testUnjoin() {
	
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    	    
	    incoming1.unjoin(incoming2);
	    
	    assertReceived(UnjoinCompleteEvent.class, incoming1);
	    assertReceived(UnjoinCompleteEvent.class, incoming2);
	    
	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testUnjoinOnHangupLocalEnd() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    	    
	    incoming1.hangup();
	    
	    assertReceived(UnjoinCompleteEvent.class, incoming1);
	    assertReceived(UnjoinCompleteEvent.class, incoming2);
	    
	    incoming2.hangup();
	    waitForEvents();
	}
	
	
	@Test
	public void testUnjoinOnHangupRemoteEnd() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
	    
	    assertReceived(JoinCompleteEvent.class, incoming1);
	    assertReceived(JoinCompleteEvent.class, incoming2);
	    	    
	    outgoing1.hangup();
	    
	    assertReceived(UnjoinCompleteEvent.class, incoming1);
	    assertReceived(UnjoinCompleteEvent.class, incoming2);
	    
	    incoming2.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testNoMediaOperationsAllowedOnJoin() {
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.DIRECT, Direction.DUPLEX);
	    
	    try {
	    	incoming1.input("yes,no");
	    	throw new AssertionError("Expected error");
	    } catch (Exception e) {
	    	assertTrue(e.getMessage().contains("Media operations are not allowed in the current call status"));
	    }

	    try {
	    	incoming1.input("hello");
	    	throw new AssertionError("Expected error");
	    } catch (Exception e) {
	    	assertTrue(e.getMessage().contains("Media operations are not allowed in the current call status"));
	    }

	    incoming1.hangup();
	    incoming2.hangup();
	    waitForEvents();
	}	
	
	@Test
	@Ignore
	//TODO: This only works because output/input do auto join. Rayo currently does not
	// support joining the media server. It always require a call id to join
	//TODO: #1594119
	public void testJoinedDirectHangupAndRejoin() {
		// Joins two calls in direct mode. Hangs up one leg 
		// and rejoins the other leg to the media server
		
	    OutgoingCall outgoing1 = dial();	    	    
	    IncomingCall incoming1 = getIncomingCall();
	    incoming1.answer();
	    waitForEvents();
	    
	    OutgoingCall outgoing2 = dial();
	    IncomingCall incoming2 = getIncomingCall();
	    incoming2.answer();
	    waitForEvents();
	    
	    incoming1.join(incoming2, JoinType.DIRECT, Direction.DUPLEX);
	    incoming1.hangup();
	    
	    incoming2.join();
	    Input<Call> input = outgoing2.input("yes,no");
	    incoming2.output("yes");
	    waitForEvents(2000);
	    assertReceived(InputCompleteEvent.class, outgoing2);

	    incoming2.hangup();
	    waitForEvents();
	}		
}
