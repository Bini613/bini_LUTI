// Function in Main. Returns value, type: double. Arguments: n (GridPoint)
// Probability multiplier at one end (node n) of a potential new link, based on how many links n already has
int d = n.colLinks.size();
if (d == 0) return parConnectToNakedNodeBias;   // new link would make n a dead end
return max(0.0, 1 - (1 - parJunctionFormationBias) * (d - 1));   // 1 for d=1 (extending a road), falling with each extra link
