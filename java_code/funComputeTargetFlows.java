// Function in Main. Returns value, type: double. Arguments: nodeDemand (double[][])
// All-or-nothing loading: puts all demand onto the current fastest routes (using each link's current varTravelTime)
// and stores the result in every link's varTargetFlowVol (Link variable, double).
// Demand: each citizen sends its weight for activity a (varActivityWeights[a]) from its home to activity a.
// nodeDemand[a][k] holds those weights already summed per home node (built once by funAssignTraffic, since citizens don't move during an assignment).
// Returns T_fastest: the total time everyone would spend travelling if they all used the current fastest routes.
//   T_fastest = sum over citizens c and activities a of  w[c][a] * tFastest[c][a]
//   (w = citizen's weight for the activity, tFastest = fastest time from the citizen's home to the activity)
// The equilibrium loop compares it with the total time actually spent on the current (blended) flows: T_actual
//   T_actual = sum over links l of  V[l] * t[l]   (V = varFlowVol, t = varTravelTime)
//   relative gap = (T_actual - T_fastest) / T_actual
// At equilibrium nobody can save time by switching routes, so the two match and the gap is 0.
// Also stores each activity's fastest times in varAccessTimes (Main variable, double[][], indexed [activity][node]),
// so after funAssignTraffic finishes, it holds the equilibrium times from every node to every activity.
// Likewise stores the lengths (metres) of those fastest routes in varAccessDistances (Main variable, double[][], [activity][node]).
//
//
// PARALLEL: each activity's pass (Dijkstra tree, access times, loading) is independent of the others, so the activities are
// spread over the computer's cores (parParallelAssignment = true; false runs them one after another, for comparison).
// Java's parallel stream decides the number of threads itself (from the cores available) and hands out activities as threads come free.
// Rules that make this safe:
//   - each activity gets its own arrays (tree: time, prev, order, dist, either from the cache or created inside its pass; and thisActDemand)
//   - shared data is only read during the passes (grid, links, travel times, nodeDemand, cached trees looked up beforehand);
//     each pass writes only its own slots of the per-activity arrays below. The tree cache and varAccessTimes / varAccessDistances
//     are only written afterwards, on the main thread
//   - no AnyLogic calls (random draws, traceln, agent changes) inside the passes
//   - link target flows are added up afterwards, on the main thread, in activity order, so results are the same every run
int nNodes = parCols * parCols;
int nAct = activities.size();
int nLinks = links.size();

// Create the access time tables on the first call (or if the number of activities has changed).
// Their rows are filled below with each activity's tree arrays (shared with the tree cache, so they must only be read).
if (varAccessTimes == null || varAccessTimes.length != nAct) varAccessTimes = new double[nAct][];
if (varAccessDistances == null || varAccessDistances.length != nAct) varAccessDistances = new double[nAct][];

// Read everything the passes need from AnyLogic collections here, on the main thread
GridPoint[] origins = new GridPoint[nAct];
for (int a = 0; a < nAct; a++) origins[a] = activities.get(a).varLocation;

// TREE CACHE (see funClearTreeCache): look up each activity node's cached tree here, on the main thread.
// A cached tree exists only if the link times haven't changed since it was built (the cache is cleared whenever they change),
// so it is exactly the tree Dijkstra would build now. In practice: on an assignment's first pass, activities that haven't moved
// reuse the trees from the previous assignment's final pass, and a moved activity usually reuses the tree its relocation choice built.
// On later passes the cache has been cleared (after each step), so every tree is built fresh.
// Inside the parallel passes, each activity only reads and writes its own slots of these per-activity arrays; new trees are put
// into the shared cache afterwards, on the main thread.
if (varTreeTime == null || varTreeTime.length != nNodes) funClearTreeCache();   // first use
double[][] time = new double[nAct][];   // per activity: its tree (null = not cached, build it)
double[][] dist = new double[nAct][];
Link[][] prev = new Link[nAct][];
int[][] order = new int[nAct][];
boolean[] built = new boolean[nAct];     // true if the tree was built in this call (to be stored in the cache afterwards)
for (int a = 0; a < nAct; a++) {
    int k = origins[a].parIndex;
    time[a] = varTreeTime[k];
    dist[a] = varTreeDist[k];
    prev[a] = varTreePrev[k];
    order[a] = varTreeOrder[k];
}
// Each link's position in 'links', looked up once here. AnyLogic's getIndex() is not thread-safe (it uses an internal
// cache), so calling it inside the parallel passes can crash with IndexOutOfBoundsException. Reading a finished map is safe.
java.util.IdentityHashMap<Link, Integer> linkIndex = new java.util.IdentityHashMap<>(nLinks);
for (int i = 0; i < nLinks; i++) linkIndex.put(links.get(i), i);

// Per-activity results, each written only by its own activity's pass
double[][] actFlow = new double[nAct][nLinks];   // target flow each activity puts on each link, indexed by the link's position in 'links' (linkIndex)
double[] actFastestTime = new double[nAct];     // that activity's share of totalFastestTime
double[] actUnreachable = new double[nAct];     // that activity's unreachable demand (should stay 0)

java.util.stream.IntStream acts = java.util.stream.IntStream.range(0, nAct); // Creates an IntStream object: it holds a rule for producing a sequence of numbers (here: start and end), and methods that take
// code blocks and decide how to run them over that sequence (one by one, or split across threads if .parallel() is set).
// range(start, end) creates it (a factory method, acting like a constructor): numbers from start up to end - 1, produced one at a time, never stored as a list.
// Other methods (e.g. filter, map) can be applied afterwards, adding steps to the rule.   
if (parParallelAssignment) acts = acts.parallel(); // Turns on the "run in parallel" flag inside the IntStream object, so that the method that runs the code (forEach) splits the
// sequence of numbers into subgroups (sub-ranges, split further as needed), which run at the same time.
// Java hands each subgroup to the next free thread in its thread pool; the operating system decides which CPU core runs each thread.
acts.forEach(thisAct -> {
    // This activity's fastest path tree: from the cache if there is one, otherwise built now into new arrays (one set per activity, so threads never share them)
    if (time [thisAct] == null) {
        time [thisAct] = new double[nNodes];
        dist [thisAct] = new double[nNodes];
        prev [thisAct] = new Link  [nNodes];
        order[thisAct] = new int   [nNodes];
        funBuildFastestPathTree(origins[thisAct], time[thisAct], prev[thisAct], order[thisAct], dist[thisAct]);
        built[thisAct] = true;
    }
    double[] thisActDemand = nodeDemand[thisAct].clone();   // demand waiting at each node, to be passed towards this particular activity.

    //1. thisActDemand starts with entry for each node, that represents the combined weights for thisAct of all citizens living there (or zero if node is unoccupied or no one there demands thisAct). In step 2, thisActDemand is changed iteratively, accumulating the demand of every node further out whose route to thisAct passes through it. We get starting demands from nodeDemand passed down as an argument from funAssignTraffic, rather than recalculating it in the present function, so that it needn't be repeatedly generated on the many times funAssignTraffic calls the present function. 
    //nodeDemand therefore must only be read, not changed by the present function, and this is why thisActDemand is created as a separate variable from nodeDemand, because it is changed progressively as overlapping routes accumulate demand (for the purpose of accumulating the target flows on the connecting links). 
    double fastest = 0, unreachable = 0;
    for (int k = 0; k < nNodes; k++) { //This activity's total travel time (added to the other activities' after all passes finish)
        if (time[thisAct][k] == Double.POSITIVE_INFINITY) {   // Track unreachable demand and don't add it to the total travel time. Instead add it to a total of unreachable demand. There should be no unreachable demand.
            unreachable += thisActDemand[k];
            continue;
        }
        fastest += thisActDemand[k] * time[thisAct][k];
    }
    actFastestTime[thisAct] = fastest;
    actUnreachable[thisAct] = unreachable;

    // 2. Ordered from farthest to nearest travel times (settle order backwards), each node passes its demand onto its previous link,
    //    and on to the node at the other end of that link, one step closer to the activity.
    //    Every node is processed after all nodes further out on its branch, so its demand is already complete.
    for (int s = nNodes - 1; s >= 1; s--) {   // s = 0 is the activity's own node: nothing to pass on
        int k = order[thisAct][s]; //get the grid index k of the first node to look at, which is the last one settled
        if (k == -1 || thisActDemand[k] == 0) continue;   // unsettled (unreachable) slot, or nothing to pass on
        Link l = prev[thisAct][k];
        actFlow[thisAct][linkIndex.get(l)] += thisActDemand[k];   // this activity's own flow array, not the shared link variable (other threads may be loading the same link)
        GridPoint up = (l.parNodeA.parIndex == k) ? l.parNodeB : l.parNodeA;   // up is defined as the next node along the route to activity thisAct. The conditional makes sure to pick the correct end of link l which is the next node, not the current one (node k).
        thisActDemand[up.parIndex] += thisActDemand[k];//Next node up the chain receives the previous one's demand on top of what it already had from other routes already tracked.
    }
});

// All passes done: back on the main thread.
// Access times and distances for the citizens' choices: each activity's row is its tree's arrays (read only, shared with the cache)
for (int a = 0; a < nAct; a++) {
    varAccessTimes[a] = time[a];
    varAccessDistances[a] = dist[a];
    if (built[a]) funStoreTree(origins[a].parIndex, time[a], dist[a], prev[a], order[a]);   // new trees go into the cache (skipped if a co-located activity's tree is already there)
}

// Add up the activities' results in activity order
double totalFastestTime = 0;
double unreachableDemand = 0; //For testing: Should remain zero
for (int a = 0; a < nAct; a++) {
    totalFastestTime += actFastestTime[a];
    unreachableDemand += actUnreachable[a];
}
for (int i = 0; i < nLinks; i++) {
    double sum = 0;
    for (int a = 0; a < nAct; a++) sum += actFlow[a][i];
    links.get(i).varTargetFlowVol = sum;   // replaces the old value, so no separate reset to 0 is needed
}

if (unreachableDemand > 0) traceln("WARNING: demand from homes with no route to an activity was ignored: " + unreachableDemand);
return totalFastestTime;
