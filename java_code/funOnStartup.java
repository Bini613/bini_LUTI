// Function in Main. Just action (returns nothing). No arguments. The whole of Main's On startup: call it as its only line
// (test calls can follow it). Runs the startup in this order:
//   1. Run log (funStartRunLog first, so everything after is recorded).
//   2. Network: reseed with parSeed, then load savedNetwork.txt (if parLoadSavedNetwork) or build a new one.
//   3. Reseed with parSeed + 1, so the rest doesn't depend on how many random numbers the network step used.
//   4. City, by one of two paths:
//      - Loaded path (network loaded from file, parLoadSavedState true, and savedState.txt usable): funLoadState recreates the
//        saved equilibrium (agents, preferences, link impedances and flows) and reseeds with parSeed + 2.
//      - Virgin path: starting impedances are drawn (funInitImpedance) and agents placed with fresh preferences (funPlaceAgents).
//   5. Both paths then build land, density, traffic and the relocation queue the same way. None of these draw random numbers
//      before the queue shuffle, so the reseed inside funLoadState comes just before the shuffle in effect.
//   6. On the loaded path the city is already settled, so the first checkpoint runs straight away (invest, or end the run)
//      before any turn happens.
funStartRunLog();

getDefaultRandomGenerator().setSeed(parSeed);                // seeds the network build
varNetworkLoaded = parLoadSavedNetwork && funLoadNetwork();   // try the saved network, if switched on
if (!varNetworkLoaded) funBuildNetwork();                    // otherwise build (and save) a new one

getDefaultRandomGenerator().setSeed(parSeed + 1);   // makes sure all steps going forward have a starting point independent of the network step

boolean stateLoaded = varNetworkLoaded && parLoadSavedState && funLoadState();   // includes the reseed to parSeed + 2
if (!stateLoaded) {
    funInitImpedance();   // needed before rent calculations below because it affects road width -> available land area
    funPlaceAgents();     // citizens and activities, with their preferences
}

funCalibrateRoadWidth();            // sets varRoadWidthRef so that starting widths take up parStartingRoadLandShare of city area
funSetAvailableLands();             // sets available land value for every grid point, depending on the roads around it
funBuildDensitySurface();           // sets varCrowding and varNearbyLand for each node
funAssignTraffic();                 // first equilibrium traffic flows and volumes
funShuffleResetRelocationQueue();   // new shuffled relocation queue

if (stateLoaded) funRunCheckpoint();   // loaded city is already settled: invest (or end) before any turn
