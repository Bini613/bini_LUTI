// TEMPORARY TEST. Paste at the bottom of Main's On startup (after funCalibrateDensity).
// For citizen 0, compares funComputeRentExcludingSelf with the "real" answer: actually removing the citizen's crowding,
// reading the rents, then putting it back. Checked at its home and at one neighbouring node. Each pair should match.
Citizen c0 = citizens.get(0);
GridPoint home = c0.varHome;
int nbrX = (home.parXIndex + 1 < parCols) ? home.parXIndex + 1 : home.parXIndex - 1;   // one step to the right, or to the left if home is on the right edge
GridPoint nbr = varGrid[nbrX][home.parYIndex];

double computedHome = funComputeRentExcludingSelf(home, home, 1); //last argument is weight of agent who would move. Citizen's weight is the number of citizens moving (1), activities aren't looked at here, but have weight parActivityCrowingWeight
double computedNbr = funComputeRentExcludingSelf(nbr, home, 1);
double storedHome = home.varRent;
double storedNbr = nbr.varRent;

funUpdateNeighbourhoodDensity(home, -1, 0);   // actually remove citizen 0's crowding and updates rent at that point and all points within the cutoff
double actualHome = home.varRent;
double actualNbr = nbr.varRent;
funUpdateNeighbourhoodDensity(home, 1, 0);    // put it back

traceln("Rent excluding self at home: computed " + roundToDecimal(computedHome, 1) + ", actual " + roundToDecimal(actualHome, 1)
        + " (stored rent with self: " + roundToDecimal(storedHome, 1) + ")");   // TEST: computed and actual should match, both below stored
traceln("Rent excluding self at neighbour: computed " + roundToDecimal(computedNbr, 1) + ", actual " + roundToDecimal(actualNbr, 1) + " (stored rent with self: " + roundToDecimal(storedNbr, 1) + ")");   // TEST: should match
