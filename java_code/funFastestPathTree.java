// Function in Main. Just action (returns nothing). Arguments: origin (GridPoint), time (double[]), prev (Link[])
// Dijkstra: fastest path tree from origin to every node, using each link's current varTravelTime.
// Fills the two arrays passed in (both of length parCols * parCols), indexed by node k = parXIndex * parCols + parYIndex. That index is built by taking all the points in the 2D grid, and laying their columns end-end.
//   time[k]: fastest travel time (minutes) from origin to node k; infinity if unreachable
//   prev[k]: last link on the fastest route into node k; null for the origin and unreachable nodes.
//            Follow prev from any node back to the origin to rebuild its whole route.
Arrays.fill(time, Double.POSITIVE_INFINITY);   // every node starts "unknown"
Arrays.fill(prev, null);
boolean[] settled = new boolean[time.length];   // true once a node's time is final

int o = origin.parXIndex * parCols + origin.parYIndex;
time[o] = 0;
PriorityQueue<double[]> queue = new PriorityQueue<>((x, y) -> Double.compare(x[0], y[0]));   // Each entry in this queue is itself an array: {time, node index}; poll() gives the smallest time
queue.add(new double[]{0, o}); //origin is the first point to be added to queue, and we know its time to itself is zero

while (!queue.isEmpty()) {
    int k = (int) queue.poll()[1];   // closest unsettled node. Poll also removes this entry so not present in next loop
    if (settled[k]) continue;   // old entry: this node was already settled via a faster time added later
    settled[k] = true;
    GridPoint n = varGrid[k / parCols][k % parCols]; //truncation and remainders translate index k back into x and y grid indices respectively. The X index would need +1 if we used base 1 indexing like in other programming lagnuages. 
    for (Link l : n.colLinks) {   // update each neighbour if going through n is faster
        GridPoint m = (l.parNodeA == n) ? l.parNodeB : l.parNodeA; //node at other end of link l
        int km = m.parXIndex * parCols + m.parYIndex; //the above node's index translated back into stacked column format
        double t = time[k] + l.varTravelTime;
        if (t < time[km]) { //if adding this link creates a new route shorter than the previously recorded one, then the recorded one gets updated to this
            time[km] = t;
            prev[km] = l; //this node now knows which link it must follow to go on the shortest path to origin.
            queue.add(new double[]{t, km});   // Add this neighbour to the queue of whom to settle. If the neighbour was already on the list with a slower time, then that entry remains; first the faster entry will be run through and settled. Then eventually one by one, each slower entry counterpart will be looked at, removed from the queue, and then immediately end its iteration when the second line in the iteration finds it was already settled.
        }
    }
}