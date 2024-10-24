package mets_r.routing;

import edu.uci.ics.jung.graph.Graph;
import repast.simphony.space.graph.RepastEdge;

/**
 * A Converter class that converts a Jung graph to that used in the Jgraph library.
 * 
 * @author Samiul Hasan, Zengxiang Lei
 **/

public class NodeToJgraph<T> {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SimWeightedGraph<T, RepastEdge<T>> convertToJgraph(Graph<T, RepastEdge<T>> jungGraph) {
		SimWeightedGraph<T, RepastEdge<T>> jGraph = new SimWeightedGraph<T, RepastEdge<T>>(
				(Class<? extends RepastEdge<T>>) RepastEdge.class);

		for (T vertex : jungGraph.getVertices()) {
			jGraph.addVertex(vertex);
		}

		for (RepastEdge<T> edge : jungGraph.getEdges()) {
			double weight = 1.0;
			if (edge instanceof RepastEdge) {
				weight = ((RepastEdge) edge).getWeight();
			}
			T source = jungGraph.getSource(edge);
			T target = jungGraph.getDest(edge);
			jGraph.addEdge(source, target, edge);
			jGraph.setEdgeWeight(edge, weight);
		}
		return jGraph;
	}
}
