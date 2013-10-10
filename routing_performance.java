import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Object;

//http://www.vogella.com/articles/JavaAlgorithmsDijkstra/article.html

public class routing_performance {

  //public int numVCRequests = 0;

  // Maximum number of nodes   
  static final int MAX_NODES = 26;
  
  // Max propagation delay 
  static final int MAX_DELAY = 200;
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

    if (args[0].equals("SHP")) {
      System.out.println("SHP");
      routingProtocol = "SHP";
      SHP(args[2]);
    }
    else if (args[0].equals("SDP")) {
      System.out.println("SDP");
      routingProtocol = "SDP";
      SHP(args[2]);
    }

    percentageSuccessRequests = ((float)numSuccessRequests/numVCRequests * 100);
    percentageBlockedRequests = ((float)numBlockedRequests/numVCRequests * 100);
    System.out.println("total number of virtual circuit requests: " + numVCRequests);
    //numSuccessRequests = numVCRequests;
    System.out.println("number of successfully routed requests: " + numSuccessRequests);
    System.out.println("percentage of successfully routed request: " + (int)percentageSuccessRequests);
    System.out.println("number of blocked requests: " + numBlockedRequests);
    System.out.println("percentage of blocked requests: " + (int)percentageBlockedRequests);



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

  public static void SHP (String file) {

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
        Dijkstra(srcNode, destNode);
      

        System.out.println(line);
        numVCRequests++;
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
        numSuccessRequests++;
        success = true;
        break;
      }

      // now add adjacent nodes to unusedNodes
      checkAdjacentNodes(node);
      //findMinimalDistances(node);
    }
    if (!success) {
      numBlockedRequests++;
    }
    System.out.println("distance from " + srcNode + " to " + destNode + " : " + distances.get(destNode));
    char n = destNode;
    System.out.println("Path from " + srcNode + " to " + destNode);
    while (path.get(n) != null) {
      finalPath.add(n);
      System.out.print(n + "<-");
      n = path.get(n);
    }

    System.out.println(srcNode);
    finalPath.add(srcNode);

    System.out.println("FINAL PATH");
    Collections.reverse(finalPath);
    for (int i = 0; i < finalPath.size()-1; i++) {
      System.out.print(finalPath.get(i) + "->");
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
        distances.put(ch, MAX_NODES);
      }
      else if (routingProtocol.equals("SDP")) {
        distances.put(ch, MAX_DELAY);
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
    System.out.println("checkAdjacentNodes");
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
        }
        
        
        //if (newdistance < getShortestDistance(e.destNode)) {
        if (newdistance < distances.get(ch)) {
          distances.put(ch, newdistance);
          unusedNodes.add(ch);
          path.put(ch, srcNode); // ie you get to e.destNode from srcNode
        }
        
        System.out.println(srcNode + "->" + ch);
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

  public Edge (char src, char dest, int d, int c) {
    this.srcNode = src;
    this.destNode = dest;
    this.delay = d;
    this.capacity = c;
  }

  public void printEdge () {
    System.out.print(destNode + " " + delay + " " + capacity + " ");
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
