// Function in Main. Just action (returns nothing). No arguments. The whole of Main's On startup: call it as its only line
// (test calls can follow it, but they only run on the virgin path, which doesn't return early). Runs the startup in this order:
//   1. Run log (funStartRunLog first, so everything after is recorded).
//   2. Network: reseed with parSeed, then load savedNetwork.txt (if parLoadSavedNetwork) or build a new one.
//   3. Reseed with parSeed + 1, so the rest doesn't depend on how many random numbers the network step used.
//   4. City, by one of two paths:
//      - Loaded path (network loaded from file, parLoadSavedState true, and savedState.txt usable): funLoadState recreates the
//        saved equilibrium (agents, preferences, link impedances and flows). After the land setup, funResumeFromEquilibrium
//        (the same function the straight-through run calls at its first equilibrium) reseeds with parSeed + 2, rebuilds
//        density and traffic, reshuffles the queue and resets the counters. The first checkpoint then runs straight away
//        (invest, or end the run) before any turn happens.
//      - Virgin path: starting impedances are drawn (funInitImpedance), agents placed with fresh preferences (funPlaceAgents),
//        then land, density, traffic and the relocation queue are built.
funStartRunLog();

getDefaultRandomGenerator().setSeed(parSeed);                // seeds the network build
varNetworkLoaded = parLoadSavedNetwork && funLoadNetwork();   // try the saved network, if switched on
if (!varNetworkLoaded) funBuildNetwork();                    // otherwise build (and save) a new one

getDefaultRandomGenerator().setSeed(parSeed + 1);   // makes sure all steps going forward have a starting point independent of the network step

// Loaded path: start from the saved equilibrium, then go straight to the first checkpoint
if (varNetworkLoaded && parLoadSavedState && funLoadState()) {
    funCalibrateRoadWidth();      // sets varRoadWidthRef so that starting widths take up parStartingRoadLandShare of city area
    funSetAvailableLands();       // sets available land value for every grid point, depending on the roads around it
    funResumeFromEquilibrium();   // reseed, rebuild density, assign traffic, reshuffle queue, reset counters (same as the straight-through run at its first equilibrium)
    funRunCheckpoint();           // loaded city is already settled: invest (or end) before any turn
    return;
}

// Virgin path
funInitImpedance();                 // needed before rent calculations below because it affects road width -> available land area
funPlaceAgents();                   // citizens and activities, with their preferences
funCalibrateRoadWidth();            // sets varRoadWidthRef so that starting widths take up parStartingRoadLandShare of city area
funSetAvailableLands();             // sets available land value for every grid point, depending on the roads around it
funBuildDensitySurface();           // sets varCrowding and varNearbyLand for each node
funAssignTraffic();                 // first equilibrium traffic flows and volumes
funShuffleResetRelocationQueue();   // new shuffled relocation queue
