// Function in Main. Just action (returns nothing). Arguments: a (Activity), to (GridPoint)
// Carries out an activity's move (the choice of where to move is made elsewhere):
//   1. Takes its crowding (weight parActivityCrowdingWeight) away from around its old location and adds it around the new one,
//      which also updates rent at every affected node (funUpdateNeighbourhoodDensity recomputes varRent there)
//   2. Updates its location and moves its icon
// Does not check occupancy: the caller must only pick a node without another activity.
GridPoint from = a.varLocation;
if (to == from) return;   // staying put: nothing to change
//funUpdateNeighbourhoodDensity was built to update crowding (and nearby land) and rent for all nodes within the cutoff box around the central node (first argument) who changes by dCrowding and dLand in the second and third arguments. So what we are doing below is first updating the crowding and rent for all nodes around and including the one the agent leaves, and then doing the same around and including the node the agent arrives at.
funUpdateNeighbourhoodDensity(from, -parActivityCrowdingWeight, 0);
funUpdateNeighbourhoodDensity(to, parActivityCrowdingWeight, 0);
a.varLocation = to; //updates the internal location variable
a.setXY(to.getX(), to.getY()); //updates the location in presentation
