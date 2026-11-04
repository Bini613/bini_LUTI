// Function in Main. Just action (returns nothing). No arguments.
// Run once at startup, after funInitImpedance (impedances and link lengths must be set), and before funSetAvailableLands.
// Sets varRoadWidthRef, the effective width of a road with impedance I = 1, so that before any investment
// roads take parStartingRoadLandShare of all land.
// A link's width is w_ref * I^(-delta), with delta = parRoadLandUsageExponent, so a road with I = 1 has width exactly w_ref.
// w_ref is therefore the local road width only while parImpedanceLocal = 1; otherwise local roads are w_ref * parImpedanceLocal^(-delta) wide (Generalised width, representing total space per m of road dedicated to car-related land use).
// delta sets how much more land bigger roads take (parking, setbacks, roadside uses): an arterial at I = 0.5 takes 2^delta times a road at I = 1.
//   Land consumed by each link:   w_ref * length[l] * I[l]^(-delta)   (split half to each end node)
//   All links:         w_ref * sum over links of (length[l] * I[l]^(-delta))  =  share * N * L0
//   Solving:           w_ref = share * N * L0 / sum over links of (length[l] * I[l]^(-delta))
// (N = number of nodes, L0 = parCellLand, the land in one node's cell)

double sumLengthWeighted = 0;
for (Link l : links) sumLengthWeighted += l.parLength * pow(l.varImpedance, -parRoadLandUsageExponent);
if (sumLengthWeighted <= 0) error("Road width calibration failed: links or their poperties are missing");

varRoadWidthRef = parStartingRoadLandShare * gridPoints.size() * parCellLand / sumLengthWeighted;
