// Function in Main. Just action (returns nothing). Arguments: c (Citizen), to (GridPoint)
// Carries out a citizen's move (the choice of where to move is made elsewhere):
//   1. Takes its crowding (weight 1) away from around its old home and adds it around the new one,
//      which also updates rent at every affected node (funUpdateNeighbourhoodDensity recomputes varRent there)
//   2. Updates its home and moves its icon
// Occupancy is not checked here. The caller decides whether multiple occupancy is permitted.
GridPoint from = c.varHome;
if (to == from) return;   // staying put: nothing to change
funUpdateNeighbourhoodDensity(from, -1, 0);
funUpdateNeighbourhoodDensity(to, 1, 0);
from.colResidents.remove(c);   // GridPoint collection (ArrayList of Citizen): everyone living at a node. Citizen leaves the old node's list...
to.colResidents.add(c);       // ...and joins the new node's list
c.varHome = to;
funPlaceIcon(c, to);   // icon near the new node (display only)
