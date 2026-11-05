// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funCalibrateDensity and funAssignTraffic).
// Upgrades a local link at median crowding to parImpedanceMin, prints what changed, then checks the incremental update against a
// fully independent recomputation from the current impedances: every node's available land (funSetAvailableLands), then the
// whole density surface and rents (funBuildDensitySurface). This checks the whole chain: the end nodes' land, and its spread
// to neighbouring nodes' nearby land and rent. Incremental and recomputed values should match to rounding.
// Part 2 (bottom): worst-case land check, with every link at the centre node upgraded to parImpedanceMin.

// Pick the non-arterial link with at least one occupied end, whose occupied end has the median crowding
// (a typical lived-in location, neither the quietest nor the busiest). That occupied end ("watched" node, node A if both are occupied) is the one reported.
Set<GridPoint> occupiedNodes = new HashSet<>();   // nodes with a citizen and/or an activity
for (Citizen c : citizens) occupiedNodes.add(c.varHome);
for (Activity act : activities) occupiedNodes.add(act.varLocation);
java.util.function.Function<Link, GridPoint> watched = l -> occupiedNodes.contains(l.parNodeA) ? l.parNodeA : l.parNodeB;   // the occupied end of a link
List<Link> localLinks = new ArrayList<>();
for (Link l : links) {
    boolean anyEndOccupied = occupiedNodes.contains(l.parNodeA) || occupiedNodes.contains(l.parNodeB);
    if (l.varImpedance > parImpedanceArterial && anyEndOccupied && watched.apply(l).colLinks.size() >= 2) localLinks.add(l);   // skip arterials, links with both ends empty, and links whose watched node has no second link to upgrade
}
localLinks.sort(Comparator.comparingDouble(l -> watched.apply(l).varCrowding));   // sort from least to most crowded occupied end
Link testLink = localLinks.get(localLinks.size() / 2);   // middle of the sorted list = median
GridPoint a = watched.apply(testLink);   // the end node whose land and rent are reported
traceln("Upgrading link (" + testLink.parNodeA.parXIndex + "," + testLink.parNodeA.parYIndex + ")-(" + testLink.parNodeB.parXIndex + "," + testLink.parNodeB.parYIndex + "), watching occupied end (" + a.parXIndex + "," + a.parYIndex + "): impedance "
        + roundToDecimal(testLink.varImpedance, 2) + " -> " + parImpedanceMin + ", capacity " + roundToDecimal(testLink.varCapacity, 1)
        + ", time " + roundToDecimal(testLink.varTravelTime, 2) + " minutes, end-node land " + roundToDecimal(a.varAvailableLand, 0) + " m^2, rent " + roundToDecimal(a.varRent, 0));   // TEST

double rentBefore = a.varRent;   // watched node's rent before any upgrade, for the percentage changes
funApplyImpedanceChange(testLink, parImpedanceMin);
double rentAfterOne = a.varRent;

traceln("After upgrade: capacity " + roundToDecimal(testLink.varCapacity, 1) + " (should rise), time " + roundToDecimal(testLink.varTravelTime, 2)
        + " minutes (should fall), end-node land " + roundToDecimal(a.varAvailableLand, 0) + " m^2 (should fall), rent " + roundToDecimal(a.varRent, 0)
        + " (should rise): " + roundToDecimal(100 * (rentAfterOne - rentBefore) / rentBefore, 2) + "% vs before");   // TEST

// Second upgrade at the same watched node: another of its links (not already at parImpedanceMin) also becomes a freeway
Link secondLink = null;
for (Link l : a.colLinks) {
    if (l != testLink && l.varImpedance > parImpedanceMin) { secondLink = l; break; }
}
if (secondLink == null) traceln("Second upgrade skipped: node (" + a.parXIndex + "," + a.parYIndex + ") has no other link to upgrade");   // TEST
else {
    traceln("Upgrading second link (" + secondLink.parNodeA.parXIndex + "," + secondLink.parNodeA.parYIndex + ")-(" + secondLink.parNodeB.parXIndex + "," + secondLink.parNodeB.parYIndex
            + ") at the same node: impedance " + roundToDecimal(secondLink.varImpedance, 2) + " -> " + parImpedanceMin);   // TEST
    funApplyImpedanceChange(secondLink, parImpedanceMin);
    double rentAfterTwo = a.varRent;
    traceln("After second upgrade: end-node land " + roundToDecimal(a.varAvailableLand, 0) + " m^2 (should fall further), rent " + roundToDecimal(a.varRent, 0) + " (should rise further)");   // TEST
    traceln("Rent change vs before any upgrade: 1 freeway " + roundToDecimal(100 * (rentAfterOne - rentBefore) / rentBefore, 2) + "%, 2 freeways "
            + roundToDecimal(100 * (rentAfterTwo - rentBefore) / rentBefore, 2) + "%");   // TEST
}

// Compare the incremental update (after both upgrades) with a full recomputation
double[] incLand = new double[parCols * parCols];
double[] incRent = new double[parCols * parCols];
for (GridPoint g : gridPoints) {
    incLand[g.parXIndex * parCols + g.parYIndex] = g.varNearbyLand;
    incRent[g.parXIndex * parCols + g.parYIndex] = g.varRent;
}
funSetAvailableLands();     // recompute every node's available land from the current impedances (independent of the end-node update above; also prints its road-share line)
// Rebuild nearby land and crowding from scratch. varDensityRef is set to 0 during the rebuild so rent calculations are turned off, so that rents aren't repeatedly written with incomplete accumulations of varNearbyLand prior to final complete accumulations.
// This also protects against a divide by zero error in density as the funComputeRent argument, for nodes who have no nearbyland yet, and whose neigbhourhood centre gives it none in funUpdateNeighbourhoodDensity. 
//Such nodes probably won't have errors in rent calculation that comes for all nodes that comes afterwards, though it is still possible, if an entire neighbourhood has no available land (unlikely). That is why parMinAvailableLand should be set to a small number and not zero.
double savedDensityRef = varDensityRef;
varDensityRef = 0;
funBuildDensitySurface();
varDensityRef = savedDensityRef;
for (GridPoint g : gridPoints) g.varRent = funComputeRent(funComputeDensity(g, g, /* ownWeight = */ 0));
double worstLandError = 0, worstRentError = 0;
for (GridPoint g : gridPoints) {
    int k = g.parXIndex * parCols + g.parYIndex;
    worstLandError = max(worstLandError, abs(g.varNearbyLand - incLand[k]));
    worstRentError = max(worstRentError, abs(g.varRent - incRent[k]));
}
traceln("Incremental vs full recomputation: largest nearby-land difference " + String.format("%.2e", worstLandError) + " m^2, largest rent difference " + String.format("%.2e", worstRentError) + " (both should be ~0)");   // TEST

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
