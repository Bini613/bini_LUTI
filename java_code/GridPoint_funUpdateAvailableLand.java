// Function in GridPoint. Just action (returns nothing). No arguments.
// Sets varAvailableLand: the cell's land minus half the land of every link touching this node.
// Call it on a node whenever one of its links changes impedance (investment), after varRoadWidthRef is calibrated.
//   L_k = L0 - sum over links at k of: 0.5 * w_ref * length[l] / I[l],  floored at parMinAvailableLand
double roadLand = 0;
for (Link l : colLinks) roadLand += 0.5 * main.varRoadWidthRef * l.parLength / l.varImpedance;
varAvailableLand = max(main.parMinAvailableLand, main.parCellLand - roadLand);
