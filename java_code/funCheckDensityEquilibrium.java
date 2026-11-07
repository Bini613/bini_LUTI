// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint (cyclic event evCheckpoint).
// Equilibrium check on average density over occupied nodes (nodes with at least one citizen or activity).
// Every report's average is appended to colAverageDensity (Collection in Main: ArrayList of Double), a growing history.
// The current average is compared with the mean of the previous parDensityEquilibriumWindow reports:
//   relative change = |current - mean of last N| / mean of last N
// Comparing with several past reports, rather than just the last one, catches slow steady trends that look flat from one report to the next.
// If the change is within parDensityEquilibriumTolerance (e.g. 0.05 = 5%), prints the density and returns true; otherwise returns false.
// What happens at equilibrium (invest or end the run) is decided by the caller, funRunCheckpoint.
// Until there are parDensityEquilibriumWindow earlier reports to compare with, it only stores the average.
// The caller clears colAverageDensity after an investment, so the next equilibrium is judged only on post-investment reports.

double sum = 0;
int count = 0;
for (GridPoint g : gridPoints) {
    if (g.colResidents.isEmpty() && g.colActivities.isEmpty()) continue;   // unoccupied node
    sum += funComputeDensity(g, g, 0);   // ownWeight 0: everyone included
    count++;
}
double current = sum / count;

int n = colAverageDensity.size();          // earlier reports stored so far
int window = parDensityEquilibriumWindow;
colAverageDensity.add(current);            // stored for later reports' comparisons
if (n < window) return false;              // not enough history yet

double sumPast = 0;
for (int i = n - window; i < n; i++) sumPast += colAverageDensity.get(i);   // the last 'window' reports before this one
double pastMean = sumPast / window;
double change = abs(current - pastMean) / pastMean;
if (change > parDensityEquilibriumTolerance) return false;   // still changing: keep running

funRuntimePrint("Density equilibrium reached at turn " + varTurnCount + " (round " + varRounds + "): average occupied-node density "
        + String.format("%.3e", current) + " agents/m^2, change vs mean of last " + window + " reports " + roundToDecimal(100 * change, 5) + "% (tolerance "
        + roundToDecimal(100 * parDensityEquilibriumTolerance, 5) + "%), parDensityRef " + String.format("%.3e", parDensityRef));
return true;
