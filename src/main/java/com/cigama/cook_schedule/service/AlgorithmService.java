package com.cigama.cook_schedule.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AlgorithmService {

    // --- Variables & Initialization ---

    public String findOptimalAssignment(List<String> userNames, List<String> strings, int seed) {
        int n = 7;
        int m = userNames.size();
        char[] output = new char[n];
        List<Integer> pNodes = new ArrayList<>();

        // --- Preprocessing ---
        for (int i = 0; i < n; i++) {
            boolean allOnes = true;
            for (int j = 0; j < m; j++) {
                if (strings.get(j).charAt(i) == '0') {
                    allOnes = false;
                    break;
                }
            }

            if (allOnes) {
                output[i] = '0';
            } else {
                pNodes.add(i);
            }
        }

        // --- Graph Construction ---
        FlowGraph graph = new FlowGraph();
        Node source = graph.addNode("Source");
        Node sink = graph.addNode("Sink");

        Node[] cNodes = new Node[m];
        for (int j = 0; j < m; j++) {
            cNodes[j] = graph.addNode("C_" + j);
            for (int k = 1; k <= pNodes.size(); k++) {
                graph.addEdge(cNodes[j], sink, 1, 2 * k - 1);
            }
        }

        for (int i : pNodes) {
            Node nodeP = graph.addNode("P_" + i);
            graph.addEdge(source, nodeP, 1, 0);

            for (int j = 0; j < m; j++) {
                if (strings.get(j).charAt(i) == '0') {
                    graph.addEdge(nodeP, cNodes[j], 1, 0);
                }
            }
        }

        // --- MCMF Execution ---
        graph.runMCMF(source, sink, seed);

        // --- Result Extraction ---
        for (Edge edge : graph.getAllEdges()) {
            if (edge.from.name.startsWith("P_") && edge.to.name.startsWith("C_")) {
                if (edge.flow == 1) {
                    int pos = Integer.parseInt(edge.from.name.substring(2));
                    int userIdx = Integer.parseInt(edge.to.name.substring(2));
                    output[pos] = (char) ('1' + userIdx);
                }
            }
        }

        return new String(output);
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

        void runMCMF(Node source, Node sink, int seed) {
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

                    // Add consecutive day avoidance
                    if (u.name.startsWith("P_")) {
                        int dayPos = Integer.parseInt(u.name.substring(2));
                        shuffledEdges.sort((e1, e2) -> {
                            int penalty1 = getConsecutivePenalty(e1, dayPos);
                            int penalty2 = getConsecutivePenalty(e2, dayPos);
                            return Integer.compare(penalty1, penalty2);
                        });
                    }

                    for (Edge edge : shuffledEdges) {
                        int currentCost = edge.cost;
                        if (u.name.startsWith("P_")) {
                            int dayPos = Integer.parseInt(u.name.substring(2));
                            if (getConsecutivePenalty(edge, dayPos) > 0) {
                                currentCost += 1000;
                            }
                        }

                        if (edge.capacity > edge.flow && distances.get(edge.to) > distances.get(u) + currentCost) {
                            distances.put(edge.to, distances.get(u) + currentCost);
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

                int pushFlow = Integer.MAX_VALUE;
                Node curr = sink;
                while (curr != source) {
                    Edge edge = parents.get(curr);
                    pushFlow = Math.min(pushFlow, edge.capacity - edge.flow);
                    curr = edge.from;
                }

                curr = sink;
                while (curr != source) {
                    Edge edge = parents.get(curr);
                    edge.flow += pushFlow;
                    edge.reverseEdge.flow -= pushFlow;
                    curr = edge.from;
                }
            }
        }

        private int getConsecutivePenalty(Edge edge, int dayPos) {
            if (!edge.to.name.startsWith("C_"))
                return 0;
            for (Edge neighborEdge : edge.to.edges) {
                if (neighborEdge.to.name.startsWith("P_")) {
                    int neighborDay = Integer.parseInt(neighborEdge.to.name.substring(2));
                    if (Math.abs(neighborDay - dayPos) == 1) {
                        if (neighborEdge.reverseEdge != null && neighborEdge.reverseEdge.flow == 1) {
                            return 1;
                        }
                    }
                }
            }
            return 0;
        }
    }
}
