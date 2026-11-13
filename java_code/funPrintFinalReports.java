// Function in Main. Just action (returns nothing). Arguments: reason (String)
// Prints why the run is ending, then the end-of-run reports. Does not stop the simulation: the caller calls finishSimulation() afterwards.
// Called from every stop point (equilibrium with no investment made in funRunCheckpoint, parMaxRounds reached in funSimulateNextTurn),
// so every run ends with the same reports whichever condition stops it. Add or remove end-of-run reports here.
// Also called from Main's On destroy (Stop button), guarded by varReportPrinted so the reports never print twice:
//   if (!varReportPrinted) funPrintFinalReports("stopped manually");
funRuntimePrint("Run ended at turn " + varTurnCount + " (round " + varRounds + "): " + reason);
testPrintUtilityShares();
testPrintVCRatio();
testPrintSpread();
varReportPrinted = true;   // Main boolean: stops On destroy printing the reports again
