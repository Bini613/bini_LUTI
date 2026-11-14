// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint.
// Equilibrium check on the citizens' spread: the standard distance of their homes from the citizens' mean position (centroid),
// calculated by funComputeSpread (metres; the 2D standard deviation of positions).
// Every report's spread is appended to colAverageSpread (Collection in Main: ArrayList of Double), a growing history.
// The mean of the last N = parSpreadEquilibriumWindow reports (this one included) is compared with the mean of the N reports before them:
//   relative change = |recent mean - previous mean| / previous mean
// Averaging both sides smooths out report-to-report noise, while comparing two consecutive windows still catches
// a slow steady trend (the recent window's mean sits above or below the previous one's until the spread plateaus).
// If the change is within parSpreadEquilibriumTolerance (e.g. 0.05 = 5%), prints the spread and returns true; otherwise returns false.
// What happens at equilibrium (invest or end the run) is decided by the caller, funRunCheckpoint.
// Until there are 2N reports in the history, it only stores the spread.
// The caller clears colAverageSpread after an investment, so the next equilibrium is judged only on post-investment reports.

double current = funComputeSpread();   // spread in metres

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

String s = "Spread equilibrium reached at turn " + varTurnCount + " (round " + varRounds + "): citizens' spread from their centroid "
        + roundToDecimal(recentMean, 0) + " m (mean of last " + window + " reports), change vs the " + window + " reports before: " + roundToDecimal(100 * change, 5) + "% (tolerance "
        + roundToDecimal(100 * parSpreadEquilibriumTolerance, 5) + "%)";
// % change vs the first equilibrium (varFirstEquilibriumSpread, set by funResumeFromEquilibrium; still 0 at the first equilibrium itself)
if (varFirstEquilibriumSpread > 0) {
    double changeFromFirst = 100 * (recentMean - varFirstEquilibriumSpread) / varFirstEquilibriumSpread;
    s += ", change vs first equilibrium (" + roundToDecimal(varFirstEquilibriumSpread, 0) + " m): " + (changeFromFirst >= 0 ? "+" : "") + roundToDecimal(changeFromFirst, 1) + "%";
}
funRuntimePrint(s);
return true;
