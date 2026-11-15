// Function in Main. Just action (returns nothing). Arguments: k (int), time (double[]), dist (double[]), prev (Link[]), order (int[])
// Stores a freshly built fastest-path tree (from funBuildFastestPathTree) in the cache slot of grid node k (its parIndex).
// Does nothing if node k already has a tree (e.g. two activities on the same node both built one).
// Memory limit: each tree is roughly 0.25 MB on a 100 x 100 grid, so when the cache already holds parTreeCacheMax trees
// (Main parameter, int) it is cleared first. Trees are then simply rebuilt as needed.
// Call only from the main thread (not from inside the parallel passes).
// The arrays are stored as they are (not copied), so they must never be changed afterwards.
if (varTreeTime == null || varTreeTime.length != parCols * parCols) funClearTreeCache();   // This checks if cache doesn't exist, or has wrong shape, and if so, clears and builds new one correctly.
if (varTreeTime[k] != null) return;   // already cached (important for parrallel threads in funComputeTargetFlows, where two activities can build the same tree simultaneously: this condition prevents the second one from pointlessly overwriting the first)
if (varTreeCount >= parTreeCacheMax) funClearTreeCache();   // memory limit reached: start again
varTreeTime[k] = time;
varTreeDist[k] = dist;
varTreePrev[k] = prev;
varTreeOrder[k] = order;
varTreeCount++;
