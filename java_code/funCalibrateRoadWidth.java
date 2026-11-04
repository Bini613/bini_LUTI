// Function in Main. Just action (returns nothing). No arguments.
// Run once at startup, after funInitImpedance (impedances and link lengths must be set), and before funSetAvailableLands.
// Sets varRoadWidthRef, the effective width of a road with impedance I = 1, so that before any investment
// roads take parStartingRoadLandShare of all land.
// A link's land is divided by its impedance, so a road with I = 1 has width exactly w_ref, and a road with impedance I has width w_ref / I.
// w_ref is therefore the local road width only while parImpedanceLocal = 1; otherwise local roads are w_ref / parImpedanceLocal wide.
//   Land consumed by each link:   w_ref * length[l] / I[l]   (split half to each end node)
//   All links:         w_ref * sum over links of (length[l] / I[l])  =  share * N * L0
//   Solving:           w_ref = share * N * L0 / sum over links of (length[l] / I[l])
// (N = number of nodes, L0 = parCellLand, the land in one node's cell)

double sumLengthOverI = 0;
for (Link l : links) sumLengthOverI += l.parLength / l.varImpedance;
if (sumLengthOverI <= 0) error("Road width calibration failed: links or their poperties are missing");

varRoadWidthRef = parStartingRoadLandShare * gridPoints.size() * parCellLand / sumLengthOverI;
