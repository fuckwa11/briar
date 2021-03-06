package org.briarproject.plugins.modem;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.briarproject.api.ContactId;
import org.briarproject.api.TransportId;
import org.briarproject.api.TransportProperties;
import org.briarproject.api.crypto.PseudoRandom;
import org.briarproject.api.plugins.TransportConnectionReader;
import org.briarproject.api.plugins.TransportConnectionWriter;
import org.briarproject.api.plugins.duplex.DuplexPlugin;
import org.briarproject.api.plugins.duplex.DuplexPluginCallback;
import org.briarproject.api.plugins.duplex.DuplexTransportConnection;
import org.briarproject.util.StringUtils;

class ModemPlugin implements DuplexPlugin, Modem.Callback {

	static final TransportId ID = new TransportId("modem");

	private static final Logger LOG =
			Logger.getLogger(ModemPlugin.class.getName());

	private final Executor ioExecutor;
	private final ModemFactory modemFactory;
	private final SerialPortList serialPortList;
	private final DuplexPluginCallback callback;
	private final int maxFrameLength;
	private final long maxLatency, pollingInterval;
	private final boolean shuffle; // Used to disable shuffling for testing

	private volatile boolean running = false;
	private volatile Modem modem = null;

	ModemPlugin(Executor ioExecutor, ModemFactory modemFactory,
			SerialPortList serialPortList, DuplexPluginCallback callback,
			int maxFrameLength, long maxLatency, long pollingInterval,
			boolean shuffle) {
		this.ioExecutor = ioExecutor;
		this.modemFactory = modemFactory;
		this.serialPortList = serialPortList;
		this.callback = callback;
		this.maxFrameLength = maxFrameLength;
		this.maxLatency = maxLatency;
		this.pollingInterval = pollingInterval;
		this.shuffle = shuffle;
	}

	public TransportId getId() {
		return ID;
	}

	public int getMaxFrameLength() {
		return maxFrameLength;
	}

	public long getMaxLatency() {
		return maxLatency;
	}

	public boolean start() {
		for(String portName : serialPortList.getPortNames()) {
			if(LOG.isLoggable(INFO))
				LOG.info("Trying to initialise modem on " + portName);
			modem = modemFactory.createModem(this, portName);
			try {
				if(!modem.start()) continue;
				if(LOG.isLoggable(INFO))
					LOG.info("Initialised modem on " + portName);
				running = true;
				return true;
			} catch(IOException e) {
				if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			}
		}
		return false;
	}

	public void stop() {
		running = false;
		if(modem != null) {
			try {
				modem.stop();
			} catch(IOException e) {
				if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			}
		}
	}

	public boolean isRunning() {
		return running;
	}

	// FIXME: Don't poll this plugin
	public boolean shouldPoll() {
		return true;
	}

	public long getPollingInterval() {
		return pollingInterval;
	}

	public void poll(Collection<ContactId> connected) {
		if(!connected.isEmpty()) return; // One at a time please
		ioExecutor.execute(new Runnable() {
			public void run() {
				poll();
			}
		});
	}

	private void poll() {
		if(!running) return;
		// Get the ISO 3166 code for the caller's country
		String callerIso = callback.getLocalProperties().get("iso3166");
		if(StringUtils.isNullOrEmpty(callerIso)) return;
		// Call contacts one at a time in a random order
		Map<ContactId, TransportProperties> remote =
				callback.getRemoteProperties();
		List<ContactId> contacts = new ArrayList<ContactId>(remote.keySet());
		if(shuffle) Collections.shuffle(contacts);
		Iterator<ContactId> it = contacts.iterator();
		while(it.hasNext() && running) {
			ContactId c = it.next();
			// Get the ISO 3166 code for the callee's country
			TransportProperties properties = remote.get(c);
			if(properties == null) continue;
			String calleeIso = properties.get("iso3166");
			if(StringUtils.isNullOrEmpty(calleeIso)) continue;
			// Get the callee's phone number
			String number = properties.get("number");
			if(StringUtils.isNullOrEmpty(number)) continue;
			// Convert the number into direct dialling form
			number = CountryCodes.translate(number, callerIso, calleeIso);
			if(number == null) continue;
			// Dial the number
			try {
				if(!modem.dial(number)) continue;
			} catch(IOException e) {
				if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				if(resetModem()) continue;
				break;
			}
			LOG.info("Outgoing call connected");
			ModemTransportConnection conn = new ModemTransportConnection();
			callback.outgoingConnectionCreated(c, conn);
			try {
				conn.waitForDisposal();
			} catch(InterruptedException e) {
				LOG.warning("Interrupted while polling");
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	boolean resetModem() {
		if(!running) return false;
		for(String portName : serialPortList.getPortNames()) {
			if(LOG.isLoggable(INFO))
				LOG.info("Trying to initialise modem on " + portName);
			modem = modemFactory.createModem(this, portName);
			try {
				if(!modem.start()) continue;
				if(LOG.isLoggable(INFO))
					LOG.info("Initialised modem on " + portName);
				return true;
			} catch(IOException e) {
				if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			}
		}
		running = false;
		return false;
	}

	public DuplexTransportConnection createConnection(ContactId c) {
		if(!running) return null;
		// Get the ISO 3166 code for the caller's country
		String fromIso = callback.getLocalProperties().get("iso3166");
		if(StringUtils.isNullOrEmpty(fromIso)) return null;
		// Get the ISO 3166 code for the callee's country
		TransportProperties properties = callback.getRemoteProperties().get(c);
		if(properties == null) return null;
		String toIso = properties.get("iso3166");
		if(StringUtils.isNullOrEmpty(toIso)) return null;
		// Get the callee's phone number
		String number = properties.get("number");
		if(StringUtils.isNullOrEmpty(number)) return null;
		// Convert the number into direct dialling form
		number = CountryCodes.translate(number, fromIso, toIso);
		if(number == null) return null;
		// Dial the number
		try {
			if(!modem.dial(number)) return null;
		} catch(IOException e) {
			if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			resetModem();
			return null;
		}
		return new ModemTransportConnection();
	}

	public boolean supportsInvitations() {
		return false;
	}

	public DuplexTransportConnection createInvitationConnection(PseudoRandom r,
			long timeout) {
		throw new UnsupportedOperationException();
	}

	public void incomingCallConnected() {
		LOG.info("Incoming call connected");
		callback.incomingConnectionCreated(new ModemTransportConnection());
	}

	private class ModemTransportConnection
	implements DuplexTransportConnection {

		private final AtomicBoolean halfClosed = new AtomicBoolean(false);
		private final AtomicBoolean closed = new AtomicBoolean(false);
		private final CountDownLatch disposalFinished = new CountDownLatch(1);
		private final Reader reader = new Reader();
		private final Writer writer = new Writer();

		public TransportConnectionReader getReader() {
			return reader;
		}

		public TransportConnectionWriter getWriter() {
			return writer;
		}

		private void hangUp(boolean exception) {
			LOG.info("Call disconnected");
			try {
				modem.hangUp();
			} catch(IOException e) {
				if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
				exception = true;
			}
			if(exception) resetModem();
			disposalFinished.countDown();
		}

		private void waitForDisposal() throws InterruptedException {
			disposalFinished.await();
		}

		private class Reader implements TransportConnectionReader {

			public int getMaxFrameLength() {
				return maxFrameLength;
			}

			public long getMaxLatency() {
				return maxLatency;
			}

			public InputStream getInputStream() throws IOException {
				return modem.getInputStream();
			}

			public void dispose(boolean exception, boolean recognised) {
				if(halfClosed.getAndSet(true) || exception)
					if(!closed.getAndSet(true)) hangUp(exception);
			}
		}

		private class Writer implements TransportConnectionWriter {

			public int getMaxFrameLength() {
				return maxFrameLength;
			}

			public long getMaxLatency() {
				return maxLatency;
			}

			public long getCapacity() {
				return Long.MAX_VALUE;
			}

			public OutputStream getOutputStream() throws IOException {
				return modem.getOutputStream();
			}

			public void dispose(boolean exception) {
				if(halfClosed.getAndSet(true) || exception)
					if(!closed.getAndSet(true)) hangUp(exception);
			}
		}
	}
}
