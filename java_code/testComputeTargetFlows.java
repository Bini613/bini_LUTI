// TEMPORARY TEST. Paste at the bottom of Main's On startup (after agents are placed and impedances set).
// Runs funComputeTargetFlows, then recomputes the same flows the slow way (walking every citizen's whole route)
// and prints the largest difference between the two methods. It should be 0 (or a tiny rounding error).
double totalTime = funComputeTargetFlows();

int nNodes = parCols * parCols;
double[] t = new double[nNodes];
Link[] p = new Link[nNodes];
int[] o = new int[nNodes];
Map<Link, Double> slowFlow = new HashMap<>();   // link -> flow, slow method
for (int a = 0; a < activities.size(); a++) {
    GridPoint origin = activities.get(a).varLocation;
    funBuildFastestPathTree(origin, t, p, o);
    for (Citizen c : citizens) {
        GridPoint n = c.varHome;
        if (t[n.parXIndex * parCols + n.parYIndex] == Double.POSITIVE_INFINITY) continue;   // unreachable
        while (n != origin) {   // walk back along the route, adding the weight to every link
            Link l = p[n.parXIndex * parCols + n.parYIndex];
            slowFlow.merge(l, c.varActivityWeights[a], Double::sum);
            n = (l.parNodeA == n) ? l.parNodeB : l.parNodeA;
        }//Whole route is done for this citizen, next iteration begins for next citizen
    }//For this activity, all routes done for all citizens, next iteration begins for next activity
}

double maxDiff = 0;
double maxFlow = 0;
int loadedLinks = 0;
for (Link l : links) {
    maxDiff = max(maxDiff, abs(slowFlow.getOrDefault(l, 0.0) - l.varTargetFlowVol));
    maxFlow = max(maxFlow, l.varTargetFlowVol);
    if (l.varTargetFlowVol > 0) loadedLinks++;
}
traceln("All-or-nothing: busiest link flow " + roundToDecimal(maxFlow, 2) + ", links carrying flow " + loadedLinks + " of " + links.size()
        + ", total weighted time " + roundToDecimal(totalTime, 1) + " minutes");   // TEST
traceln("Difference from slow route-walking method: " + String.format("%.2e", maxDiff));   // TEST
