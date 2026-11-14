// Function in Main. Just action (returns nothing). No arguments.
// Sets every node's varAvailableLand (via each GridPoint's funUpdateAvailableLand).
// Road land per link comes from Link funNodeRoadLand (lane width parRoadWidthRef, lanes, parCarLandUsage). Can be re-run after investments change impedances.
for (GridPoint g : gridPoints) g.funUpdateAvailableLand();

// TEST: actual road share (slightly below target if any node hit the floor), smallest node land, nodes floored.
// Only nodes with links count (unlinked nodes have no road land anyway).
double totalAvailable = 0;
double minAvailable = Double.MAX_VALUE;
int floored = 0;
int linkedNodes = 0;
for (GridPoint g : gridPoints) {
    if (g.colLinks.isEmpty()) continue;
    linkedNodes++;
    totalAvailable += g.varAvailableLand;
    minAvailable = min(minAvailable, g.varAvailableLand); //climbs down to the smallest available land actually present on the grid
    if (g.varAvailableLand <= parMinAvailableLand) floored++;
}
double roadShare = 1 - totalAvailable / (linkedNodes * parCellLand); //proportion left over once you subtract the available land's portion of the total
traceln("Available land set. Local road width " + roundToDecimal(parRoadWidthRef, 1) + " m;  land usage multiplier " + roundToDecimal(parCarLandUsage, 2) + "--> starting road share: " + roundToDecimal(roadShare, 3)
        + ", smallest node land: " + roundToDecimal(minAvailable, 0) + " m^2, nodes at floor: " + floored);   // TEST
