// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments. Called by funPrintFinalReports.
// Prints the citizens' final spread (funComputeSpread: standard distance of their homes from their centroid, metres)
// and its % change from the first equilibrium (varFirstEquilibriumSpread, recorded once by funResumeFromEquilibrium).
// Final spread = mean of the last parSpreadEquilibriumWindow values in colAverageSpread (the reports since the last
// investment), which smooths out turn-to-turn noise; if that history is empty, the current spread is used instead.
// The % change is stored in varSpreadChangePercent (Main variable, double, initial value 0), for experiments to read after the run.
// It stays 0 if no equilibrium was reached (no baseline).
double current = funComputeSpread();
double finalSpread = current;
String source = "most recent reading";
int n = colAverageSpread.size();
if (n > 0) {
    int window = min(parSpreadEquilibriumWindow, n);
    double sum = 0;
    for (int i = n - window; i < n; i++) sum += colAverageSpread.get(i);
    finalSpread = sum / window;
    source = "mean reading of last " + window + " reports";
}
String s = "Turn " + varTurnCount + " citizens' spread from their centroid: " + roundToDecimal(finalSpread, 0) + " m (source: " + source + ")";
if (varFirstEquilibriumSpread > 0) {
    varSpreadChangePercent = 100 * (finalSpread - varFirstEquilibriumSpread) / varFirstEquilibriumSpread;   // Main variable (double): kept for experiments to read after the run
    s += ", first equilibrium " + roundToDecimal(varFirstEquilibriumSpread, 0) + " m, change " + (varSpreadChangePercent >= 0 ? "+" : "") + roundToDecimal(varSpreadChangePercent, 1) + "%";
}
else s += " (no equilibrium reached, so no baseline to compare with)";
funRuntimePrint(s);   // TEST
