// Function in Main. Just action (returns nothing). Arguments: c (Citizen), to (GridPoint)
// Carries out a citizen's move (the choice of where to move is made elsewhere):
//   1. Takes its crowding (weight 1) away from around its old home and adds it around the new one,
//      which also updates rent at every affected node (funUpdateNeighbourhoodDensity recomputes varRent there)
//   2. Updates its home and moves its icon
// Does not check occupancy: the caller must only pick a node without another citizen.
GridPoint from = c.varHome;
if (to == from) return;   // staying put: nothing to change
funUpdateNeighbourhoodDensity(from, -1, 0);
funUpdateNeighbourhoodDensity(to, 1, 0);
from.varResident = null;   // GridPoint variable (type Citizen): who lives at a node, null if nobody. In this line, it is cleared once citizen moves out
to.varResident = c; //new location records citizen as new resident
c.varHome = to;
c.setXY(to.getX(), to.getY());
