// Function in Main. Just action (returns nothing). No arguments.
// Puts the model into a standard "resume" state at the first density equilibrium, so a run that loads the saved state and
// a run that went straight through continue identically from here. Called from two places:
//   - Main on startup, on the loaded path (after funLoadState and the land setup), before the first checkpoint.
//   - funRunCheckpoint, in the straight-through run, right after funSaveState at its first equilibrium.
// Both paths then hold the same agents, preferences, links and land; this function makes everything else match:
//   1. Reseed with parSeed + 2: both continue on the same random sequence.
//   2. Rebuild the density surface from scratch: the straight-through run's surface was built up move by move, and its
//      rounding differs slightly from a fresh build, which could flip a later choice.
//   3. Reshuffle the relocation queue (draws from the new seed).
//   4. Reset the turn and round counters, the moves-since-assignment counter and the density history, so checkpoints,
//      re-assignments and rounds fall on the same turns in both runs.
//   Traffic reassignment is necessary so that loaded runs and through-runs resume from same flow data (so the through-run doesn't use flow data that may be a few relocations old). That assignment takes place in the checkpoint function that calls the present function. 
// Step 2 draws no random numbers, so the reseed only has to come before the shuffle.
getDefaultRandomGenerator().setSeed(parSeed + 2);
funBuildDensitySurface();
funShuffleResetRelocationQueue();
varTurnCount = 0;
varRounds = 0;
varRelocationsSinceAssignment = 0;
varAssignmentCount = 0;
colAverageSpread.clear();
funRuntimePrint("Resumed from equilibrium: reseeded with parSeed + 2, density and queue rebuilt, counters reset");
