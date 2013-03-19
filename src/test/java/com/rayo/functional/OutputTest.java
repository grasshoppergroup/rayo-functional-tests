package com.rayo.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

public class OutputTest extends MohoBasedIntegrationTest {

	static final String audioURL = "<audio src=\"http://www.phono.com/audio/troporocks.mp3\"/>"; // 7-8 seconds length

	@Test
	public void testOutputCompleteReceived() {

	    dial();

	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();

	    Output<Call> output = incoming.output(new OutputCommand(createOutputDocument("Hello World")));
	    assertReceived(OutputCompleteEvent.class, output);

	    incoming.hangup();
	    waitForEvents();
	}

	@Test
	public void testOutputSomethingWithTTS() {

		OutgoingCall outgoing = dial();

		IncomingCall call = getIncomingCall();
		assertNotNull(call);
		call.answer();

		Input<Call> input = call.input(new InputCommand(createSRGSGrammar("yes,no")));

		Output<Call> output = outgoing.output(new OutputCommand(createOutputDocument("yes")));
		assertReceived(OutputCompleteEvent.class, output);
		InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
		assertEquals("yes", complete.getInterpretation());

		outgoing.hangup();
		waitForEvents();
	}

	@Test
	public void testOutputAudioURL() throws Exception {

		dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		Output<Call> output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));
		waitForEvents(1000);
		assertNotReceived(OutputCompleteEvent.class, output);

		waitForEvents(6000);
		OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
		assertEquals(Cause.END, complete.getCause());

		incoming.hangup();
		waitForEvents();
	}

	@Test
	public void testOutputSSML() throws Exception {

		OutgoingCall outgoing = dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		Input<Call> input = incoming.input(new InputCommand(createSRGSGrammar("one hundred,ireland")));

		String text = "<say-as interpret-as=\"ordinal\">100</say-as>";
		OutputCommand outputCommand = new OutputCommand(createOutputDocument(text));
		outgoing.output(outputCommand);

		InputCompleteEvent<?> complete = assertReceived(InputCompleteEvent.class, input);
		assertEquals("one hundred", complete.getInterpretation());

		incoming.hangup();
		waitForEvents();
	}


	@Test
	public void testOutputSSMLDigits() throws Exception {

		OutgoingCall outgoing = dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		String text = "<audio src=\"digits/3\"/>";
		OutputCommand outputCommand = new OutputCommand(createOutputDocument(text));
		outgoing.output(outputCommand);

		OutputCompleteEvent<Call> complete = assertReceived(OutputCompleteEvent.class, outgoing);
		assertEquals(Cause.ERROR, complete.getCause());
		//assertTrue(complete.getErrorText().contains("Could not find the Resource's URI"));

		incoming.hangup();
		waitForEvents();
	}

	@Test
	public void testErrorOnInvalidSsml() throws Exception {

		OutgoingCall outgoing = dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		Input<Call> input = incoming.input(new InputCommand(createSRGSGrammar("one hundred,ireland")));

		String text = "<output-as interpret-as=\"ordinal\">100</output-as>";
		OutputCommand outputCommand = new OutputCommand(createOutputDocument(text));
		Output<Call> output = outgoing.output(outputCommand);
		waitForEvents();

		OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
		assertEquals(Cause.ERROR, complete.getCause());

		incoming.hangup();
		waitForEvents();
	}

	@Test
	public void testAudioPlayback() throws Exception {

		dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		Output<Call> output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));
		waitForEvents(1000);
		assertNotReceived(OutputCompleteEvent.class, output);

		output.pause();
		waitForEvents(6000);
		output.resume();
		waitForEvents(1000);

		output.stop();

		OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
		assertEquals(Cause.CANCEL, complete.getCause());

		incoming.hangup();
		waitForEvents();
	}

	@Test
	public void testAudioSeek() throws Exception {

		dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		// Forward
		Output<Call> output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));
		waitForEvents(1000);
		output.move(true, 6000);
		waitForEvents(1000);
		OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output);
		assertEquals(Cause.END, complete.getCause());

		// Backward. Same as above but we move back before finishing up
		output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));
		waitForEvents(1000);
		output.move(true, 6000);
		output.move(false, 4000);
		waitForEvents(1000);
		assertNotReceived(OutputCompleteEvent.class, output);

		incoming.hangup();
		waitForEvents();
	}

	@Test
	public void testAudioSpeedUp() throws Exception {

		dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		long init = System.currentTimeMillis();
		Output<Call> output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));
		// Audio is 7-8 seconds
		output.speed(true);
		output.speed(true);
		output.speed(true);
		output.speed(true);
		output.speed(true);
		output.speed(true);
		waitForEvents(4000);

		OutputCompleteEvent<?> complete = assertReceived(OutputCompleteEvent.class, output, 0);
		long end = System.currentTimeMillis();
		assertEquals(Cause.END, complete.getCause());
		assertTrue(end - init < 7000);

		incoming.hangup();
		waitForEvents();
	}

	@Test
	public void testAudioSlowDown() throws Exception {

		dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		Output<Call> output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));

		output.speed(false);
		output.speed(false);
		output.speed(false);
		output.speed(false);
		output.speed(false);

		waitForEvents(9000);
		assertNotReceived(OutputCompleteEvent.class, output);

		incoming.hangup();
		waitForEvents();
	}


	@Test
	public void testAudioVolume() throws Exception {

		dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);
		incoming.answer();

		Output<Call> output = incoming.output(new OutputCommand(createOutputDocument(audioURL)));

		// Difficult test. Just check that messages don't throw exceptions.
		output.volume(true);
		output.volume(false);

		incoming.hangup();
		waitForEvents();
	}
/*
	@Test
	public void testNewOutputStopsActiveOutput() throws Exception {

	    dial();

	    IncomingCall incoming = getIncomingCall();
	    assertNotNull(incoming);
	    incoming.answer();

	    Output<Call> output1 = incoming.output(new OutputCommand(createOutputDocument(audioURL)));
	    waitForEvents(1000);
	    assertNotReceived(OutputCompleteEvent.class, output1);

	    Output<Call> output2 = incoming.output(new OutputCommand(createOutputDocument("hello")));
	    waitForEvents(1000);

	    OutputCompleteEvent<?> complete1 = assertReceived(OutputCompleteEvent.class, output1, 1);
	    assertEquals(Cause.CANCEL, complete1.getCause());

	    waitForEvents(1000);
	    OutputCompleteEvent<?> complete2 = assertReceived(OutputCompleteEvent.class, output2, 1);
	    assertEquals(Cause.END, complete2.getCause());
	    //BUG: RAYO-66 - A new output run over an existing output cancels both

	    incoming.hangup();
	    waitForEvents();
	}
*/
	@Test
	public void testCantOutputWhileHold() throws Exception {

		OutgoingCall outgoing = dial();

		try {
			IncomingCall incoming = getIncomingCall();
			assertNotNull(incoming);
			incoming.answer();
			Thread.sleep(100);
			incoming.hold();
			try {
				incoming.output(new OutputCommand(createOutputDocument("hello")));
				fail("Expected exception");
			} catch (Exception e) {
				assertTrue(e.getMessage().contains("Call is currently on hold"));
			}
		} finally {
			outgoing.hangup();
		}
	}

	@Test
	public void testCanOutputAfterUnhold() throws Exception {

		OutgoingCall outgoing = dial();

		try {
			IncomingCall incoming = getIncomingCall();
			assertNotNull(incoming);
			incoming.answer();
			Thread.sleep(100);
			incoming.hold();
			Thread.sleep(100);
			incoming.unhold();
			Thread.sleep(100);
			incoming.output(new OutputCommand(createOutputDocument("hello")));
			Thread.sleep(100);
		} finally {
			outgoing.hangup();
		}
	}

	@Test
	public void testCantOutputNonAnswered() throws Exception {

		OutgoingCall outgoing = dial();

		IncomingCall incoming = getIncomingCall();
		assertNotNull(incoming);

		try {
			incoming.output(new OutputCommand(createOutputDocument("hello")));
			fail("Expected exception");
		} catch(Exception e) {
			assertTrue(e.getMessage().contains("The call has not been answered"));
		}
		Thread.sleep(100);
		MohoCallCompleteEvent endOutgoing = assertReceived(MohoCallCompleteEvent.class, outgoing);
		assertEquals(com.voxeo.moho.event.CallCompleteEvent.Cause.DECLINE, endOutgoing.getCause());
		MohoCallCompleteEvent endIncoming = assertReceived(MohoCallCompleteEvent.class, incoming);
		assertEquals(com.voxeo.moho.event.CallCompleteEvent.Cause.ERROR, endIncoming.getCause());
	}
}
