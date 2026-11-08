// Function in Main. Just action (returns nothing). No arguments.
// Builds every node's varCrowding and varNearbyLand from scratch, by resetting them to 0 and then letting each node j
// contribute its own crowding n[j] and available land L[j] through funUpdateNeighbourhoodDensity (the same function later updates use).
//   n[j] = citizens living at j + parActivityCrowdingWeight * activities at j
// Then sets every node's rent once, from the finished surface.
// Needs agents placed and varAvailableLand set (funSetAvailableLands) first.
// Rent updates inside funUpdateNeighbourhoodDensity are paused during the build (varPauseRentUpdates), because each node's
// nearby land is incomplete until every node has contributed: rents computed mid-build would be wasted work, and a node
// with no nearby land yet would stop the run in funComputeDensity.
// Also builds varDensityKernel (Main variable, double[][]), the distance-decay weights read by funUpdateNeighbourhoodDensity and
// funComputeDensity: so this function must run (at startup) before either of those is used.

// Density kernel: weight exp(-d / r) for every grid offset (dx, dy) within the cutoff, 0 beyond it.
// Grid distances never change, so the weights are computed once here instead of on every density update or lookup.
double cutoff = parRentInfluenceDecayLength_CutOffMultiple * parRentInfluenceDecayLength;   // metres
int reach = (int) ceil(cutoff / parStepLength);   // cutoff in whole grid steps
varDensityKernel = new double[reach + 1][reach + 1];   // indexed [|dx|][|dy|], the absolute grid offsets between two nodes
for (int dx = 0; dx <= reach; dx++) {
    for (int dy = 0; dy <= reach; dy++) {
        double d = sqrt(dx * dx + dy * dy) * parStepLength;   // metres
        varDensityKernel[dx][dy] = (d <= cutoff) ? exp(-d / parRentInfluenceDecayLength) : 0;   // 0 for the square's corners beyond the cutoff circle
    }
}

double[] n = new double[parCols * parCols];   // crowding at each node, index parIndex
for (Citizen c : citizens) n[c.varHome.parIndex] += 1;
for (Activity a : activities) n[a.varLocation.parIndex] += parActivityCrowdingWeight; //Common universal weight as simplification in this model
//Empty nodes will just have n=0, which will affect densities in funUpdateNeighbourhoodDensity() as intended

for (GridPoint g : gridPoints) { //reset crowding and nearbyland globally
    g.varCrowding = 0;
    g.varNearbyLand = 0;
}

varPauseRentUpdates = true;   // Main variable (boolean): rent updates off until the surface is complete
for (GridPoint g : gridPoints) {
    funUpdateNeighbourhoodDensity(g, n[g.parIndex], g.varAvailableLand);// Both arguments are normally changes (dCrowding, dLand). Since this function first resets varCrowding and varNearbyLand to 0,
// the change at each node is from zero to its full value: its crowding n[k] and its available land varAvailableLand.
}
varPauseRentUpdates = false;

for (GridPoint g : gridPoints) g.varRent = funComputeRent(funComputeDensity(g, g, /* ownWeight = */ 0));   // every node's rent (everyone included), now that the surface is complete

// TEST: average density over occupied nodes (compare with parDensityRef), rent range over all nodes, and average rent over occupied nodes
double sumD = 0, sumOccupiedR = 0, minR = Double.MAX_VALUE, maxR = 0;
int occupied = 0;
for (GridPoint g : gridPoints) {
    minR = min(minR, g.varRent); //Climbs down to minimum rent
    maxR = max(maxR, g.varRent); //Climbs up to maximum rent
    if (g.colResidents.isEmpty() && g.colActivities.isEmpty()) continue;   // unoccupied node
    sumD += funComputeDensity(g, g, 0);
    sumOccupiedR += g.varRent;
    occupied++;
}
traceln("Density surface built. Average density over " + occupied + " occupied nodes: " + String.format("%.3e", sumD / occupied) + " agents/m^2 (parDensityRef "
        + String.format("%.3e", parDensityRef) + "). Rent range: " + roundToDecimal(minR, 0) + " to " + roundToDecimal(maxR, 0)
        + ", average on occupied nodes " + roundToDecimal(sumOccupiedR / occupied, 0) + " (parRentRef " + parRentRef + ", parRentMin " + parRentMin + ")");   // TEST
