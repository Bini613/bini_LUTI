// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint.
// Equilibrium check on the citizens' spread: the standard distance of their homes from the citizens' mean position (centroid),
//   spread = sqrt( (1/N) * sum over citizens of ((x - x_mean)^2 + (y - y_mean)^2) )   (metres; the 2D standard deviation of positions)
// A compact city has a small spread; expansion raises it and contraction lowers it.
// Every report's spread is appended to colAverageSpread (Collection in Main: ArrayList of Double), a growing history.
// The current spread is compared with the mean of the previous parSpreadEquilibriumWindow reports:
//   relative change = |current - mean of last N| / mean of last N
// Comparing with several past reports, rather than just the last one, catches slow steady trends that look flat from one report to the next.
// If the change is within parSpreadEquilibriumTolerance (e.g. 0.05 = 5%), prints the spread and returns true; otherwise returns false.
// What happens at equilibrium (invest or end the run) is decided by the caller, funRunCheckpoint.
// Until there are parSpreadEquilibriumWindow earlier reports to compare with, it only stores the spread.
// The caller clears colAverageSpread after an investment, so the next equilibrium is judged only on post-investment reports.

// Centroid of the citizens' homes (grid steps), then the mean squared distance from it
double sumX = 0, sumY = 0;
for (Citizen c : citizens) {
    sumX += c.varHome.parXIndex;
    sumY += c.varHome.parYIndex;
}
int nCit = citizens.size();
double meanX = sumX / nCit;
double meanY = sumY / nCit;
double sumSq = 0;
for (Citizen c : citizens){
   double dx = c.varHome.parXIndex - meanX;
   double dy = c.varHome.parYIndex - meanY;
   sumSq += dx * dx + dy * dy;
} 
double current = sqrt(sumSq / nCit) * parStepLength;   // spread in metres

int n = colAverageSpread.size();          // earlier reports stored so far
int window = parSpreadEquilibriumWindow;
colAverageSpread.add(current);            // stored for later reports' comparisons
if (n < window) return false;              // not enough history yet

double sumPast = 0;
for (int i = n - window; i < n; i++) sumPast += colAverageSpread.get(i);   // the last 'window' reports before this one
double pastMean = sumPast / window;
double change = abs(current - pastMean) / pastMean;
if (change > parSpreadEquilibriumTolerance) return false;   // still changing: keep running

funRuntimePrint("Spread equilibrium reached at turn " + varTurnCount + " (round " + varRounds + "): citizens' spread from their centroid "
        + roundToDecimal(current, 0) + " m, change vs mean of last " + window + " reports " + roundToDecimal(100 * change, 5) + "% (tolerance "
        + roundToDecimal(100 * parSpreadEquilibriumTolerance, 5) + "%)");
return true;
