import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Object;
import java.math.BigDecimal;

// RoutingPerformance.java
// COMP3331 Assignment S2 2013
// By Daniel Cai and Daniel Musso

public class RoutingPerformance {

  //public int numVCRequests = 0;

  // Maximum number of nodes   
  static final int MAX_NODES = 26;
  
  // Max propagation delay 
  static final int MAX_DELAY = 200;
  static final int MAX_CAP   = 100;
  public static String routingProtocol;

  // Map of edge lists for each node
  public static Map<Character, List<Edge>> graph;

  public static List<Edge> edges;
  public static Map<Pair, Edge> edgeMap;
  public static List<Character> nodes;

  public static Set<Character> usedNodes;
  public static Set<Character> unusedNodes;

  public static Map<Character, Integer> distances;

  // First Character is child, second is parent
  // e.g. path.get(D) returns B, (ie quickest to get to D from B
  public static Map<Character, Character> path;

  public static int numVCRequests = 0;
  public static int numSuccessRequests = 0;
  public static float percentageSuccessRequests = 0;
  public static int numBlockedRequests = 0;
  public static float percentageBlockedRequests = 0;
  
  public static List<Integer> averageHops;
  public static List<Integer> averageCost;

  public static void main(String[] args) {

    //int numVCRequests = 0;
    //Graph g = new Graph(args[1]);
    readGraph(args[1]);  

    // printGraph
    for (char ch = 'A'; ch <= 'Z'; ch++) {
      if (graph.containsKey(ch)) {
        System.out.print(ch + ": " );
        for (int i = 0; i < graph.get(ch).size(); i++) {
          //g.map.get(ch).get(i).printEdge();
          graph.get(ch).get(i).printEdge();
        } 
        System.out.println();
      }
    }

    averageHops = new ArrayList<Integer>();
    averageCost = new ArrayList<Integer>();

    if (args[0].equals("SHP")) {
      System.out.println("SHP");
      routingProtocol = "SHP";
      SHP(args[2]);
    } else if (args[0].equals("SDP")) {
      System.out.println("SDP");
      routingProtocol = "SDP";
      SHP(args[2]);
    } else if (args[0].equals("LLP")){
      System.out.println("LLP");
      routingProtocol = "LLP";
      SHP(args[2]);
    }

    percentageSuccessRequests = ((float)numSuccessRequests/numVCRequests * 100);
    percentageBlockedRequests = ((float)numBlockedRequests/numVCRequests * 100);

    System.out.println("total number of virtual circuit requests: " + numVCRequests);
    //numSuccessRequests = numVCRequests;
    System.out.println("number of successfully routed requests: " + numSuccessRequests);
    //System.out.println("percentage of successfully routed request: " + (int)percentageSuccessRequests);
    System.out.printf("percentage of successfully routed request: %.2f\n", percentageSuccessRequests);
    System.out.println("number of blocked requests: " + numBlockedRequests);
    //System.out.println("percentage of blocked requests: " + (float)percentageBlockedRequests);
    System.out.printf("percentage of blocked requests: %.2f\n", percentageBlockedRequests);

    float average = 0;
    if (!averageHops.isEmpty()) {
      for (Integer val : averageHops)
        average += val;
    }
    //System.out.println("average number of hops per circuit: " + (float)(average/averageHops.size()));
    System.out.printf("average number of hops per circuit: %.2f\n", (average/averageHops.size()));
    average = 0;
    if (!averageCost.isEmpty()) {
      for (Integer val : averageCost)
        average += val;
    }
    //System.out.println("average cumulative propagation delay per circuit: " + (float)(average/averageCost.size()));
    System.out.printf("average cumulative propagation delay per circuit: %.2f\n", (average/averageCost.size()));
  }

  public static void readGraph (String file) {
    graph = new HashMap<Character, List<Edge>>();
    edges = new ArrayList<Edge>();
    nodes = new ArrayList<Character>();
    edgeMap = new HashMap<Pair, Edge>();

/*
    for (char ch = 'A'; ch <= 'Z'; ++ch) {
      graph.put(ch, new LinkedList<Edge>());
      ///Node n = new Node(ch);
      nodes.add(ch);
    }
*/
    try {
      // read in Topology
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;

      System.out.println("Printing file");
      while ((line = br.readLine()) != null) {
        String[] specs = line.split(" ");
        //System.out.println("specs[0]: " + specs[0]);
        char node = specs[0].charAt(0);
        char adjNode = specs[1].charAt(0);
        int delay = Integer.parseInt(specs[2]);
        int capacity = Integer.parseInt(specs[3]);
        Edge e1 = new Edge(node, adjNode, delay, capacity);
        Edge e2 = new Edge(adjNode, node, delay, capacity);
        edges.add(e1);
        edges.add(e2);
        Pair p1 = new Pair(node,adjNode);
        Pair p2 = new Pair(adjNode,node);
        edgeMap.put(p1,e1);
        edgeMap.put(p2,e2);  
        Pair p3 = new Pair (node,adjNode);
/*
        if (edgeMap.containsKey(p3)) {
          System.out.println("CONTAINS P3");
        }
*/
        // put nodes in graph and nodes list
        if (!graph.containsKey(node)) {
          graph.put(node, new LinkedList<Edge>());
          nodes.add(node);

        }
        if (!graph.containsKey(adjNode)) {
          graph.put(adjNode, new LinkedList<Edge>());
          nodes.add(adjNode);
        }


        graph.get(node).add(e1); 
        graph.get(adjNode).add(e2); // dont copy both directions to graph? 
        //System.out.println(line);
        //numVCRequests++;
        
      }
      br.close();
    }
    catch (Exception e) {
      System.err.println(e.getMessage()); // handle exception
    }

  }

  //TODO: make edgepath as return for Djkstra()
  public static List<Pair> finalEdgePath;

  public static void updateWorkLoad(int expired, int duration){
    Pair p;
    Edge edge = null;
    boolean blocked = false;         
    ListIterator it = finalEdgePath.listIterator();  
    while (it.hasNext()){
      p = (Pair)it.next();
      edge = edgeMap.get(p);
      if (edge.isBlocked()){
        blocked = true;
        System.out.println("Blocked at " + p.first + " " + p.second);
        break;      
      }
    }
    int totalDelay = 0;
    if (!blocked) {
      it = finalEdgePath.listIterator();
      averageHops.add(finalEdgePath.size());
      while (it.hasNext()){
        p = (Pair)it.next();
        edge = edgeMap.get(p);
        edge.request(duration);
        totalDelay += edge.delay;
      }
      numSuccessRequests++;
      averageCost.add(totalDelay);
    } else {
      numBlockedRequests++;
    }
    numVCRequests++;
  }

  public static void SHP (String file) {
    finalEdgePath = new ArrayList<Pair>();
    int currtime = 0;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      //int numVCRequests = 0;
      System.out.println("Printing workload");
      while ((line = br.readLine()) != null) {
        String[] specs = line.split(" ");
        //System.out.println("specs[0]: " + specs[0]);
        char srcNode = specs[1].charAt(0);
        char destNode = specs[2].charAt(0);
        

        //update every node's workload
        BigDecimal a = new BigDecimal(specs[0]).multiply(new BigDecimal("1000000."));
        BigDecimal b = new BigDecimal(specs[3]).multiply(new BigDecimal("1000000."));
        int expired = a.intValue() - currtime;
        int duration = b.intValue();
        currtime = a.intValue();
        ListIterator it = edges.listIterator();  
        Edge e;
        while (it.hasNext()){
          e = (Edge)it.next();
          e.update(expired);
        }

        finalEdgePath.clear();
        Dijkstra(srcNode, destNode);
        //traverse edges to add to workload
        updateWorkLoad(expired, duration);
        System.out.println("Finished request for " + srcNode + " " + destNode + " " + a.intValue() + " " + b.intValue()+ "\n");
      }
      br.close();
      //System.out.println("total number of virtual circuit requests: " + numVCRequests);
    }
    catch (Exception e) {
      System.err.println(e.getMessage()); // handle exception
    }
  }

  public static void Dijkstra (char srcNode, char destNode) {
    System.out.println("\nDijkstra");
    System.out.println("Source: " + srcNode + " Destination: " + destNode);

    usedNodes = new HashSet<Character>();
    unusedNodes = new HashSet<Character>();
    List<Character> finalPath = new ArrayList<Character>();
    boolean success = false;

    unusedNodes.add(srcNode);
    distances = new HashMap<Character, Integer>();
    path = new HashMap<Character, Character>();
    initialiseDistances();
    distances.put(srcNode, 0);

    while (unusedNodes.size() > 0) {
      char node = getClosestNode(unusedNodes);
      
      usedNodes.add(node);
      unusedNodes.remove(node);
      if (node == destNode) {
        ///////////////////////////////////////////////////
        //numSuccessRequests++;
        success = true;
        break;
      }

      // now add adjacent nodes to unusedNodes
      checkAdjacentNodes(node);
      //findMinimalDistances(node);
    }
    System.out.println("distance from " + srcNode + " to " + destNode + " : " + distances.get(destNode));
    char n = destNode;
    System.out.println("Path from " + srcNode + " to " + destNode);
    while (path.get(n) != null) {
      finalPath.add(n);
      //--->System.out.print(n + "<-");
      n = path.get(n);
    }

   //---> System.out.println(srcNode);
    finalPath.add(srcNode);

   //---> System.out.println("FINAL PATH");
    Collections.reverse(finalPath);
    char src,dest;
    for (int i = 0; i < finalPath.size()-1; i++) {
      System.out.print(finalPath.get(i) + "->");
      src = finalPath.get(i);
      dest = finalPath.get(i+1);
      finalEdgePath.add((new Pair(src,dest)));
    }
    System.out.println(finalPath.get(finalPath.size()-1));

    usedNodes = null;
    unusedNodes = null;
    distances = null;
    path = null;
  }

  public static void initialiseDistances() {
    for (char ch = 'A'; ch <= 'Z'; ch++) {
      if (routingProtocol.equals("SHP")) {
        distances.put(ch, MAX_NODES+1);
      }
      else if (routingProtocol.equals("SDP")) {
        distances.put(ch, MAX_DELAY+1);
      } else if (routingProtocol.equals("LLP")) {
        distances.put(ch, MAX_CAP+1);
      }
    }
  }

  public static char getClosestNode(Set<Character> unusedNodes) {

    char closest = '\0';
    for (char ch : unusedNodes) {
      if (closest == '\0') {
        closest = ch;
      }
      else {
        //if (getShortestDistance(ch) < getShortestDistance(closest)) {
          if (distances.get(ch) < distances.get(closest)) {
          closest = ch;
        }
      }
    }

    return closest;
  }
/*
  public static int getShortestDistance(char nextNode) {
    Integer distance = distances.get(nextNode);

    if (distance == null) {
      distance = Integer.MAX_VALUE;
    }
    
    return distance;

  }
*/
  public static void checkAdjacentNodes(char srcNode) {
    //--->System.out.println("checkAdjacentNodes");
/*
    for (Edge e : edges) {
      if (e.srcNode == srcNode && !usedNodes.contains(e.destNode)) { // then its adjacent
        
        int newdistance = 1 + distances.get(srcNode);
        
        //if (newdistance < getShortestDistance(e.destNode)) {
        if (newdistance < distances.get(e.destNode)) {
          distances.put(e.destNode, newdistance);
          unusedNodes.add(e.destNode);
          path.put(e.destNode, srcNode); // ie you get to e.destNode from srcNode
        }
        
  
        System.out.println(srcNode + "->" + e.destNode);
        System.out.println("distance to : " + e.destNode + " from " + srcNode + " : " + distances.get(e.destNode));
      }
    }
*/
    for (char ch = 'A'; ch <= 'Z'; ch++) {
      Pair p = new Pair(srcNode,ch);
      if (edgeMap.containsKey(p) && !usedNodes.contains(ch)) {
        int newdistance = 0;
        if (routingProtocol.equals("SHP")) {
          newdistance = 1 + distances.get(srcNode);
        }
        else if (routingProtocol.equals("SDP")) {
          int linkCost = edgeMap.get(p).delay;
          
          newdistance = linkCost + distances.get(srcNode);
        } else if (routingProtocol.equals("LLP")){
          int workload = (int)((edgeMap.get(p).load * 100.) / edgeMap.get(p).capacity);
          newdistance = workload;
          //System.out.println("new distance: " + workload);
        }
        
        
        //if (newdistance < getShortestDistance(e.destNode)) {
        if (newdistance < distances.get(ch)) {
          distances.put(ch, newdistance);
          unusedNodes.add(ch);
          path.put(ch, srcNode); // ie you get to e.destNode from srcNode
        }
        
        //--->System.out.println(srcNode + "->" + ch);
        //System.out.println("distance to : " + ch + " from " + srcNode + " : " + distances.get(ch));
      }
    }

  }
}

class Edge {
  public char srcNode;
  public char destNode;
  public final int delay;
  public final int capacity;
  public int load = 0;
  private List<Integer> workload;
  
  public Edge (char src, char dest, int d, int c) {
    this.srcNode = src;
    this.destNode = dest;
    this.delay = d;
    this.capacity = c;
    workload = new LinkedList<Integer>();
  }

  public void printEdge () {
    System.out.print(destNode + " " + delay + " " + capacity + " ");
  }

  public boolean isBlocked(){
    //--->System.out.print("isBlocked load: " + load + " capacity: " +capacity + "\n" );
    return load >= capacity;  
  }

  @SuppressWarnings(value = "unchecked")
  public void update(int expired){
    ListIterator it = workload.listIterator();
    while (it.hasNext()){
      int curr = (Integer)it.next();
      //-->>System.out.print("Updating curr for " + srcNode + " " + destNode + " " + curr + " - " + expired + " = " + (curr-expired) + "\n");
      curr -= expired;
      if (curr <= 0){
        it.remove();
        load--;
        //-->>System.out.print("Released finished load " + srcNode + "->" + destNode +"\n");
      } else {
        it.set(curr);
        
      }
    }
  }

  public void request(int duration){
    load++;
    workload.add(duration);
    System.out.print("load++ for edge " + srcNode + "->" + destNode + " now " + load + "/" + capacity+ "\n" );
  }
}
class Pair {
  public final char first;
  public final char second;

  public Pair(char f, char s) {
    this.first = f;
    this.second = s;
  }

  @Override
  public boolean equals(Object other) {
    Pair p = (Pair) other;
    return this.first == p.first && this.second == p.second;
  }


  @Override public int hashCode() {
    int hash = 9;
    hash = hash * 23 + first + second;
    return hash;
  }
}


/*
class Node {
  public char name;

  public Node (char n) {
    this.name = n;
  }
}
*/
/*
    public void addNeighbour(char v1, char v2, int delay, int capacity) {

      string str = v2 + delay + capacity
      map.get(v1).add(neighbour);
    }
*/
/*
    public List<Character> getNeighbours(int v) {
      return adj.get(v);
    }
*/
