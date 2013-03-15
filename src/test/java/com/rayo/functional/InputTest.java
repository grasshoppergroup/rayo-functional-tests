package com.rayo.functional;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
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

	public static String grxml = "<grammar xmlns=\"http://www.w3.org/2001/06/grammar\" root=\"MAINRULE\"> " +
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

		String yesnoGrammar =
			"<grammar xmlns=\"http://www.w3.org/2001/06/grammar\" root=\"yesno\">\n" +
			"  <rule id=\"yesno\">\n" +
			"    <one-of>\n" +
			"      <item>yes</item>\n" +
			"      <item>no</item>\n" +
			"    </one-of>\n" +
			"  </rule>\n" +
			"</grammar>";
		Input<Call> input = incoming.input(new InputCommand(new Grammar("application/srgs+xml", yesnoGrammar)));

		String toSpeak =
			"<document content-type='application/ssml+xml'>\n" +
			"  <![CDATA[<?xml version=\"1.0\"?>\n" +
			"  <!DOCTYPE speak PUBLIC \"-//W3C//DTD SYNTHESIS 1.0//EN\"\n" +
			"                   \"http://www.w3.org/TR/speech-synthesis/synthesis.dtd\">\n" +
			"  <speak version=\"1.0\"\n" +
			"         xmlns=\"http://www.w3.org/2001/10/synthesis\"\n" +
			"         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
			"         xsi:schemaLocation=\"http://www.w3.org/2001/10/synthesis\n" +
			"                     http://www.w3.org/TR/speech-synthesis/synthesis.xsd\"\n" +
			"         xml:lang=\"en-US\">\n" +
			"    yes\n" +
			"  </speak>\n" +
			" ]]></document>\n";
		outgoing.output(toSpeak);

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
	public void testInputMultipleDTMF() {

	    OutgoingCall outgoing = dial();

	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();

	    InputCommand command = new InputCommand(new SimpleGrammar("[2 DIGITS]"));
	    command.setInputMode(InputMode.DTMF);
	    Input<Call> input = incoming.input(command);

        String text = "<speak><audio src=\"dtmf:75\"/></speak>";
        OutputCommand outputCommand = new OutputCommand(new TextToSpeechResource(text));
	    outgoing.output(outputCommand);

	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.MATCH);
	    assertEquals(complete.getInterpretation(),"dtmf-7 dtmf-5");
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
	    Thread.sleep(100);
	    outgoing.hangup();
	    Thread.sleep(200);
	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    assertEquals(complete.getCause(), Cause.DISCONNECT);
	}

	@Test
	@Ignore
	// Does not work on Gatway's Prism build. Needs to be updated.
	// Not sure if related with the build but it works locally.
	public void testDifferentRecognizer() throws Exception {

	    OutgoingCall outgoing = dial();

	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();

	    InputCommand command = new InputCommand(new SimpleGrammar("si,no"));
	    command.setMinConfidence(1f);
	    command.setRecognizer("es-es");
	    Input<Call> input = incoming.input(command);

	    outgoing.output("si");

	    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
	    // no support for recognizers so it will fail. But the important thing is that #1532827 is fixed
	    assertEquals(complete.getCause(), Cause.NO_MATCH);

	    outgoing.hangup();
	    waitForEvents();
	}

	@Test
	public void testCantInputNonAnswered() throws Exception {

	    OutgoingCall outgoing = dial();

	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);

	    try {
	    	incoming.input("yes,no");
	    	fail("Expected exception");
	    } catch(Exception e) {
	    	assertTrue(e.getMessage().contains("The call has not been answered"));
	    }
	    Thread.sleep(100);
	    MohoCallCompleteEvent endIncoming = assertReceived(MohoCallCompleteEvent.class, incoming);
	    assertEquals(endIncoming.getCause(), com.voxeo.moho.event.CallCompleteEvent.Cause.ERROR);
	    MohoCallCompleteEvent endOutgoing = assertReceived(MohoCallCompleteEvent.class, outgoing);
	    assertEquals(endOutgoing.getCause(), com.voxeo.moho.event.CallCompleteEvent.Cause.DECLINE);
	}

	@Test
	@Ignore
	// Not supported on current Gateway prism build
	public void testMaxSilence() throws Exception {

		// Tests that max silence command has any effect. The input should be
		// stop only after maximum silence has timed out

	    OutgoingCall outgoing = dial();

	    try {
		    IncomingCall incoming = getIncomingCall();
		    assertNotNull(incoming);
		    incoming.answer();

	    	InputCommand inputCommand = new InputCommand(new SimpleGrammar("yes,no"));
	    	inputCommand.setSpeechIncompleteTimeout(2000);

	    	Input<Call> input = incoming.input(inputCommand);
	    	waitForEvents(1000);
	    	assertNotReceived(InputCompleteEvent.class, input);
	    	waitForEvents(1000);
		    InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
		    assertEquals(complete.getCause(), Cause.NO_MATCH);

	    } finally {
	    	outgoing.hangup();
	    }
	}
}
