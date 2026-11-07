// Function in Main. Just action (returns nothing). Arguments: origin (GridPoint), time (double[]), prev (Link[]), order (int[]), dist (double[])
// Dijkstra: fastest path tree from origin to every node, using each link's current varTravelTime.
// Fills the two arrays passed in (both of length parCols * parCols), indexed by node k = parXIndex * parCols + parYIndex. That index is built by taking all the points in the 2D grid, and laying their columns end-end.
//   time[k]: fastest travel time (minutes) from origin to node k; infinity if unreachable
//   prev[k]: last link on the fastest route into node k; null for the origin and unreachable nodes.
//            Follow prev from any node back to the origin to rebuild its whole route.
//   order:   node indices in the order they were settled, nearest first (order[0] is the origin).
//            Unreachable nodes are never settled, so the slots after the last settled node stay -1.
//            Looping through it backwards visits every node before any other node on its same route that happens to be closer to the origin
//   dist[k]: length (metres) of that fastest route to node k; infinity if unreachable.
//            This is the length of the fastest route, not the shortest possible route.
Arrays.fill(time, Double.POSITIVE_INFINITY);   // every node starts "unknown"
Arrays.fill(dist, Double.POSITIVE_INFINITY);
Arrays.fill(prev, null);
Arrays.fill(order, -1);
int settledCount = 0;

int o = origin.parXIndex * parCols + origin.parYIndex;
time[o] = 0;
dist[o] = 0;
PriorityQueue<double[]> queue = new PriorityQueue<>((x, y) -> Double.compare(x[0], y[0]));   // Each entry in this queue is itself an array: {time, node index}; poll() gives the smallest time
queue.add(new double[]{0, o}); //origin is the first point to be added to queue, and we know its time to itself is zero

while (!queue.isEmpty()) {
    double[] entry = queue.poll();   // closest entry. Poll also removes it so it is not present in the next loop
    int k = (int) entry[1];
    if (entry[0] > time[k]) continue;   // outdated entry: a faster time to node k was found after it was added, and that faster entry was polled (and k settled) first
    order[settledCount] = k;   // E.g. on third run, third slot of order is filled with k, meaning that node at index k was the third node settled
    settledCount++;
    GridPoint n = varGrid[k / parCols][k % parCols]; //truncation and remainders translate index k back into x and y grid indices respectively. The X index would need +1 if we used base 1 indexing like in other programming languages. 
    for (Link l : n.colLinks) {   // update each neighbour if going through n is faster
        GridPoint m = (l.parNodeA == n) ? l.parNodeB : l.parNodeA; //node at other end of link l
        int km = m.parXIndex * parCols + m.parYIndex; //the above node's index translated back into stacked column format
        double t = time[k] + l.varTravelTime;
        if (t < time[km]) { //if adding this link creates a new route shorter than the previously recorded one, then the recorded one gets updated to this
            time[km] = t;
            dist[km] = dist[k] + l.parLength;   // length of this new fastest route
            prev[km] = l; //this node now knows which link it must follow to go on the shortest path to origin.
            queue.add(new double[]{t, km});   // Add this neighbour to the queue of whom to settle. If the neighbour was already on the list with a slower time, then that entry remains; first the faster entry will be run through and settled. Then eventually one by one, each slower entry counterpart will be looked at, removed from the queue, and then immediately end its iteration when the second line in the iteration finds it was already settled.
        }
    }
}