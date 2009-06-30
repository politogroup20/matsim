package playground.yu.visum.filter;

import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

/**
 * transfer the "right" persons to next PersonFilter. These "right" persons
 * don't move on false links and nodes, which should not exist in network(file).
 * 
 * beurteilen, ob Personen auf nicht existierende Links oder durch nicht
 * existierende Nodes fahren oder andere Aktivitaeten durchfuehren. Die
 * richtigen Personen wurden hier zur NewPlansWriter übertragen.
 * 
 * @author yu chen
 */
public class PersonRouteFilter extends PersonFilterA {
	/**
	 * The underlying list of link-IDs of this PersonRouteFilter.
	 */
	private List<Id> criterionLinkIds;

	/**
	 * The underlying list of node-IDs of this PersonRouteFilter.
	 */
	private List<Id> criterionNodeIds;

	/**
	 * create a PersonFilter, which deletes Persons moving or staying on some
	 * links and nodes, which should not exist.
	 * 
	 * @param linkIds -
	 *            a list of link-IDs, which should not exist in network-file.
	 * @param nodeIds -
	 *            a list of node-IDs, which should not exist in network-file
	 * 
	 */
	public PersonRouteFilter(List<Id> linkIds, List<Id> nodeIds) {
		this.criterionLinkIds = linkIds;
		this.criterionNodeIds = nodeIds;
	}

	/**
	 * judge, whether the person will move or stay on some links and nodes,
	 * which should not exist in network(file).
	 */
	@Override
	public boolean judge(Person person) {
		List<Plan> plans = person.getPlans();
		for (Plan plan : plans) {
			if (plan.isSelected()) {
				List<? extends BasicPlanElement> acts_Legs = plan.getPlanElements();
				boolean even = false;
				for (BasicPlanElement obj : acts_Legs) {
					if (even) {
						LegImpl leg = (LegImpl) obj;
						NetworkRoute route = (NetworkRoute) leg.getRoute();
						if (route != null) {
							List<Link> links = route.getLinks();
							if (links != null)
								for (Link link : links) {
									if (this.criterionLinkIds.contains(link
											.getId()))
										return false;
								}
							List<Node> nodes = route.getNodes();
							if (nodes != null)
								for (Node node : nodes) {
									if (this.criterionNodeIds.contains(node
											.getId()))
										return false;
								}
						}
					} else {
						ActivityImpl act = (ActivityImpl) obj;
						if (this.criterionLinkIds.contains(act.getLink()
								.getId()))
							return false;
					}
					even = !even;
				}
			}
		}
		return true;
	}
}
