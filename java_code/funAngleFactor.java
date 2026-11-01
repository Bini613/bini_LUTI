// Function in Main. Returns value, type: double. Arguments: n (GridPoint), other (GridPoint)
// Probability multiplier at node n for a new link n-other, based on the turn it makes from n's single existing link.
// Only chains of links care about angles: nodes with 0 or 2+ links return 1.
if (n.colLinks.size() != 1) return 1;
Link e = n.colLinks.get(0);
GridPoint m = (e.parNodeA == n) ? e.parNodeB : e.parNodeA;   // node at the far end of the existing link
// Dot product of the two directions, both pointing out from n
int dot = (m.parXIndex - n.parXIndex) * (other.parXIndex - n.parXIndex)
        + (m.parYIndex - n.parYIndex) * (other.parYIndex - n.parYIndex);
if (dot < 0) return 1;                        // straight on or 45 degree turn
if (dot == 0) return parSharpTurnBias;        // right-angle turn
return parSharpTurnBias * parSharpTurnBias;   // acute angle (worst)
