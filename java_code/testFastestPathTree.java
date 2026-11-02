// TEMPORARY TEST. Paste at the bottom of Main's On startup (after agents are placed and impedances set).
// Runs funFastestPathTree from the first activity, colours nodes by travel time,
// and highlights the route to the slowest-to-reach node.
double[] testTime = new double[parCols * parCols];
Link[] testPrev = new Link[parCols * parCols];
GridPoint testOrigin = activities.get(0).varLocation;
funFastestPathTree(testOrigin, testTime, testPrev);

GridPoint farthest = testOrigin;
int unreachable = 0;
for (GridPoint g : gridPoints) {
    double t = testTime[g.parXIndex * parCols + g.parYIndex];
    g.varTestTime = t; //every point records its time to access testOrigin
    if (t == Double.POSITIVE_INFINITY) unreachable++;
    else if (t > farthest.varTestTime) farthest = g; //update the farthest point to the current one in the loop, only if the time to get there is bigger than the previous farthest time
}
varTestMaxTime = farthest.varTestTime;

// Walk back from the farthest node to the origin along prev links
int steps = 0;
GridPoint n = farthest;
while (n != testOrigin) {
    Link l = testPrev[n.parXIndex * parCols + n.parYIndex];
    l.varTestOnPath = true;
    n = (l.parNodeA == n) ? l.parNodeB : l.parNodeA; //update node to next link in path
    steps++;
}
traceln("Fastest path tree from (" + testOrigin.parXIndex + "," + testOrigin.parYIndex + "): farthest node (" + farthest.parXIndex + "," + farthest.parYIndex + ") at " + (int) varTestMaxTime + " minutes, " + steps + " links; unreachable nodes: " + unreachable);   // TEST
