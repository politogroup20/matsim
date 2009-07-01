package playground.andreas.bln;

import java.io.File;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;

/**
 * Combines DuplicatePlans and ShuffleCoords to<br>
 *  - first - expand a given plan by a certain number of clones<br>
 *  - second - alternate the coords of the clones, so that new coord is in a perimeter 
 *  with radius specified and new coords are equally distributed within that perimeter. 
 * 
 * @author aneumann
 *
 */
public class PlanExpander {
	
	public static void main(String[] args) {
		
		String networkFile = "./bb_cl.xml.gz";
		String plansFile = "./plans3";
		int numberOfAdditionalCopies = 4;
		double radiusOfPerimeter = 1000.0;
		
		Gbl.startMeasurement();
		
		BasicScenarioImpl sc = new BasicScenarioImpl();
		Gbl.setConfig(sc.getConfig());
		
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);

		Population inPop = new PopulationImpl();
		PopulationReader popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile(plansFile + ".xml.gz");

		DuplicatePlans dp = new DuplicatePlans(inPop, "tmp.xml.gz", numberOfAdditionalCopies);
		dp.run(inPop);
		dp.writeEndPlans();
		
		System.out.println("Dublicating plans finished");
		Gbl.printElapsedTime();
		
		inPop = new PopulationImpl();
		popReader = new MatsimPopulationReader(inPop, net);
		popReader.readFile("tmp.xml.gz");

		ShuffleCoords shuffleCoords = new ShuffleCoords(inPop, plansFile + "_" + (numberOfAdditionalCopies + 1) + "x.xml.gz", radiusOfPerimeter);
		shuffleCoords.run(inPop);
		shuffleCoords.writeEndPlans();
		
		(new File("tmp.xml.gz")).deleteOnExit();

		Gbl.printElapsedTime();

	}

}
