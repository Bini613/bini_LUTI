// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funCalibrateDensity and funAssignTraffic).
// Upgrades the first local link to parImpedanceMin, prints what changed, then checks the incremental update against a
// fully independent recomputation from the current impedances: every node's available land (funSetAvailableLands), then the
// whole density surface and rents (funBuildDensitySurface). This checks the whole chain: the end nodes' land, and its spread
// to neighbouring nodes' nearby land and rent. Incremental and recomputed values should match to rounding.
Link testLink = null;
double mostCrowded = -1;
for (Link l : links) {
    if (l.varImpedance <= parImpedanceArterial) continue;   // skip arterials
    if (l.parNodeA.varCrowding > mostCrowded) { 
        mostCrowded = l.parNodeA.varCrowding; testLink = l; 
    }a
}
GridPoint a = testLink.parNodeA;
traceln("Upgrading link (" + a.parXIndex + "," + a.parYIndex + ")-(" + testLink.parNodeB.parXIndex + "," + testLink.parNodeB.parYIndex + "): impedance "
        + roundToDecimal(testLink.varImpedance, 2) + " -> " + parImpedanceMin + ", capacity " + roundToDecimal(testLink.varCapacity, 1)
        + ", time " + roundToDecimal(testLink.varTravelTime, 2) + " minutes, end-node land " + roundToDecimal(a.varAvailableLand, 0) + " m^2, rent " + roundToDecimal(a.varRent, 0));   // TEST

funApplyImpedanceChange(testLink, parImpedanceMin);

traceln("After upgrade: capacity " + roundToDecimal(testLink.varCapacity, 1) + " (should rise), time " + roundToDecimal(testLink.varTravelTime, 2)
        + " minutes (should fall), end-node land " + roundToDecimal(a.varAvailableLand, 0) + " m^2 (should fall), rent " + roundToDecimal(a.varRent, 0) + " (should rise)");   // TEST

// Compare the incremental update with a full recomputation
double[] incLand = new double[parCols * parCols];
double[] incRent = new double[parCols * parCols];
for (GridPoint g : gridPoints) {
    incLand[g.parXIndex * parCols + g.parYIndex] = g.varNearbyLand;
    incRent[g.parXIndex * parCols + g.parYIndex] = g.varRent;
}
funSetAvailableLands();     // recompute every node's available land from the current impedances (independent of the end-node update above; also prints its road-share line)
funBuildDensitySurface();   // rebuild nearby land and crowding from scratch; varDensityRef is already set, so this also recomputes every rent
double worstLandError = 0, worstRentError = 0;
for (GridPoint g : gridPoints) {
    int k = g.parXIndex * parCols + g.parYIndex;
    worstLandError = max(worstLandError, abs(g.varNearbyLand - incLand[k]));
    worstRentError = max(worstRentError, abs(g.varRent - incRent[k]));
}
traceln("Incremental vs full recomputation:largest nearby-land difference " + String.format("%.2e", worstLandError) + " m^2, largest rent difference " + String.format("%.2e", worstRentError) + " (both should be ~0)");   // TEST
