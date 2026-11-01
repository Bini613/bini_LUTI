// Function in Main. Returns value, type: Link. Arguments: a (GridPoint), b (GridPoint)
// Returns the link between a and b, or null if there is none
for (Link l : a.colLinks) {
    if (l.parNodeA == b || l.parNodeB == b) return l;
}
return null;