package com.rayo.functional;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;

public class MiscTest extends MohoBasedIntegrationTest {

	@Test
	public void testMuteAndUnmute() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.answer();
		    Thread.sleep(100);
		    incoming.mute();
		    
		    Thread.sleep(500);
		    
		    incoming.unmute();
	    } finally {
	    	outgoing.hangup();
	    }
	}
	

	@Test
	public void testMuteAndOutput() throws Exception {
		
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Input<Call> input = incoming.input(new InputCommand(createSRGSGrammar("yes,no")));
	    outgoing.mute();
	    waitForEvents(100);
	    outgoing.output(new OutputCommand(createOutputDocument("yes")));
	    
	    waitForEvents();
	    assertNotReceived(InputCompleteEvent.class, input);
	    
	    outgoing.unmute();
	    waitForEvents(100);
	    outgoing.output(new OutputCommand(createOutputDocument("yes")));
	    
	    waitForEvents();
	    assertReceived(InputCompleteEvent.class, input);

	}
	
	@Test
	public void testHoldUnhold() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    try {
		    IncomingCall incoming = getIncomingCall();	    
		    assertNotNull(incoming);
		    incoming.answer();
		    Thread.sleep(100);
		    incoming.hold();
		    
		    Thread.sleep(500);
		    
		    incoming.unhold();
	    } finally {
	    	outgoing.hangup();
	    }
	}
}
