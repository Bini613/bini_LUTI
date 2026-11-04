// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funCalibrateDensity).
// Moves citizen 0 to a free corner-most node and back, and activity 0 likewise, then checks every node's crowding and rent
// are back to where they started (only rounding differences allowed). The first move is printed so you can see rent change.
double[] startCrowding = new double[parCols * parCols];
double[] startRent = new double[parCols * parCols];
for (GridPoint g : gridPoints) {
    startCrowding[g.parXIndex * parCols + g.parYIndex] = g.varCrowding;
    startRent[g.parXIndex * parCols + g.parYIndex] = g.varRent;
}

// Pick a linked node with no citizen and no activity, farthest from the centre
Set<GridPoint> taken = new HashSet<>();
for (Citizen c : citizens) taken.add(c.varHome);
for (Activity a : activities) taken.add(a.varLocation);
GridPoint target = null;
double farthest = -1;
for (GridPoint g : gridPoints) {
    if (g.colLinks.isEmpty() || taken.contains(g)) continue;
    double d_squared = pow(g.parXIndex - parCols / 2, 2) + pow(g.parYIndex - parCols / 2, 2);
    if (d_squared > farthest) { farthest = d_squared; target = g; }
}

Citizen c0 = citizens.get(0);
GridPoint c0Home = c0.varHome;
traceln("Citizen 0 moving: old home rent " + roundToDecimal(c0Home.varRent, 0) + ", target rent " + roundToDecimal(target.varRent, 0));   // TEST
funMoveCitizen(c0, target);
traceln("After move: old home rent " + roundToDecimal(c0Home.varRent, 0) + " (should drop), target rent " + roundToDecimal(target.varRent, 0) + " (should rise)");   // TEST
funMoveCitizen(c0, c0Home);   // move back

Activity a0 = activities.get(0);
GridPoint a0Home = a0.varLocation;
funMoveActivity(a0, target);
funMoveActivity(a0, a0Home);   // move back

double worstCrowdingError = 0, worstRentError = 0;
for (GridPoint g : gridPoints) {
    int k = g.parXIndex * parCols + g.parYIndex;
    worstCrowdingError = max(worstCrowdingError, abs(g.varCrowding - startCrowding[k]));
    worstRentError = max(worstRentError, abs(g.varRent - startRent[k]));
}
traceln("After moving back: largest crowding difference " + String.format("%.2e", worstCrowdingError) + ", largest rent difference " + String.format("%.2e", worstRentError) + " (both should be ~0)");   // TEST
