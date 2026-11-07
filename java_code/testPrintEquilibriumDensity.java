// TEMPORARY TEST. Function in Main. Returns value, type: boolean. No arguments. Called by the cyclic test report event evTestReport.
// Equilibrium check on average density over occupied nodes (nodes with at least one citizen or activity).
// Compares the current average with the one stored at the previous report (varAverageDensity):
//   relative change = |current - previous| / previous
// If the change is within parDensityEquilibriumTolerance (e.g. 0.05 = 5%), prints the density and stops the simulation.
// Returns true when equilibrium is reached (so evTestReport can run its other reports first), false otherwise.
// On the first report there is nothing to compare with yet, so it only stores the average.

double sum = 0;
int count = 0;
for (GridPoint g : gridPoints) {
    if (g.colResidents.isEmpty() && g.colActivities.isEmpty()) continue;   // unoccupied node
    sum += funComputeDensity(g, g, 0);   // ownWeight 0: everyone included
    count++;
}
double current = sum / count;
double previous = varAverageDensity;
varAverageDensity = current;   // stored for the next report's comparison

if (previous <= 0) return false;   // first report: nothing to compare with yet
double change = abs(current - previous) / previous;
if (change > parDensityEquilibriumTolerance) return false;   // still changing: keep running

funRuntimePrint("Density equilibrium reached at turn " + varTurnCount + " (round " + varRounds + "): average occupied-node density "
        + String.format("%.3e", current) + " agents/m^2, change since last report " + roundToDecimal(100 * change, 2) + "% (tolerance "
        + roundToDecimal(100 * parDensityEquilibriumTolerance, 2) + "%), parDensityRef " + String.format("%.3e", parDensityRef));   // TEST
finishSimulation();
return true;
