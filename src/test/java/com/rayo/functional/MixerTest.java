package com.rayo.functional;

import java.util.UUID;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.ActiveSpeakerEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;

public class MixerTest extends MohoBasedIntegrationTest {

	// test https://voxeolabs.atlassian.net/browse/RAYO-4
	@Test
	public void testJoinSpeak() {

		String mixerName = UUID.randomUUID().toString();
		OutgoingCall outgoing1 = null;
		OutgoingCall outgoing2 = null;

		Mixer mixer = null;
		outgoing1 = dial();
		IncomingCall incoming1 = getIncomingCall();
		incoming1.answer();
		waitForEvents();

		outgoing2 = dial();
		IncomingCall incoming2 = getIncomingCall();
		incoming2.answer();
		waitForEvents();

		// create mixer
		MixerEndpoint mixerEndpoint = mohoRemote.createMixerEndpoint();
		mixer = mixerEndpoint.create(mixerName, null);

		Joint joint = incoming1.join(mixer, JoinType.BRIDGE, Direction.DUPLEX);

		try {
			joint.get();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		outgoing1.input(new InputCommand(createSRGSGrammar("yes,no")));
		mixer.output(new OutputCommand(createOutputDocument("yes")));
		waitForEvents(4000);
		assertReceived(InputCompleteEvent.class, outgoing1);

		Joint joint2 = incoming2.join(mixer, JoinType.BRIDGE, Direction.DUPLEX);

		try {
			joint2.get();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		outgoing1.input(new InputCommand(createSRGSGrammar("yes,no")));
		outgoing2.input(new InputCommand(createSRGSGrammar("yes,no")));
		mixer.output(new OutputCommand(createOutputDocument("yes")));
		waitForEvents(4000);
		assertReceived(InputCompleteEvent.class, outgoing1);
		assertReceived(InputCompleteEvent.class, outgoing2);

		outgoing1.hangup();
		outgoing2.hangup();
		mixer.disconnect();
		waitForEvents();
	}

	@Test
	public void testActiveSpeakerEvent() {

		String mixerName = UUID.randomUUID().toString();
		OutgoingCall outgoing1 = dial();
		IncomingCall incoming1 = getIncomingCall();
		incoming1.answer();
		waitForEvents();

		// create mixer
		Mixer mixer = createMixer(mixerName);

		Joint joint = incoming1.join(mixer, JoinType.BRIDGE, Direction.DUPLEX);

		try {
			joint.get();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		outgoing1.output(new OutputCommand(createOutputDocument("yes")));
		waitForEvents(4000);
		assertReceived(ActiveSpeakerEvent.class, mixer);

		outgoing1.hangup();
		mixer.disconnect();
		waitForEvents();
	}
}
