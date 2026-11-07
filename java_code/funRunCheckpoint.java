// Function in Main. Just action (returns nothing). No arguments. Called by the cyclic event evCheckpoint.
// At each checkpoint: if density has not settled yet, nothing happens. Once it has (funCheckDensityEquilibrium):
//   - funInvest tries to invest. If it did, traffic is re-assigned on the upgraded network (so access times reflect it
//     straight away) and the density history is cleared, so the next equilibrium is judged only on post-investment reports.
//   - If nothing was invested (budget used up, nothing allowed, or placeholder), the run ends: final reports, then stop.
if (!funCheckDensityEquilibrium()) return;   // still settling

if (funInvest()) {
    funAssignTraffic();
    colAverageDensity.clear();
    funRuntimePrint("Investment made at turn " + varTurnCount + " (round " + varRounds + "); waiting for the next equilibrium");
    return;
}

funPrintFinalReports("density equilibrium, no further investment");
finishSimulation();
