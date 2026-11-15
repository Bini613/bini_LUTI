// Function in Main. Just action (returns nothing). No arguments.
// Empties the fastest-path tree cache (also creates it the first time).
// The cache holds, per grid node (index parIndex), the Dijkstra tree from that node to every node: varTreeTime, varTreeDist,
// varTreePrev, varTreeOrder (Main variables: double[][], double[][], Link[][], int[][], each indexed [node][...]; null = no tree).
// A tree is only valid for the link travel times it was built on, so the cache must be cleared whenever link times change:
//   - funAssignTraffic, after every step (the step updates all link times)
//   - funApplyImpedanceChange (investment changes a link's time)
//   - funStoreTree, when the cache holds parTreeCacheMax trees (memory limit)
// Users: funComputeTargetFlows (activity nodes, during assignment) and funEvaluateAndMoveActivity (candidate nodes).
// Between assignments link times don't change, so trees built by relocation choices are reused by later choices and by the
// next assignment's first pass, and the trees from an assignment's final pass are reused by the choices after it.
if (varTreeTime != null && varTreeTime.length == parCols * parCols && varTreeCount == 0) return;   // already empty. First two conditions check if cache already exists in correct shape, otherwise the present function is still needed to build a new empty cache with the correct shape.
int nNodes = parCols * parCols;
varTreeTime = new double[nNodes][];
varTreeDist = new double[nNodes][];
varTreePrev = new Link[nNodes][];
varTreeOrder = new int[nNodes][];
varTreeCount = 0;   // Main variable (int): number of trees currently cached
