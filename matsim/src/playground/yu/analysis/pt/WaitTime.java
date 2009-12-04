/**
 * 
 */
package playground.yu.analysis.pt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.misc.Time;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class WaitTime implements PersonEntersVehicleEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler {
	/** saves the first departure times of every agent/person */
	private Map<Id, Double> firstDepartures = new HashMap<Id, Double>(),
	/** saves the newest departure times of every agent/person */
	departures = new HashMap<Id, Double>(),
	/** saves the newest arrival times of every agent/person */
	arrivals = new HashMap<Id, Double>();

	/**
	 * saves the results to output, format: personId, [departureTime,
	 * enterVehTime][departureTime, enterVehTime]...
	 */
	private Map<Id, List<Double>> results1 = new HashMap<Id, List<Double>>(),
	/** saves the waitTimes of agents/persons */
	waitTimes = new HashMap<Id, List<Double>>();

	// public WaitTime() {}

	public void handleEvent(PersonEntersVehicleEvent event) {
		Id perId = event.getPersonId();
		double time/* [s] */= event.getTime();

		List<Double> resTimes = results1.get(perId);
		if (resTimes == null)
			resTimes = new ArrayList<Double>();

		try {
			resTimes.add(departures.get(perId)/* departureTime */);
			resTimes.add(time/* enterVehTime */);
		} catch (NullPointerException npe) {
			System.err
					.println("WARN:\tthere is not the departure time record of agent:\t"
							+ perId);
		}

		results1.put(perId, resTimes);
	}

	public void reset(int iteration) {
	}

	public void handleEvent(AgentDepartureEvent event) {
		Id perId = event.getPersonId();
		double time/* [s] */= event.getTime();

		if (!firstDepartures.containsKey(perId))
			firstDepartures.put(perId, time);

		departures.put(perId, time);
	}

	public void handleEvent(AgentArrivalEvent event) {
		arrivals
				.put(event.getPersonId()/* personId */, event.getTime()/* arrivalTime */);
	}

	public void write(String outputFilenameBase) {
		SimpleWriter writer = new SimpleWriter(outputFilenameBase + "txt");
		/* writes the first Result */
		writer
				.writeln("personId\t[departureTime, enterVehTime]\t[departureTime, enterVehTime]");
		XYScatterChart chartB = new XYScatterChart(
				"arrivalTimeAtBusStop<->enterVehTime", "arrivalTimeAtBusStop",
				"enterVehTime");
		int arraySizeB = results1.size();
		int seriesBsize = results1.values().iterator().next().size() / 2;
		List<double[]> xBss = new ArrayList<double[]>(), yBss = new ArrayList<double[]>();
		for (int i = 0; i < seriesBsize; i++) {
			double[] xBs = new double[arraySizeB], yBs = new double[arraySizeB];
			xBss.add(xBs);
			yBss.add(yBs);
		}
		int k = 0;
		for (Entry<Id, List<Double>> result : results1.entrySet()) {
			Id perId = result.getKey();
			writer.write(perId + "\t");
			List<Double> times = result.getValue();
			for (int i = 0; i < times.size(); i += 2) {
				double departureTime = times.get(i), enterVehTime = times
						.get(i + 1);
				xBss.get(i / 2)[k] = departureTime / 3600.0;
				yBss.get(i / 2)[k] = enterVehTime / 3600.0;
				writer.write("[" + Time.writeTime(departureTime) + ", "
						+ Time.writeTime(enterVehTime) + "]\t");
				List<Double> waitTimeList = waitTimes.get(perId);
				if (waitTimeList == null)
					waitTimeList = new ArrayList<Double>();
				waitTimeList.add(enterVehTime - departureTime/* waitTime */);
				waitTimes.put(perId, waitTimeList);
			}
			writer.writeln();
			k++;
		}
		for (int i = 0; i < seriesBsize; i++)
			chartB.addSeries((i + 1) + ". enterVehTime", xBss.get(i), yBss
					.get(i));
		chartB.saveAsPng(outputFilenameBase + "B.png", 1024, 768);
		writer
				.writeln("-----------------\npersonId\tenRouteTime\tenRouteWaitTime\twaitTimeFraction\t[waitTime,waitTime...]");
		// TODO to write result2 in graphic
		XYScatterChart chartA = new XYScatterChart(
				"enRouteTime<->waitTimeAtBusStop", "enRouteTime [s]",
				"waitTimeAtBusStop [s]");

		int arraySizeA = arrivals.size();
		double[] xAs = new double[arraySizeA];
		int waitTimesSize = waitTimes.values().iterator().next().size();
		List<double[]> yAss = new ArrayList<double[]>();
		for (int i = 0; i < waitTimesSize; i++) {
			double[] yAs = new double[arraySizeA];
			yAss.add(yAs);
		}

		int j = 0;
		for (Entry<Id, List<Double>> waitTimeEntry : waitTimes.entrySet()) {

			Id perId = waitTimeEntry.getKey();
			List<Double> waitTimeList = waitTimeEntry.getValue();

			double enRouteTime = arrivals.get(perId)
					- firstDepartures.get(perId);
			xAs[j] = enRouteTime;

			writer.write(perId + "\t" + Time.writeTime(enRouteTime) + "\t");
			StringBuffer sb = new StringBuffer("[");
			double waitTimeSum = 0;
			int i = 0;
			for (double d : waitTimeList) {
				sb.append(Time.writeTime(d) + "][");
				waitTimeSum += d;
				yAss.get(i)[j] = d;
				i++;
			}
			writer.writeln(Time.writeTime(waitTimeSum) + "\t" + waitTimeSum
					/ enRouteTime + "\t" + sb);
			j++;
		}
		writer.close();
		for (int i = 0; i < waitTimesSize; i++)
			chartA.addSeries((i + 1) + ". waitTimeAtBusStop", xAs, yAss.get(i));

		chartA.saveAsPng(outputFilenameBase + "A.png", 1024, 768);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/nullfall_M44_344/test/output200/ITERS/it.100/100.events.xml.gz";

		EventsManager em = new EventsManagerImpl();
		WaitTime wt = new WaitTime();
		em.addHandler(wt);

		new MatsimEventsReader(em).readFile(eventsFilename);

		wt
				.write("../berlin-bvg09/pt/nullfall_M44_344/test/output200/ITERS/it.100/100.waitTimes.");
	}
}
