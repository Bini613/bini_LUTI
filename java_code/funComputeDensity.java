// Function in Main. Returns value, type: double. Arguments: candidate (GridPoint), home (GridPoint), ownWeight (double)
// Density at candidate (agents nearby per m^2 of usable land nearby). 
//Candidate and home are given as separate nodes, because we might want to exclude the contribution towards density of the home node when looking at a prospective candidate.
// Why exclude itself: the stored rent (varRent) counts the agent's own crowding, but unevenly across its options:
//   - at its home, its full weight is counted, so staying looks pricier than it really is to this agent
//   - at nearby candidates, part of its weight is counted (through the distance kernel), so they look somewhat pricier
//   - at distant candidates (beyond the cutoff), none of it is counted
// Left uncorrected, this bias falls from home outwards, so staying always looks worst and distant moves best,
// purely because of the agent's own presence: a false push to move away, causing excess and jumpy relocation.
// Removing its own contribution everywhere judges every option, staying included, as the city without this agent.
// One agent from a node without a large number of agents is a large share of its neighbourhood's crowding, so the bias is not negligible in this model.
//We do that exclusion by passing in argument ownWeight, which represents the agent in the home node who is making the evaluation (1 for citizens, parActivityCrowdingWeight for activities). This function will then return a density whose home's influence is subtracted from the candidate's inclusive crowding (set in funBuildDensitySurface and updated upon relocations in funUpdateNeighbourhoodDensity).
//But this function can also be used to find the inclusive density at the candidate (i.e. without excluding home's influence), by passing ownWeight = 0. The original inclusive crowding values will be used at the candidate
//We can also find density at one's own node by just passing the same node for candidate and home. Again, by passing ownWeight = 0, the inclusive density is returned, otherwise passing 1 or parActivityCrowdingWeight gives the exclusive density.
//   ownShare = ownWeight * exp(-d / r) if d <= cutoff, else 0   (d = distance home -> candidate; same kernel and cutoff as funUpdateNeighbourhoodDensity)
//   density  = (varCrowding[candidate] - ownShare) / varNearbyLand[candidate]

if (candidate.varNearbyLand <= 0) error("funComputeDensity: node (" + candidate.parXIndex + "," + candidate.parYIndex + ") has no nearby land (check funBuildDensitySurface and parMinAvailableLand)");

double ownShare = 0;
if (ownWeight != 0) {   // skip the distance calculation when nothing is removed
    double cutoff = parRentInfluenceDecayLength_CutOffMultiple * parRentInfluenceDecayLength;   // metres
    double d = sqrt(pow(candidate.parXIndex - home.parXIndex, 2) + pow(candidate.parYIndex - home.parYIndex, 2)) * parStepLength;
    if (d <= cutoff) ownShare = ownWeight * exp(-d / parRentInfluenceDecayLength);   // at candidate == home, d = 0 so the full weight is removed. Original crowding computation used the same cutoff, so outside of it, candidate was never factored in anyway, and doesn't need to be removed even if you were following a self-exclusion approach (by setting ownWeight != 0)
}
return max(0, candidate.varCrowding - ownShare) / candidate.varNearbyLand;   // max(0, ...) guards against tiny negative rounding leftovers
