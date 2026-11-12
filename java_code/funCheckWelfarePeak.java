// Function in Main. Returns value, type: boolean. No arguments. Called by funRunCheckpoint at each density equilibrium.
// Returns true once citizen welfare has passed its peak, i.e. further investment is making citizens worse off.
//
// Measure: mean over all citizens of varExpectedWelfare (logsum stored on each citizen's last turn, rand per month).
// After an investment, the next check only comes once density has settled again (at least parDensityEquilibriumWindow
// half-round checkpoints later), so every citizen has had a turn since the investment. On the loaded path the values come
// from savedState.txt (funLoadState), so the first check matches the straight-through run.
//
// Peak rule: the best mean so far is kept in varBestMeanWelfare.
//   - New best: store it, return false (keep investing).
//   - More than parWelfarePeakMargin below the best: the peak has passed, return true (stop).
//   - Otherwise (within the margin): treated as noise, return false (keep investing).
// The first check (no investment yet) always sets the first best, so the no-investment city is the baseline.
// Uses Main variable varBestMeanWelfare (double, initial value Double.NEGATIVE_INFINITY)
// and Main parameter parWelfarePeakMargin (double, rand per month, e.g. 20).

double sum = 0;
for (Citizen c : citizens) sum += c.varExpectedWelfare;
double meanW = sum / citizens.size();

if (meanW > varBestMeanWelfare) {
    varBestMeanWelfare = meanW;
    funRuntimePrint("Mean citizen welfare " + roundToDecimal(meanW, 1) + " (new best) at turn " + varTurnCount);
    return false;
}
boolean peaked = meanW < varBestMeanWelfare - parWelfarePeakMargin;
funRuntimePrint("Mean citizen welfare " + roundToDecimal(meanW, 1) + " (best " + roundToDecimal(varBestMeanWelfare, 1)
        + ", margin " + parWelfarePeakMargin + ")" + (peaked ? ": peak passed" : ": within margin") + " at turn " + varTurnCount);
return peaked;
