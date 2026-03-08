package com.cigama.cook_schedule.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AlgorithmService {

    // --- Unified Graph Algorithm ---

    public Map<String, String> generateFullRoster(List<String> userNames, List<List<String>> allTasks, int seed) {
        int numDays = 7;
        int numTasks = 5;
        int m = userNames.size();

        char[][] outputs = new char[numTasks][numDays];
        for (int t = 0; t < numTasks; t++) {
            Arrays.fill(outputs[t], '0');
        }

        // --- Graph Construction ---
        FlowGraph graph = new FlowGraph();
        Node source = graph.addNode("Source");
        Node sink = graph.addNode("Sink");

        Node[] uNodes = new Node[m];
        for (int j = 0; j < m; j++) {
            uNodes[j] = graph.addNode("U_" + j);
            for (int k = 1; k <= 35; k++) {
                graph.addEdge(uNodes[j], sink, 1, 2 * k - 1);
            }
        }

        Node[][] tNodes = new Node[numDays][numTasks];
        for (int d = 0; d < numDays; d++) {
            for (int t = 0; t < numTasks; t++) {
                boolean allOnes = true;
                for (int j = 0; j < m; j++) {
                    if (allTasks.get(t).get(j).charAt(d) == '0') {
                        allOnes = false;
                        break;
                    }
                }

                if (!allOnes) {
                    tNodes[d][t] = graph.addNode("T_" + d + "_" + t);
                    graph.addEdge(source, tNodes[d][t], 1, 0);

                    for (int j = 0; j < m; j++) {
                        if (allTasks.get(t).get(j).charAt(d) == '0') {
                            graph.addEdge(tNodes[d][t], uNodes[j], 1, 0);
                        }
                    }
                }
            }
        }

        // --- MCMF Execution with Dynamic Penalty ---
        graph.runMCMFWithPenalty(source, sink, seed);

        // --- Result Extraction ---
        for (Edge edge : graph.getAllEdges()) {
            if (edge.from.name.startsWith("T_") && edge.to.name.startsWith("U_")) {
                if (edge.flow == 1) {
                    String[] parts = edge.from.name.split("_");
                    int d = Integer.parseInt(parts[1]);
                    int t = Integer.parseInt(parts[2]);
                    int userIdx = Integer.parseInt(edge.to.name.substring(2));
                    outputs[t][d] = (char) ('1' + userIdx);
                }
            }
        }

        Map<String, String> results = new HashMap<>();
        results.put("resMarket", new String(outputs[0]));
        results.put("resCookNoon", new String(outputs[1]));
        results.put("resWashNoon", new String(outputs[2]));
        results.put("resCookNight", new String(outputs[3]));
        results.put("resWashNight", new String(outputs[4]));

        return results;
    }

    // --- Complex Methods ---

    private static class Node {
        String name;
        List<Edge> edges = new ArrayList<>();

        Node(String name) {
            this.name = name;
        }
    }

    private static class Edge {
        Node from;
        Node to;
        int capacity;
        int flow;
        int cost;
        Edge reverseEdge;

        Edge(Node from, Node to, int capacity, int cost) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
            this.cost = cost;
            this.flow = 0;
        }
    }

    private static class FlowGraph {
        List<Node> nodes = new ArrayList<>();

        Node addNode(String name) {
            Node node = new Node(name);
            nodes.add(node);
            return node;
        }

        Node getNodeByName(String name) {
            for (Node n : nodes) {
                if (n.name.equals(name))
                    return n;
            }
            return null;
        }

        void addEdge(Node from, Node to, int capacity, int cost) {
            Edge forward = new Edge(from, to, capacity, cost);
            Edge backward = new Edge(to, from, 0, -cost);
            forward.reverseEdge = backward;
            backward.reverseEdge = forward;
            from.edges.add(forward);
            to.edges.add(backward);
        }

        List<Edge> getAllEdges() {
            List<Edge> allEdges = new ArrayList<>();
            for (Node node : nodes) {
                allEdges.addAll(node.edges);
            }
            return allEdges;
        }

        void runMCMFWithPenalty(Node source, Node sink, int seed) {
            Random random = new Random(seed);
            while (true) {
                Map<Node, Integer> distances = new HashMap<>();
                Map<Node, Edge> parents = new HashMap<>();
                for (Node node : nodes)
                    distances.put(node, Integer.MAX_VALUE);
                distances.put(source, 0);

                Queue<Node> queue = new LinkedList<>();
                Set<Node> inQueue = new HashSet<>();
                queue.add(source);
                inQueue.add(source);

                while (!queue.isEmpty()) {
                    Node u = queue.poll();
                    inQueue.remove(u);

                    List<Edge> shuffledEdges = new ArrayList<>(u.edges);
                    Collections.shuffle(shuffledEdges, random);

                    for (Edge edge : shuffledEdges) {
                        if (edge.capacity > edge.flow && distances.get(edge.to) > distances.get(u) + edge.cost) {
                            distances.put(edge.to, distances.get(u) + edge.cost);
                            parents.put(edge.to, edge);
                            if (!inQueue.contains(edge.to)) {
                                queue.add(edge.to);
                                inQueue.add(edge.to);
                            }
                        }
                    }
                }

                if (distances.get(sink) == Integer.MAX_VALUE)
                    break;

                int pushFlow = 1;
                Node taskNode = null;
                Node userNode = null;
                boolean isAssignment = true;

                Node curr = sink;
                while (curr != source) {
                    Edge edge = parents.get(curr);
                    edge.flow += pushFlow;
                    edge.reverseEdge.flow -= pushFlow;

                    if (edge.from.name.startsWith("T_") && edge.to.name.startsWith("U_")) {
                        taskNode = edge.from;
                        userNode = edge.to;
                        isAssignment = true;
                    } else if (edge.from.name.startsWith("U_") && edge.to.name.startsWith("T_")) {
                        userNode = edge.from;
                        taskNode = edge.to;
                        isAssignment = false;
                    }

                    curr = edge.from;
                }

                if (taskNode != null && userNode != null) {
                    String[] parts = taskNode.name.split("_");
                    int d = Integer.parseInt(parts[1]);
                    int t = Integer.parseInt(parts[2]);

                    int penalty = isAssignment ? 1000 : -1000;

                    Node prevTask = getNodeByName("T_" + d + "_" + (t - 1));
                    Node nextTask = getNodeByName("T_" + d + "_" + (t + 1));

                    if (prevTask != null) {
                        for (Edge e : prevTask.edges) {
                            if (e.to == userNode && e.capacity > 0) {
                                e.cost += penalty;
                                e.reverseEdge.cost -= penalty;
                            }
                        }
                    }
                    if (nextTask != null) {
                        for (Edge e : nextTask.edges) {
                            if (e.to == userNode && e.capacity > 0) {
                                e.cost += penalty;
                                e.reverseEdge.cost -= penalty;
                            }
                        }
                    }
                }
            }
        }
    }
}
