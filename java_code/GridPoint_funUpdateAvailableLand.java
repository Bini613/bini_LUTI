// Function in GridPoint. Just action (returns nothing). No arguments.
// Sets varAvailableLand: the cell's land minus half the land of every link touching this node.
// Call it on a node whenever one of its links changes impedance (investment).
//   L_k = L0 - sum over links at k of: their land at each end node (Link funNodeRoadLand), floored at parMinAvailableLand
double roadLand = 0;
for (Link l : colLinks) roadLand += l.funNodeRoadLand();
varAvailableLand = max(main.parMinAvailableLand, main.parCellLand - roadLand);
