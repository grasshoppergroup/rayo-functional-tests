package com.rayo.functional.base;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.core.verb.Ssml;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.MixerEndpoint;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.Participant;
import com.voxeo.moho.common.event.MohoMediaCompleteEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.MediaOperation;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;
import com.voxeo.moho.remote.sample.SimpleAuthenticateCallbackImpl;

public abstract class MohoBasedIntegrationTest {

	private Logger log = LoggerFactory.getLogger(MohoBasedIntegrationTest.class);
	
	private LinkedBlockingQueue<IncomingCall> callsQueue = new LinkedBlockingQueue<IncomingCall>();
	private List<Event> events = new ArrayList<Event>();

	private List<OutgoingCall> outgoingCalls = new ArrayList<OutgoingCall>();
	private List<IncomingCall> incomingCalls = new ArrayList<IncomingCall>();

	protected MohoRemote mohoRemote;

	private int retries = 6;
	private int waitTime = 3000;
	private int next = 0;

	protected String xmppUsername;
	protected String xmppPassword;
	protected List<String> sipDialUris = new ArrayList<String>();
	protected String xmppServer;
	protected String rayoServer;
	protected String dtmfUriPrefix;

	@Before
	public void setup() {
		
		setup(xmppUsername, xmppPassword);
	}

	public void setup(String username, String password) {

		callsQueue.clear();
		events.clear();
		incomingCalls.clear();
		outgoingCalls.clear();

		mohoRemote = new MohoRemoteImpl();
		mohoRemote.addObserver(new MohoObserver(this));

		loadProperties();
		if (username == null) {
			username = xmppUsername;
		}
		if (password == null) {
			password = xmppPassword;
		}

		mohoRemote.connect(new SimpleAuthenticateCallbackImpl(username,
				password, "", "voxeo3"), xmppServer, rayoServer);
		
		try {
			// Wait 1 seconds to get presence events propagated		
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
	}

	protected void loadProperties() {

		xmppUsername = getProperty("xmpp.username", "usera");
		xmppPassword = getProperty("xmpp.password", "1");
    xmppServer = getProperty("xmpp.server", "localhost");
    rayoServer = getProperty("rayo.server", "localhost");
    String[] uris = getProperty("sip.dial.uri", "sip:usera@localhost").split(",");
		sipDialUris.addAll(Arrays.asList(uris));
		dtmfUriPrefix = getProperty("dtmf.uri.prefix", "dtmf:");
	}

	protected String getProperty(String property, String defaultValue) {

		String result = System.getProperty(property);
		if (result == null) {
			result = defaultValue;
		} else {
			log.trace(String.format(
					"Using system property value for [%s]:[%s]", property,
					result));
		}
		return result;
	}

	@After
	public void shutdown() {

		try {
			for (OutgoingCall call : outgoingCalls) {
				try {
					call.disconnect();
				} catch (Exception e) {
					log.error(String.format(
							"[ERROR] Problem disconnecting outgoing call [%s]",
							call.getId()));
				}
			}

			mohoRemote.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public OutgoingCall dial() {
		
		return dial(getSipDialUri());
	}
	
	public OutgoingCall dial(String uri) {

		CallableEndpoint endpoint = (CallableEndpoint) mohoRemote
				.createEndpoint(URI.create(uri));
		Call call = endpoint.createCall("sip:test@test.com");
		call.addObserver(new MohoObserver(this));
		call.join();

		outgoingCalls.add((OutgoingCall) call);

		return (OutgoingCall) call;
	}
	
	 public Mixer createMixer(String name) {
	   MixerEndpoint mixerEndpoint = mohoRemote.createMixerEndpoint();
	   Mixer mixer = mixerEndpoint.create(name, null);
	   mixer.addObserver(new MohoObserver(this));
	   return mixer;
	  }

	protected synchronized IncomingCall getIncomingCall() {

		try {
			IncomingCall call = callsQueue.poll(15000, TimeUnit.MILLISECONDS);
			if (call != null) {
				incomingCalls.add(call);
			}
			return call;
		} catch (InterruptedException e) {
			return null;
		}
	}

	protected <T> T assertReceived(Class<T> eventClass, MediaOperation operation) {

		return assertReceived(eventClass, operation, retries);
	}

	protected <T> T assertReceived(Class<T> eventClass,
			MediaOperation operation, int retries) {

		int i = 0;
		do {
			synchronized (events) {
				T evt = null;
				log.trace(String.format(
						"[%s] Asserting event [%s] on operation [%s]. Try %s",
						DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
						eventClass, operation, i + 1));
				for (Event event : events) {
					log.trace(String.format(
							"[%s] Checking if event [%s] is assignable.",
							DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
							event.getClass()));
					if (eventClass.isAssignableFrom(event.getClass())) {
						log.trace(String
										.format("[%s] Checking if event [%s] is media complete.",
												DateFormatUtils.format(
														new Date(),
														"hh:mm:ss.SSS"), event
														.getClass()));
						if (event instanceof MohoMediaCompleteEvent) {
							System.out
									.println(String
											.format("[%s] Checking if event operation [%s] is same as [%s].",
													DateFormatUtils.format(
															new Date(),
															"hh:mm:ss.SSS"),
													((MohoMediaCompleteEvent) event)
															.getMediaOperation(),
													operation));
							if (((MohoMediaCompleteEvent) event)
									.getMediaOperation() == operation) {
								log.trace(String.format(
										"[%s] Found match [%s].",
										DateFormatUtils.format(new Date(),
												"hh:mm:ss.SSS"), (T) event));
								evt = (T) event;
								break;
							}
						}
					}
				}
				log.trace(String.format(
								"[%s] Checking if [%s] is null.",
								DateFormatUtils.format(new Date(),
										"hh:mm:ss.SSS"), evt));
				if (evt != null) {
					log.trace(String.format(
							"[%s] Removing [%s] from events.",
							DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
							evt));
					events.remove(evt);
					log.trace(String.format("[%s] Returning [%s].",
							DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
							evt));
					return evt;
				}
				i++;
			}
			if (i < retries) {
				waitForEvents(waitTime);
			}
		} while (i < retries);
		log.trace("Call event not found. Throwing exception");
		throw new AssertionError("Call Event not found");
	}

	protected boolean assertNotReceived(Class eventClass,
			MediaOperation operation) {

		try {
			System.out
					.println(String
							.format("[%s] Asserting that event [%s] was not received for media operation [%s].",
									DateFormatUtils.format(new Date(),
											"hh:mm:ss.SSS"), eventClass,
									operation));
			assertReceived(eventClass, operation, 0);
		} catch (AssertionError e) {
			log.error(String.format("[%s] Assertion error: [%s].",
					DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
					e.getMessage()));
			return true;
		}
		throw new AssertionError("Call Event found and was not expected");
	}

	protected <T> T assertReceived(Class<T> eventClass, Participant call) {

		return assertReceived(eventClass, call, retries);
	}

	protected <T> T assertReceived(Class<T> eventClass, Participant call, int retries) {

		int i = 0;
		do {
			synchronized (events) {
				T evt = null;
				log.debug(String.format(
						"[%s] Asserting event [%s] on call [%s]. Try %s",
						DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"),
						eventClass, call.getId(), i + 1));
				for (Event event : events) {
					if (eventClass.isAssignableFrom(event.getClass())) {
						if (event.getSource() == call) {
							evt = (T) event;
							break;
						}
					}
				}
				if (evt != null) {
					events.remove(evt);
					return evt;
				}
				i++;
			}
			waitForEvents(waitTime);
		} while (i < retries);
		log.error("Call event not found");
		throw new AssertionError("Call Event not found");
	}

	protected boolean assertNotReceived(Class eventClass, Call call) {

		try {
			assertReceived(eventClass, call, 0);
			throw new AssertionError("Call Event found");
		} catch (AssertionError e) {
			return true;
		}
	}

	protected void waitForEvents() {

		waitForEvents(1000);
	}

	protected void waitForEvents(int time) {

		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {

		}
	}

	void addCall(IncomingCall call) {

		log.debug(String.format("[%s] Adding incoming call [%s]",
				DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"), call));
		callsQueue.add(call);
	}

	void addEvent(Event event) {

		log.debug(String.format("[%s] Adding event [%s]",
				DateFormatUtils.format(new Date(), "hh:mm:ss.SSS"), event));
		synchronized (events) {
			events.add(event);
		}
	}

	protected AudibleResource resolveAudio(final Ssml item) {

		return new AudibleResource() {
			public URI toURI() {
				return item.toUri();
			}
		};
	}

	public String getXmppUsername() {
		return xmppUsername;
	}

	public void setXmppUsername(String xmppUsername) {
		this.xmppUsername = xmppUsername;
	}

	public String getXmppPassword() {
		return xmppPassword;
	}

	public void setXmppPassword(String xmppPassword) {
		this.xmppPassword = xmppPassword;
	}

	public String getSipDialUri() {

		String uri = sipDialUris.get(next);
		next = (next+1);
		if (next >= sipDialUris.size()) {
			next = 0;
		}
		return uri;
	}

	public String getDtmfUri(String digit) {
		return dtmfUriPrefix + digit;
	}

	public String getXmppServer() {
		return xmppServer;
	}

	public void setXmppServer(String xmppServer) {
		this.xmppServer = xmppServer;
	}

	public String getRayoServer() {
		return rayoServer;
	}

	public void setRayoServer(String rayoServer) {
		this.rayoServer = rayoServer;
	}

        public String createOutputDocument(String text) {
                return
                        "<document content-type='application/ssml+xml'>\n" +
                        "  <speak version=\"1.0\"\n" +
                        "         xmlns=\"http://www.w3.org/2001/10/synthesis\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"http://www.w3.org/2001/10/synthesis\n" +
                        "                     http://www.w3.org/TR/speech-synthesis/synthesis.xsd\"\n" +
                        "         xml:lang=\"en-US\">" + text + "</speak>\n" +
                        "</document>\n";
        }

        static final String digitsGrammarFormat =
                "<grammar xmlns=\"http://www.w3.org/2001/06/grammar\" mode=\"dtmf\" root=\"digits\">\n" +
                "  <rule id=\"digits\">\n" +
                "    <item repeat=\"%i\"\n" +
                "      <one-of>\n" +
                "        <item>0</item>\n" +
                "        <item>1</item>\n" +
                "        <item>2</item>\n" +
                "        <item>3</item>\n" +
                "        <item>4</item>\n" +
                "        <item>5</item>\n" +
                "        <item>6</item>\n" +
                "        <item>7</item>\n" +
                "        <item>8</item>\n" +
                "        <item>9</item>\n" +
                "        <item>*</item>\n" +
                "        <item>#</item>\n" +
                "      </one-of>\n" +
                "    </item>\n" +
                "  </rule>\n" +
                "</grammar>";

        public Grammar createDigitsGrammar(int numDigits) {
                return new Grammar("application/srgs+xml", String.format(digitsGrammarFormat, numDigits));
        }

        public Grammar createSRGSGrammar(String toMatch) {
                String matchArray[] = toMatch.split(",");
                StringBuilder grammarString = new StringBuilder();
                grammarString.append("<grammar xmlns=\"http://www.w3.org/2001/06/grammar\" root=\"match\">\n");
                grammarString.append("  <rule id=\"match\">\n");
                grammarString.append("    <one-of>\n");
                for (String match : matchArray) {
                        grammarString.append("      <item>").append(match).append("</item>\n");
                }
                grammarString.append("      </one-of>\n");
                grammarString.append("  </rule>\n");
                grammarString.append("</grammar>");
                return new Grammar("application/srgs+xml", grammarString.toString());
        }
}
