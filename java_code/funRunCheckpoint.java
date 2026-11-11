// Function in Main. Just action (returns nothing). No arguments. Called by funSimulateNextTurn at appropriate intervals, and on start up when loading a saved state.
// At each checkpoint: if density has not settled yet, nothing happens. Once it has (funCheckDensityEquilibrium):
//   - The first equilibrium is saved to savedState.txt (funSaveState), if the road network was loaded from file
//     (varNetworkLoaded) and this state hasn't been saved or loaded already (varStateSaved). It is then followed by
//     funResumeFromEquilibrium, the same restart a run loading this state goes through, so both continue identically.
//   - funInvest tries to invest. If it did, traffic is re-assigned on the upgraded network (so access times reflect it
//     straight away) and the density history is cleared, so the next equilibrium is judged only on post-investment reports.
//   - If nothing was invested (budget used up, nothing allowed, or placeholder), the run ends: final reports, then stop.
// After a saved state is loaded (varAtLoadedEquilibrium), the first checkpoint counts as settled without checking,
// since the loaded city is already at equilibrium.
if (varInitialising) return;   // initialisation phase (funSimulateNextTurn): no density history, investment or stopping yet.
boolean settled;
if (varAtLoadedEquilibrium) {
    settled = true;
    varAtLoadedEquilibrium = false;   // only the first checkpoint after loading will run this if block
    funRuntimePrint("Starting from the loaded equilibrium at turn " + varTurnCount);
}
else settled = funCheckDensityEquilibrium();
if (!settled) return;   // still settling

if (varNetworkLoaded && !varStateSaved) {
    funSaveState();
    varStateSaved = true;
    funResumeFromEquilibrium();   // same restart as a run that loads this state, so both continue identically from here
}

if (funInvest()) {
    funAssignTraffic();
    colAverageDensity.clear();
    funRuntimePrint("Investment made at turn " + varTurnCount + " (round " + varRounds + "); waiting for the next equilibrium");
    return;
}

funPrintFinalReports("density equilibrium, no further investment");
varShowGraphics = true;   // show the final graphics only
finishSimulation();
