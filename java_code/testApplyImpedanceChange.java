// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funCalibrateDensity and funAssignTraffic).
// Upgrades a local link at median crowding to parImpedanceMin, prints what changed, then checks the incremental update against a
// fully independent recomputation from the current impedances: every node's available land (funSetAvailableLands), then the
// whole density surface and rents (funBuildDensitySurface). This checks the whole chain: the end nodes' land, and its spread
// to neighbouring nodes' nearby land and rent. Incremental and recomputed values should match to rounding.
// Part 2 (bottom): worst-case land check, with every link at the centre node upgraded to parImpedanceMin.

// Pick the non-arterial link whose node A has the median crowding (a typical location, neither empty nor the busiest)
List<Link> localLinks = new ArrayList<>();
for (Link l : links) {
    if (l.varImpedance > parImpedanceArterial) localLinks.add(l);   // skip arterials
}
localLinks.sort(Comparator.comparingDouble(l -> l.parNodeA.varCrowding));   // sort from least to most crowded node A
Link testLink = localLinks.get(localLinks.size() / 2);   // middle of the sorted list = median
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

// Part 2: worst case for land. Upgrade every link at the centre node to parImpedanceMin ("surrounded by freeways"),
// then show how much land the roads would take before the floor is applied.
GridPoint centre = varGrid[parCols / 2][parCols / 2];
double landBefore = centre.varAvailableLand;
for (Link l : centre.colLinks) funApplyImpedanceChange(l, parImpedanceMin);

double roadLand = 0;   // same formula as GridPoint's funUpdateAvailableLand, but without the floor
for (Link l : centre.colLinks) roadLand += 0.5 * varRoadWidthRef * l.parLength * pow(l.varImpedance, -parRoadLandUsageExponent);
traceln("Worst case at centre (" + centre.colLinks.size() + " links, all at impedance " + parImpedanceMin + "): land before " + roundToDecimal(landBefore, 0)
        + " m^2, now, road land: " + roundToDecimal(roadLand, 0) + " m^2 of " + roundToDecimal(parCellLand, 0) + " (" + roundToDecimal(100 * roadLand / parCellLand, 0)
        + "% of the cell), land after floor " + roundToDecimal(centre.varAvailableLand, 0) + " m^2");   // TEST: above 100% means roads alone would need more than the whole cell
