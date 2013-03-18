package com.rayo.functional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.media.mscontrol.join.Joinable.Direction;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.rayo.functional.base.MohoBasedIntegrationTest;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.MohoInputCompleteEvent;
import com.voxeo.moho.common.event.MohoJoinCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.UnjoinCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;

public class JoinTest extends MohoBasedIntegrationTest {

  @Test
  public void testJoinBridge() {

    OutgoingCall outgoing1 = dial();
    IncomingCall incoming1 = getIncomingCall();
    incoming1.answer();

    OutgoingCall outgoing2 = dial();
    IncomingCall incoming2 = getIncomingCall();
    incoming2.join(incoming1, JoinType.BRIDGE, Direction.DUPLEX);

    assertReceived(MohoJoinCompleteEvent.class, incoming1);
    assertReceived(MohoJoinCompleteEvent.class, incoming2);

    incoming1.input(new InputCommand(createSRGSGrammar("yes,no")));
    incoming2.input(new InputCommand(createSRGSGrammar("yes,no")));

    outgoing2.output(new OutputCommand(createOutputDocument("yes")));
    outgoing1.output(new OutputCommand(createOutputDocument("no")));

    MohoInputCompleteEvent<?> complete = assertReceived(MohoInputCompleteEvent.class, incoming1);
    Assert.assertEquals("no", complete.getUtterance());

    complete = assertReceived(MohoInputCompleteEvent.class, incoming2);
    Assert.assertEquals("yes", complete.getUtterance());

    incoming1.hangup();
    incoming2.hangup();
    waitForEvents();
  }

  @Test
  public void testJoinBridgeFailsIfAnotherJoinIsInProgress() {

    dial();
    IncomingCall incoming1 = getIncomingCall();
    incoming1.answer();
    waitForEvents();

    OutgoingCall outgoing2 = dial();
    waitForEvents(3000);
    try {
      incoming1.join(outgoing2, JoinType.BRIDGE, Direction.DUPLEX);
    }
    catch (Exception e) {
      assertTrue(e.getMessage().contains("other join operation in process"));
    }
  }

  @Test
  @Ignore
  // TODO: #1600683
  public void testJoinOutgoingCallsFails() {

    // #1579867
    // This test tries to join two outgoing calls that haven't been answered.
    // This should not be allowed

    OutgoingCall outgoing1 = dial();
    // waitForEvents(200);
    // OutgoingCall nobother = dial();
    waitForEvents(200);
    OutgoingCall outgoing2 = dial();
    try {
      outgoing1.join(outgoing2, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
      fail("Expected exception");
    }
    catch (Exception e) {
      assertTrue(e.getMessage().contains("None of the calls you are trying to join have been answered"));
    }

    outgoing1.hangup();
    outgoing2.hangup();
  }

  @Test
  // TODO: #1600683
  public void testJoinOutgoingCalls() {

    // This test tries two join two outgoing calls that already have been
    // answered.

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
    Input<Call> input1 = outgoing1.input(new InputCommand(createSRGSGrammar("yes,no")));
    outgoing2.output(new OutputCommand(createOutputDocument("yes")));
    waitForEvents();
    assertReceived(InputCompleteEvent.class, input1);

    // Asserts the other leg does not receive media
    Input<Call> input2 = outgoing2.input(new InputCommand(createSRGSGrammar("yes,no")));
    outgoing1.output(new OutputCommand(createOutputDocument("yes")));
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
    Input<Call> input2 = outgoing2.input(new InputCommand(createSRGSGrammar("yes,no")));
    outgoing1.output(new OutputCommand(createOutputDocument("yes")));
    waitForEvents();
    assertReceived(InputCompleteEvent.class, input2);

    // Assert leg does receive media
    Input<Call> input1 = outgoing1.input(new InputCommand(createSRGSGrammar("yes,no")));
    outgoing2.output(new OutputCommand(createOutputDocument("yes")));
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
    waitForEvents(500);

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

      incoming1.input(new InputCommand(createSRGSGrammar("yes,no")));
      throw new AssertionError("Expected error");
    }
    catch (Exception e) {
      assertTrue(e.getMessage().contains("Media operations are not allowed in the current call status"));
    }

    try {
      incoming1.input(new InputCommand(createSRGSGrammar("hello")));
      throw new AssertionError("Expected error");
    }
    catch (Exception e) {
      assertTrue(e.getMessage().contains("Media operations are not allowed in the current call status"));
    }

    incoming1.hangup();
    incoming2.hangup();
    waitForEvents();
  }

  @Test
  // TODO: This only works because output/input do auto join. Rayo currently
  // does not
  // support joining the media server. It always require a call id to join
  // TODO: #1594119
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
    Input<Call> input = outgoing2.input(new InputCommand(createSRGSGrammar("yes,no")));
    incoming2.output(new OutputCommand(createOutputDocument("yes")));
    waitForEvents(2000);
    assertReceived(InputCompleteEvent.class, outgoing2);

    incoming2.hangup();
    waitForEvents();
  }

  // test https://voxeolabs.atlassian.net/browse/RAYO-1
  @Test
  public void testJoinedDuplextRejoinReceived() {
    OutgoingCall outgoing1 = dial();
    IncomingCall incoming1 = getIncomingCall();
    incoming1.answer();
    waitForEvents();

    OutgoingCall outgoing2 = dial();
    IncomingCall incoming2 = getIncomingCall();
    incoming2.answer();
    waitForEvents();

    Joint joint = incoming1.join(outgoing2, JoinType.BRIDGE, Direction.DUPLEX);

    try {
      joint.get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    outgoing1.input(new InputCommand(createSRGSGrammar("yes,no")));
    incoming2.output(new OutputCommand(createOutputDocument("yes")));
    waitForEvents(4000);
    assertReceived(InputCompleteEvent.class, outgoing1);

    incoming1.join(outgoing2, JoinType.BRIDGE, Direction.RECV);

    try {
      joint.get();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }

    outgoing1.input(new InputCommand(createSRGSGrammar("yes,no")));
    incoming2.output(new OutputCommand(createOutputDocument("yes")));
    waitForEvents(4000);
    assertReceived(InputCompleteEvent.class, outgoing1);

    outgoing1.hangup();
    outgoing2.hangup();
    waitForEvents();
  }

  @Test
  public void testJoinAndHold() {

    OutgoingCall outgoing1 = dial();
    IncomingCall incoming1 = getIncomingCall();
    incoming1.answer();

    OutgoingCall outgoing2 = dial();
    IncomingCall incoming2 = getIncomingCall();
    incoming2.join(incoming1, JoinType.BRIDGE, Direction.DUPLEX);

    assertReceived(MohoJoinCompleteEvent.class, incoming1);
    assertReceived(MohoJoinCompleteEvent.class, incoming2);

    incoming1.input(new InputCommand(createSRGSGrammar("yes,no")));
    incoming2.input(new InputCommand(createSRGSGrammar("yes,no")));

    incoming1.hold();
    outgoing1.output(new OutputCommand(createOutputDocument("no")));
    waitForEvents();
    assertNotReceived(MohoInputCompleteEvent.class, incoming1);

    incoming1.unhold();
    outgoing1.output(new OutputCommand(createOutputDocument("no")));
    waitForEvents();

    MohoInputCompleteEvent<?> complete = assertReceived(MohoInputCompleteEvent.class, incoming1);
    Assert.assertEquals("no", complete.getUtterance());

    incoming1.hangup();
    incoming2.hangup();
    waitForEvents();
  }

  @Test
  public void testOutboundHangupBeforeAnswer() {
    OutgoingCall outgoing1 = dial();
    IncomingCall incoming1 = getIncomingCall();


    outgoing1.hangup();

    waitForEvents(3000);

    Assert.assertEquals(Call.State.DISCONNECTED, outgoing1.getCallState());
    Assert.assertEquals(Call.State.DISCONNECTED, incoming1.getCallState());

    waitForEvents();
  }
}
