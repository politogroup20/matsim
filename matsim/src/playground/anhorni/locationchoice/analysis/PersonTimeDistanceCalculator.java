package playground.anhorni.locationchoice.analysis;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.population.LegImpl;

/*
 * This is a helper class for TravelTimeandDistanceStats
 * TODO: move this functionality to Person.java
 */

public class PersonTimeDistanceCalculator {

	private static double planTravelTime;
	private static double planTravelDistance;
	private static int numberOfLegs;


	private static void init() {
		planTravelTime=0.0;
		planTravelDistance=0.0;
		numberOfLegs=0;
	}

	public static void run(final Person person){

		init();

		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof LegImpl) {
				final LegImpl leg = (LegImpl) pe;
				planTravelTime+=leg.getTravelTime();
				planTravelDistance+=leg.getRoute().getDistance();
				numberOfLegs++;
			}
		}
	}

	public static double getPlanTravelTime() {
		return planTravelTime;
	}

	public static double getPlanTravelDistance() {
		return planTravelDistance;
	}

	public static int getNumberOfLegs() {
		return numberOfLegs;
	}
}
