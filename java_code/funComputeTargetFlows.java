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
//initialise empty time, prev and order arrays to be filled in shortly by funBuildFastestPathTree:
int nNodes = parCols * parCols;
double[] time = new double[nNodes]; 
Link[] prev = new Link[nNodes];
int[] order = new int[nNodes];
double[] dist = new double[nNodes];
double[] thisActDemand = new double[nNodes];   // demand waiting at each node, to be passed towards the activity
double totalFastestTime = 0;
double unreachableDemand = 0; //For testing: Should remain zero

for (Link l : links) l.varTargetFlowVol = 0; //initialise target flow

// Create the access time table on the first call (or if the number of activities or nodes has changed)
if (varAccessTimes == null || varAccessTimes.length != activities.size() || varAccessTimes[0].length != nNodes)//varAccessTimes[0] is the array of access times for activity zero, which is the same number of nodes as any other activity, since they all are looking at the same grid.
    varAccessTimes = new double[activities.size()][nNodes];
if (varAccessDistances == null || varAccessDistances.length != activities.size() || varAccessDistances[0].length != nNodes)
    varAccessDistances = new double[activities.size()][nNodes];

for (int thisAct = 0; thisAct < activities.size(); thisAct++) {
    funBuildFastestPathTree(activities.get(thisAct).varLocation, time, prev, order, dist); //first argument selects the gridPoint for the current activity in this loop. Time array passed in from previous loop needn't be empty, since this function resets it internally
    System.arraycopy(time, 0, varAccessTimes[thisAct], 0, nNodes);   // keep a copy of this activity's times (time[] is reused for the next activity). Recall this time calculation considered congestion, because the fasest tree looks at travel times in the BPR function, from whatever the last calculated flows were.
    System.arraycopy(dist, 0, varAccessDistances[thisAct], 0, nNodes);   // same for the lengths of those routes

    //1. thisActDemand starts with entry for each node, that represents the combined weights for thisAct of all citizens living there (or zero if node is unoccupied or no one there demands thisAct). In step 2, thisActDemand is changed iteratively, accumulating the demand of every node further out whose route to thisAct passes through it. We get starting demands from nodeDemand passed down as an argument from funAssignTraffic, rather than recalculating it in the present function, so that it needn't be repeatedly generated on the many times funAssignTraffic calls the present function. 
    //nodeDemand therefore must only be read, not changed by the present function, and this is why thisActDemand is created as a separate variable from nodeDemand, because it is changed progressively as overlapping routes accumulate demand (for the purpose of accumulating the target flows on the connecting links). A copy function is used rather than assignment because assignment would just make both names refer to the same array, so changes would corrupt nodeDemand. A clone would also work, but then each iteration would produce a different clone in memory which would need clearing up later. Copy overwrites between iterations, so no needless memory accumulation between iterations, for which Java would waste time at the end clearing up automatically. 
    System.arraycopy(nodeDemand[thisAct], 0, thisActDemand, 0, nNodes); 
    for (int k = 0; k < nNodes; k++) { //Add all the travel time to this activity to the accumulation of all activities'
        if (time[k] == Double.POSITIVE_INFINITY) {   // Track unreachable demand and don't add it to the total travel time. There should be no unreachable demand.
            unreachableDemand += thisActDemand[k];
            continue;
        }
        totalFastestTime += thisActDemand[k] * time[k];
    }

    // 2. Ordered from farthest to nearest travel times (settle order backwards), each node passes its demand onto its previous link,
    //    and on to the node at the other end of that link, one step closer to the activity.
    //    Every node is processed after all nodes further out on its branch, so its demand is already complete.
    for (int s = nNodes - 1; s >= 1; s--) {   // s = 0 is the activity's own node: nothing to pass on
        int k = order[s]; //get the index k of the first node to look at, which is the last one settled
        if (k == -1 || thisActDemand[k] == 0) continue;   // unsettled (unreachable) slot, or nothing to pass on
        Link l = prev[k];
        l.varTargetFlowVol += thisActDemand[k];
        GridPoint up = (l.parNodeA == varGrid[k / parCols][k % parCols]) ? l.parNodeB : l.parNodeA;   // up is defined as the next node along the route to activity thisAct. We had to convert k indices back to grid indices for varGrid[][]. The conditional makes sure to pick the correct end of link l which is the next node, not the current one.
        thisActDemand[up.parXIndex * parCols + up.parYIndex] += thisActDemand[k];//Next node up the chain receives the previous one's demand on top of what it already had from other routes already tracked.
    }
}

if (unreachableDemand > 0) traceln("WARNING: demand from homes with no route to an activity was ignored: " + unreachableDemand);
return totalFastestTime;
