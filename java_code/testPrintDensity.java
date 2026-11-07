// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments.
// Prints the current average density over occupied nodes (nodes with at least one citizen or activity),
// next to parDensityRef for comparison.
// Also prints the smallest and largest occupied-node density. Called by a cyclic test report event in Main (no longer by funSimulateNextTurn).
double sum = 0, minD = Double.MAX_VALUE, maxD = 0;
int count = 0;
for (GridPoint g : gridPoints) {
    if (g.colResidents.isEmpty() && g.colActivities.isEmpty()) continue;   // unoccupied node
    double d = funComputeDensity(g, g, 0);   // ownWeight 0: everyone included
    sum += d;
    minD = min(minD, d);
    maxD = max(maxD, d);
    count++;
}
funRuntimePrint("Turn " + varTurnCount + " density over " + count + " occupied nodes: avg " + String.format("%.3e", sum / count)
        + ", min " + String.format("%.3e", minD) + ", max " + String.format("%.3e", maxD)
        + " agents/m^2 (parDensityRef " + String.format("%.3e", parDensityRef) + ")");   // TEST
