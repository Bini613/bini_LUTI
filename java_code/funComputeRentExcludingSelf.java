// Function in Main. Returns value, type: double. Arguments: candidate (GridPoint), home (GridPoint), w (double)
// The rent an agent living at home, with crowding weight w (1 for a citizen, parActivityCrowdingWeight for an activity),
// would see at candidate if it weren't living anywhere. Used when the agent compares candidate nodes, including staying at home.
//
// Why exclude itself: the stored rent (varRent) counts the agent's own crowding, but unevenly across its options:
//   - at its home, its full weight is counted, so staying looks pricier than it really is to this agent
//   - at nearby candidates, part of its weight is counted (through the distance kernel), so they look somewhat pricier
//   - at distant candidates (beyond the cutoff), none of it is counted
// Left uncorrected, this bias falls from home outwards, so staying always looks worst and distant moves best,
// purely because of the agent's own presence: a false push to move away, causing excess and jumpy relocation.
// Removing its own contribution everywhere judges every option, staying included, as the city without this agent.
// One agent per node is a large share of its neighbourhood's crowding, so the bias is not negligible in this model.
//
// Removes the agent's own contribution to crowding at candidate, then applies the same formula as GridPoint's funUpdateRent:
//   ownShare = w * exp(-d / r) if d <= cutoff, else 0      (d = distance home -> candidate, same kernel and cutoff as funUpdateNeighbourhoodDensity)
//   density  = (varCrowding[candidate] - ownShare) / varNearbyLand[candidate]
//   R        = parRentMin + (parRentRef - parRentMin) * (density / varDensityRef)^parRentDensityElasticity
// Only reads stored values: never changes varCrowding or varRent.

// Stop the run if the rent surface isn't ready: this function should only ever be called after funBuildDensitySurface and funCalibrateDensity
if (varDensityRef <= 0) error("funComputeRentExcludingSelf called before varDensityRef was calibrated (run funCalibrateDensity first)");
if (candidate.varNearbyLand <= 0) error("funComputeRentExcludingSelf: node (" + candidate.parXIndex + "," + candidate.parYIndex + ") has no nearby land (check funBuildDensitySurface and parMinAvailableLand)");

double cutoff = parRentInfluenceDecayLength_CutOffMultiple * parRentInfluenceDecayLength;   // metres, as in funUpdateNeighbourhoodDensity
double d = sqrt(pow(candidate.parXIndex - home.parXIndex, 2) + pow(candidate.parYIndex - home.parYIndex, 2)) * parStepLength;
double ownShare = (d <= cutoff) ? w * exp(-d / parRentInfluenceDecayLength) : 0;   // at candidate == home, d = 0 so the full weight w is removed. Candidates beyond the cuttoff were never affected by present home anyway, so ownshare for them was 0.

double density = max(0, candidate.varCrowding - ownShare) / candidate.varNearbyLand;   // max(0, ...) guards against tiny negative rounding leftovers in smallest crowding scenario where varCrowding = ownShare .
return parRentMin + (parRentRef - parRentMin) * pow(density / varDensityRef, parRentDensityElasticity);
