// Function in Main. Just action (returns nothing). No arguments. Called by funSimulateNextTurn at appropriate intervals, and on start up when loading a saved state.
// At each checkpoint: 
//   - Terminate simulation if maximum rounds parMaxRounds have happened. Otherwise:
//   - Check if spread has reached equilibrium, return if it hasn't. Otherwise:
//   - The first equilibrium is saved to savedState.txt (funSaveState), if this state hasn't been saved or loaded already
//     (varStateSaved). The network is always on file (loaded from, or just saved to, savedNetwork.txt). It is then followed by
//     funResumeFromEquilibrium, the same restart a run loading this state goes through, so both continue identically.
//   - Traffic is assigned, and then we check if welfare has passed its peak, and either finish simulation if it has, or run an investment if it has not.
//     If funInvest finds no link it can upgrade, the run ends (no welfare peak reached).
if (varInitialising) return;   // initialisation phase (funSimulateNextTurn): no spread history, investment or stopping yet.
boolean settled;
if (varAtLoadedEquilibrium) { // For runs loading from saved state file
    settled = true;
    varAtLoadedEquilibrium = false;   // only the first checkpoint after loading will run this if block
    funRuntimePrint("Starting from the loaded equilibrium at turn " + varTurnCount);
}
else settled = funCheckSpreadEquilibrium();

if (varTurnCount >= parMaxRounds * (parNumCitizens + parNumActivities)) {
    funPrintFinalReports("max rounds reached");
    varShowGraphics = true;
    finishSimulation();
    return;
}
if (!settled) return;   // still settling

if (!varStateSaved) {
    funSaveState();
    varStateSaved = true;
    funResumeFromEquilibrium();   // same restart as a run that loads this state, so both continue identically from here
}

// both loaded and through-run paths: fresh flows + access times for the settled city. 
//Future checkpoints coming from either also need this, because they will either be ending the simulation and need final data, or will be performing an investment.
funAssignTraffic();                       
varAssignmentCount++;
varRelocationsSinceAssignment = 0;
if (funCheckWelfarePeak()) {
    funPrintFinalReports("Welfare peaked, no further investment");
    varShowGraphics = true;   // show the final graphics only
    finishSimulation();
}
//If no welfare peak, then invest, and wait for next spread equilibrium to check welfare peak or invest again.
else if (!funInvest()) {   // no link could be upgraded: nothing more to do
    funPrintFinalReports("No upgradable links left before a welfare peak was reached");
    varShowGraphics = true;
    finishSimulation();
}
else {
    funAssignTraffic(); // Assign according to upgraded network.
    varAssignmentCount++;
    varRelocationsSinceAssignment = 0;
    colAverageSpread.clear();
}
