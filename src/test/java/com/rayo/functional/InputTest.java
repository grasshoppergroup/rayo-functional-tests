package com.rayo.functional;

import static org.junit.Assert.*;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent.Cause;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class InputTest extends MohoBasedIntegrationTest {

	private String grxml = "<grammar xmlns=\"http://www.w3.org/2001/06/grammar\" root=\"MAINRULE\"> " +
        "<rule id=\"MAINRULE\"> " +
          "<one-of> " +
            "<item> " +
              "<item repeat=\"0-1\"> need a</item> " +
              "<item repeat=\"0-1\"> i need a</item> " +
              "<one-of> " +
                "<item> clue </item> " +
              "</one-of> " +
              "<tag> out.concept = \"clue\";</tag> " +
            "</item> " +
            "<item> " +
              "<item repeat=\"0-1\"> have an</item> " +
              "<item repeat=\"0-1\"> i have an</item> " +
              "<one-of> " +
                "<item> answer </item> " +
              "</one-of> " +
              "<tag> out.concept = \"answer\";</tag> " +
            "</item> " +
          "</one-of> " +
        "</rule> " +
      "</grammar>";
	
	@Test
	public void testInput() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    Input<Call> input = incoming.input(new InputCommand(new SimpleGrammar("yes,no")));
	    outgoing.output("yes");
	    
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.MATCH);
	    assertEquals(complete.getInterpretation(),"yes");
	}
	
	@Test
	public void testInputDTMF() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    InputCommand command = new InputCommand(new SimpleGrammar("[1 DIGITS]"));
	    command.setInputMode(InputMode.DTMF);
	    Input<Call> input = incoming.input(command);
	    
        String text = "<speak><audio src=\"dtmf:7\"/></speak>";
        OutputCommand outputCommand = new OutputCommand(new TextToSpeechResource(text));        
	    outgoing.output(outputCommand);

	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.MATCH);
	    assertEquals(complete.getInterpretation(),"dtmf-7");		
	}
	
	@Test
	public void testInputGrxml() {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    
	    Grammar grammar = new Grammar("application/srgs+xml", grxml);	    
	    Input<Call> input = incoming.input(new InputCommand(grammar));
	    outgoing.output("clue");
	    
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.MATCH);
	    assertEquals(complete.getInterpretation(),"clue");
	    
	    outgoing.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testNoInput() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    
	    InputCommand command = new InputCommand(new SimpleGrammar("yes,no"));
	    command.setInitialTimeout(2000);
	    Input<Call> input = incoming.input(command);
	    
	    Thread.sleep(2500);
	    
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.INI_TIMEOUT);
	    
	    outgoing.hangup();
	    waitForEvents();
	}
		
	@Test
	public void testNoMatchWithConfidence1() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    
	    InputCommand command = new InputCommand(new SimpleGrammar("yes,no"));
	    command.setMinConfidence(1f);
	    Input<Call> input = incoming.input(command);
	    
	    outgoing.output("lalala");
	    
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.NO_MATCH);
	    
	    outgoing.hangup();
	    waitForEvents();
	}
	
	@Test
	public void testCompleteIfFarsideHangsUp() throws Exception {
		
	    OutgoingCall outgoing = dial();
	    
	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();
	    
	    
	    InputCommand command = new InputCommand(new SimpleGrammar("yes,no"));
	    command.setMinConfidence(1f);
	    Input<Call> input = incoming.input(command);
	    
	    outgoing.hangup();
	    
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.DISCONNECT);	    
	}
}
