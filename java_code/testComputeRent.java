// TEMPORARY TEST. Paste in Main's On startup, after funCalibrateDensity();
// 1. Inclusive rent: recomputes every node's rent with funComputeRent from its stored density, and compares with the stored varRent.
//    The stored rent now comes from the same formula, so this only checks that varRent is current. The largest difference should be 0.
// 2. Rent excluding self: for citizen 0, compares funComputeRent(funComputeDensity(..., 1)) with the "real" answer:
//    actually removing the citizen's crowding, reading the rents, then putting it back. Checked at its home and at one
//    neighbouring node. Each computed / actual pair should match, both below the stored rent.

// 1. Inclusive rent
double worstDiff = 0;
for (GridPoint g : gridPoints) {
    if (g.varNearbyLand <= 0) continue;   // safety: rent updates skip these too
    double r = funComputeRent(g.varCrowding / g.varNearbyLand);
    worstDiff = max(worstDiff, abs(r - g.varRent));
}
traceln("funComputeRent vs stored rent, largest difference: " + String.format("%.2e", worstDiff) + " (should be 0)");   // TEST

// 2. Rent excluding self
Citizen c0 = citizens.get(0);
GridPoint home = c0.varHome;
int nbrX = (home.parXIndex + 1 < parCols) ? home.parXIndex + 1 : home.parXIndex - 1;   // one step to the right, or to the left if home is on the right edge
GridPoint nbr = varGrid[nbrX][home.parYIndex];

double computedHome = funComputeRent(funComputeDensity(home, home, /* ownWeight = */ 1));   // citizen's weight is 1
double computedNbr = funComputeRent(funComputeDensity(nbr, home, /* ownWeight = */ 1));
double storedHome = home.varRent;
double storedNbr = nbr.varRent;

funUpdateNeighbourhoodDensity(home, -1, 0);   // actually remove citizen 0's crowding (updates rent at home and every node within the cutoff)
double actualHome = home.varRent;
double actualNbr = nbr.varRent;
funUpdateNeighbourhoodDensity(home, 1, 0);    // put it back

traceln("Rent excluding self at home: computed " + roundToDecimal(computedHome, 1) + ", actual " + roundToDecimal(actualHome, 1)
        + " (stored rent with self: " + roundToDecimal(storedHome, 1) + ")");   // TEST: computed and actual should match, both below stored
traceln("Rent excluding self at neighbour: computed " + roundToDecimal(computedNbr, 1) + ", actual " + roundToDecimal(actualNbr, 1)
        + " (stored rent with self: " + roundToDecimal(storedNbr, 1) + ")");   // TEST: should match
