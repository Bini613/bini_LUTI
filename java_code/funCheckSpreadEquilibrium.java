// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint.
// Equilibrium check on the citizens' spread: the standard distance of their homes from the citizens' mean position (centroid),
//   spread = sqrt( (1/N) * sum over citizens of ((x - x_mean)^2 + (y - y_mean)^2) )   (metres; the 2D standard deviation of positions)
// A compact city has a small spread; expansion raises it and contraction lowers it.
// Every report's spread is appended to colAverageSpread (Collection in Main: ArrayList of Double), a growing history.
// The mean of the last N = parSpreadEquilibriumWindow reports (this one included) is compared with the mean of the N reports before them:
//   relative change = |recent mean - previous mean| / previous mean
// Averaging both sides smooths out report-to-report noise, while comparing two consecutive windows still catches
// a slow steady trend (the recent window's mean sits above or below the previous one's until the spread plateaus).
// If the change is within parSpreadEquilibriumTolerance (e.g. 0.05 = 5%), prints the spread and returns true; otherwise returns false.
// What happens at equilibrium (invest or end the run) is decided by the caller, funRunCheckpoint.
// Until there are 2N reports in the history, it only stores the spread.
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

colAverageSpread.add(current);             // stored for this and later reports' comparisons
int window = parSpreadEquilibriumWindow;
int n = colAverageSpread.size();
if (n < 2 * window) return false;          // not enough history yet for two full windows

double sumRecent = 0, sumPrevious = 0;
for (int i = n - window; i < n; i++) sumRecent += colAverageSpread.get(i);                    // last 'window' reports, this one included
for (int i = n - 2 * window; i < n - window; i++) sumPrevious += colAverageSpread.get(i);     // the 'window' reports before those
double recentMean = sumRecent / window;
double previousMean = sumPrevious / window;
double change = abs(recentMean - previousMean) / previousMean;
if (change > parSpreadEquilibriumTolerance) return false;   // still changing: keep running

funRuntimePrint("Spread equilibrium reached at turn " + varTurnCount + " (round " + varRounds + "): citizens' spread from their centroid "
        + roundToDecimal(recentMean, 0) + " m (mean of last " + window + " reports), change vs the " + window + " reports before: " + roundToDecimal(100 * change, 5) + "% (tolerance "
        + roundToDecimal(100 * parSpreadEquilibriumTolerance, 5) + "%)");
return true;
