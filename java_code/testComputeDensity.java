// TEMPORARY TEST. Paste in Main's On startup, after funCalibrateDensity();
// Full density (ownWeight 0) at every node should equal varCrowding / varNearbyLand: the largest difference should be 0.
// (The excluding-self case is checked in testComputeRent.)
double worstFullError = 0;
for (GridPoint g : gridPoints) {
    if (g.varNearbyLand <= 0) continue;
    worstFullError = max(worstFullError, abs(funComputeDensity(g, g, /* ownWeight = */ 0) - g.varCrowding / g.varNearbyLand));
}

traceln("funComputeDensity: full-density largest difference " + String.format("%.2e", worstFullError));   // TEST
