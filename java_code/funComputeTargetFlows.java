// Function in Main. Returns value, type: double. No arguments.
// All-or-nothing loading: puts all demand onto the current fastest routes (using each link's current varTravelTime)
// and stores the result in every link's varTargetFlowVol (Link variable, double).
// Demand: each citizen sends its weight for activity a (varActivityWeights[a]) from its home to activity a.
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
//initialise empty time, prev and order arrays to be filled in shortly by funBuildFastestPathTree:
int nNodes = parCols * parCols;
double[] time = new double[nNodes]; 
Link[] prev = new Link[nNodes];
int[] order = new int[nNodes];
double[] dist = new double[nNodes];
double[] demand = new double[nNodes];   // demand waiting at each node, to be passed towards the activity
double totalFastestTime = 0;
double unreachableDemand = 0; //For testing: Should remain zero

for (Link l : links) l.varTargetFlowVol = 0; //initialise target flow

// Create the access time table on the first call (or if the number of activities or nodes has changed)
if (varAccessTimes == null || varAccessTimes.length != activities.size() || varAccessTimes[0].length != nNodes)//varAccessTimes[0] is the array of access times for activity zero, which is the same number of nodes as any other activity, since they all are looking at the same grid.
    varAccessTimes = new double[activities.size()][nNodes];
if (varAccessDistances == null || varAccessDistances.length != activities.size() || varAccessDistances[0].length != nNodes)
    varAccessDistances = new double[activities.size()][nNodes];

for (int a = 0; a < activities.size(); a++) {
    funBuildFastestPathTree(activities.get(a).varLocation, time, prev, order, dist); //first argument selects the gridPoint for the current activity in this loop. Time array passed in from previous loop needn't be empty, since this function resets it internally
    System.arraycopy(time, 0, varAccessTimes[a], 0, nNodes);   // keep a copy of this activity's times (time[] is reused for the next activity). Recall this time calculation considered congestion, because the fasest tree looks at travel times in the BPR function, from whatever the last calculated flows were.
    System.arraycopy(dist, 0, varAccessDistances[a], 0, nNodes);   // same for the lengths of those routes

    // 1. Each node starts with the weights of the citizens living there for activity a
    Arrays.fill(demand, 0);
    for (Citizen c : citizens) {
        int k = c.varHome.parXIndex * parCols + c.varHome.parYIndex; //convert into stacked columns index form
        double w = c.varActivityWeights[a]; //weight of citizen c for activity a
        if (time[k] == Double.POSITIVE_INFINITY) {   // no route to this activity: can't be loaded
            unreachableDemand += w;
            continue;
        }
        demand[k] += w;   // += so several citizens on one node add up
        totalFastestTime += w * time[k];
    }

    // 2. Ordered from farthest to nearest travel times (settle order backwards), each node passes its demand onto its previous link,
    //    and on to the node at the other end of that link, one step closer to the activity.
    //    Every node is processed after all nodes further out on its branch, so its demand is already complete.
    for (int s = nNodes - 1; s >= 1; s--) {   // s = 0 is the activity's own node: nothing to pass on
        int k = order[s]; //get the index k of the first node to look at, which is the last one settled
        if (k == -1 || demand[k] == 0) continue;   // unsettled (unreachable) slot, or nothing to pass on
        Link l = prev[k];
        l.varTargetFlowVol += demand[k];
        GridPoint up = (l.parNodeA == varGrid[k / parCols][k % parCols]) ? l.parNodeB : l.parNodeA;   // up is defined as the next node along the route to activity a. We had to convert k indices back to grid indices for varGrid[][]. The conditional makes sure to pick the correct end of link l which is the next node, not the current one.
        demand[up.parXIndex * parCols + up.parYIndex] += demand[k];//up indices converted back to stacked column form, and up receives the demand from the link.
    }
}

if (unreachableDemand > 0) traceln("WARNING: demand from homes with no route to an activity was ignored: " + unreachableDemand);
return totalFastestTime;
