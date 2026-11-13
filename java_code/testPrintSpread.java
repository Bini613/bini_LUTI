// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments. Called by funPrintFinalReports.
// Prints the citizens' current spread (funComputeSpread: standard distance of their homes from their centroid, metres),
// and, if the equilibrium history has values, the mean of the last parSpreadEquilibriumWindow of them for comparison.
double spread = funComputeSpread();
String s = "Turn " + varTurnCount + " citizens' spread from their centroid: " + roundToDecimal(spread, 0) + " m";
int n = colAverageSpread.size();
if (n > 0) {
    int window = min(parSpreadEquilibriumWindow, n);
    double sum = 0;
    for (int i = n - window; i < n; i++) sum += colAverageSpread.get(i);
    s += " (mean of last " + window + " equilibrium reports " + roundToDecimal(sum / window, 0) + " m)";
}
funRuntimePrint(s);   // TEST
