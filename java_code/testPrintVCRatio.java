// TEMPORARY TEST. Function in Main. Just action (returns nothing). No arguments. Called by funPrintFinalReports.
// Volume / capacity (V/C) over the network, from each link's current equilibrium flow (varFlowVol) and capacity (varCapacity):
// Links with no flow are left out entirely.
//   simple average over used links (each used link counts equally),
//   flow-weighted average (sum of V^2/C over sum of V: the V/C a typical trip experiences, so busy links count more),
//   maximum, and the share of links over capacity (V/C > 1).
double sumVC = 0, sumFlow = 0, sumFlowVC = 0, maxVC = 0;
int count = 0, overCapacity = 0;
for (Link l : links) {
    if (l.varCapacity <= 0) continue;   // safety: capacity not set
    if (l.varFlowVol <= 0) continue;    // empty link: left out
    double vc = l.varFlowVol / l.varCapacity;
    sumVC += vc;
    sumFlow += l.varFlowVol;
    sumFlowVC += l.varFlowVol * vc;
    maxVC = max(maxVC, vc);
    if (vc > 1) overCapacity++;
    count++;
}
if (count == 0) {
    funRuntimePrint("V/C report: no used links");
    return;
}
funRuntimePrint("Turn " + varTurnCount + " V/C over " + count + " used links (of " + links.size() + "): average " + roundToDecimal(sumVC / count, 3)
        + ", flow-weighted average " + roundToDecimal(sumFlow > 0 ? sumFlowVC / sumFlow : 0, 3)
        + ", max " + roundToDecimal(maxVC, 3) + ", links over capacity " + overCapacity
        + " (" + roundToDecimal(100.0 * overCapacity / count, 1) + "%)");   // TEST
