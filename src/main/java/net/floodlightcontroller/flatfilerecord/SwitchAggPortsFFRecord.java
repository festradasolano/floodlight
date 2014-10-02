/**
 * Copyright 2013-2014 Felipe Estrada-Solano <festradasolano at gmail>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.floodlightcontroller.flatfilerecord;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.flatfilerecord.util.FilePath;

/**
 * Creates threads and flat files per each OpenFlow switch connected to the
 * controller in order to get and record (as plain text) the aggregate port
 * stats with a defined sample time (default is 10 seconds).
 * 
 * The flat file is created as following:
 * 
 * /[USER HOME]/of-controller-db/floodlight/switchaggports_[DPID].log
 * 
 * [USER HOME] is the user's home directory. [DPID] is the switch DPID in
 * numeric format (long).
 * 
 * Copyright 2013-2014 Felipe Estrada-Solano <festradasolano at gmail>
 * 
 * Distributed under the Apache License, Version 2.0
 * 
 * @author festradasolano
 */
public class SwitchAggPortsFFRecord implements IFloodlightModule,
		IOFSwitchListener, ISwitchAggPortsFFRecordService {

	/**
	 * Builds and returns the port statistics request.
	 */
	private class makePortStatsRequest implements OFSRCallback {

		/*
		 * (non-Javadoc)
		 * 
		 * @see net.floodlightcontroller.flatfilerecord.SwitchAggPortsFFRecord.
		 * OFSRCallback#getRequest()
		 */
		public OFStatisticsRequest getRequest() {
			OFStatisticsRequest req = new OFStatisticsRequest();
			OFPortStatisticsRequest psr = new OFPortStatisticsRequest();
			psr.setPortNumber(OFPort.OFPP_NONE.getValue());
			req.setStatisticType(OFStatisticsType.PORT);
			req.setStatistics(Collections.singletonList((OFStatistics) psr));
			req.setLengthU(req.getLengthU() + psr.getLength());
			return req;
		};

	}

	/**
	 * Interface to retrieve the OpenFlow request objects for the statistics
	 * request.
	 */
	private interface OFSRCallback {
		OFStatisticsRequest getRequest();
	}

	/**
	 * Thread that corresponds to each switch to record aggregate switch port
	 * stats in a flat file as plain text with a defined sample time.
	 * 
	 * @author festradasolano
	 * 
	 */
	private class ThreadSwitchAggPortsFFRecord extends Thread {

		/**
		 * Defines if thread is infinite.
		 */
		private boolean infinite = true;

		/**
		 * Sample time to record aggregate port stats.
		 */
		private int sampleTime = 10000;

		/**
		 * Switch DPID in numeric format to identify thread.
		 */
		private long switchDpid = 0;

		/**
		 * Creates a new thread to record aggregate port stats in flat files.
		 * 
		 * @param switchDpid
		 *            Switch DPID in numeric format
		 */
		public ThreadSwitchAggPortsFFRecord(long switchDpid) {
			this.infinite = true;
			this.sampleTime = 10000;
			this.switchDpid = switchDpid;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			// build and check flat file path
			String filePath = FilePath.getFilePath(DIRS_NAME, FILE_NAME + "_"
					+ this.switchDpid + ".log");
			PrintWriter writer = null;
			try {
				// create file writer in file
				writer = new PrintWriter(new FileWriter(filePath, true));
			} catch (IOException e) {
				logger.error("Error creating thread to record aggregate port stats from switch "
						+ HexString.toHexString(switchDpid));
				logger.error(e.getMessage(), e.getCause());
				return;
			}
			while (this.infinite) {
				// initialize stats
				long rxPackets = 0;
				long txPackets = 0;
				long rxBytes = 0;
				long txBytes = 0;
				long rxDrops = 0;
				long txDrops = 0;
				long rxError = 0;
				long txError = 0;
				long rxFrameError = 0;
				long rxOverrunError = 0;
				long rxCrcError = 0;
				long collisions = 0;
				// get stats
				Iterator<OFStatistics> iterator = getSwitchStats(
						new makePortStatsRequest(), switchDpid, "ports")
						.iterator();
				// aggregate ports stats
				while (iterator.hasNext()) {
					OFPortStatisticsReply portStats = (OFPortStatisticsReply) iterator
							.next();
					rxPackets += portStats.getreceivePackets();
					txPackets += portStats.getTransmitPackets();
					rxBytes += portStats.getReceiveBytes();
					txBytes += portStats.getTransmitBytes();
					rxDrops += portStats.getReceiveDropped();
					txDrops = +portStats.getTransmitDropped();
					rxError += portStats.getreceiveErrors();
					txError += portStats.getTransmitErrors();
					rxFrameError += portStats.getReceiveFrameErrors();
					rxOverrunError += portStats.getReceiveCRCErrors();
					collisions += portStats.getCollisions();
				}
				// write in file
				long time = System.currentTimeMillis();
				writer.println("time=" + time + "|rxPackets=" + rxPackets
						+ "|txPackets=" + txPackets + "|rxBytes=" + rxBytes
						+ "|txBytes=" + txBytes + "|rxDrops=" + rxDrops
						+ "|txDrops=" + txDrops + "|rxError=" + rxError
						+ "|txError=" + txError + "|rxFrameError="
						+ rxFrameError + "|rxOverrunError=" + rxOverrunError
						+ "|rxCrcError=" + rxCrcError + "|collisions="
						+ collisions + "|");
				writer.flush();
				logger.debug("Thread is recording aggregate port stats from switch "
						+ HexString.toHexString(this.switchDpid));
				// sleep sample time seconds
				try {
					Thread.sleep(this.sampleTime);
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e.getCause());
				}
			}
			writer.close();
		}

		/**
		 * Sets thread infinity.
		 * 
		 * @param infinite
		 *            Thread infinity
		 */
		public void setInfinite(boolean infinite) {
			this.infinite = infinite;
		}

	}

	/**
	 * Array of directories to reach flat file records.
	 */
	public static final String[] DIRS_NAME = { "of-controller-db", "floodlight" };

	/**
	 * Name of flat file record that stores switch aggregate port stats.
	 */
	public static final String FILE_NAME = "switchaggports";

	/**
	 * Controller logger.
	 */
	private static Logger logger;

	/**
	 * Service to interact with FloodLight.
	 */
	private IFloodlightProviderService floodlightProvider;

	/**
	 * Map to match switch DPIDs and flat line record threads.
	 */
	private Map<Long, ThreadSwitchAggPortsFFRecord> threadMap;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.IOFSwitchListener#addedSwitch(net.
	 * floodlightcontroller.core.IOFSwitch)
	 */
	@Override
	public void addedSwitch(IOFSwitch sw) {
		// add thread to new Openflow switch connection
		ThreadSwitchAggPortsFFRecord thread = new ThreadSwitchAggPortsFFRecord(
				sw.getId());
		threadMap.put(sw.getId(), thread);
		thread.start();
		logger.info("Added thread to record aggregate port stats from switch "
				+ HexString.toHexString(sw.getId()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.flatfilerecord.ISwitchFlatFileRecordService#
	 * getFlatFileRecordPath(long)
	 */
	@Override
	public String getFlatFileRecordPath(long switchDpid) {
		// return full path to switch flat file record
		return FilePath.getFilePath(DIRS_NAME, FILE_NAME + "_" + switchDpid
				+ ".log");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.module.IFloodlightModule#getModuleDependencies
	 * ()
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// get depencencies: floodlightprovider and restapi
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.module.IFloodlightModule#getModuleServices
	 * ()
	 */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// get services: fileflatrecord
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ISwitchAggPortsFFRecordService.class);
		return l;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.IOFSwitchListener#getName()
	 */
	@Override
	public String getName() {
		// return module name
		return SwitchAggPortsFFRecord.class.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.module.IFloodlightModule#getServiceImpls()
	 */
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// get services implementation: fileflatrecord
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(ISwitchAggPortsFFRecordService.class, this);
		return m;
	}

	/**
	 * Returns per switch and per type stats.
	 * 
	 * @param f
	 *            Callback function to receive requested stats
	 * @param switchDpid
	 *            Switch DPID to request in numeric format
	 * @param statsType
	 *            Identifies the type of stats requested
	 * @return Switch list of requested stats
	 */
	private List<OFStatistics> getSwitchStats(OFSRCallback f, long switchDpid,
			String statsType) {
		IOFSwitch sw = floodlightProvider.getSwitches().get(switchDpid);
		Future<List<OFStatistics>> future;
		List<OFStatistics> values = null;
		if (sw != null) {
			try {
				future = sw.getStatistics(f.getRequest());
				values = future.get(10, TimeUnit.SECONDS);
			} catch (Exception e) {
				logger.error("Failure retrieving " + statsType, e);
			}
		}
		return values;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#init(net.
	 * floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		// initialize parameters
		logger = LoggerFactory.getLogger(SwitchAggPortsFFRecord.class);
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		threadMap = new HashMap<Long, ThreadSwitchAggPortsFFRecord>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.IOFSwitchListener#removedSwitch(net.
	 * floodlightcontroller.core.IOFSwitch)
	 */
	@Override
	public void removedSwitch(IOFSwitch sw) {
		// remove thread from disconnected Openflow switch
		ThreadSwitchAggPortsFFRecord thread = threadMap.get(sw.getId());
		thread.setInfinite(false);
		threadMap.remove(sw.getId());
		logger.info("Removed thread to record aggregate port stats from switch "
				+ HexString.toHexString(sw.getId()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.floodlightcontroller.core.module.IFloodlightModule#startUp(net.
	 * floodlightcontroller.core.module.FloodlightModuleContext)
	 */
	@Override
	public void startUp(FloodlightModuleContext context) {
		// start listener and rest api
		floodlightProvider.addOFSwitchListener(this);
		// restApi.addRestletRoutable(new LoggingWebRoutable());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.floodlightcontroller.core.IOFSwitchListener#switchPortChanged(java
	 * .lang.Long)
	 */
	@Override
	public void switchPortChanged(Long switchId) {
	}

}
