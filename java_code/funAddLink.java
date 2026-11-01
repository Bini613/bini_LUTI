// Function in Main. Just action (returns nothing). Arguments: a (GridPoint), b (GridPoint)
// Creates a link between a and b and registers it with both nodes
Link l = add_links(a, b, 0);   // 0 = parLength placeholder; Link's On startup overwrites it
l.setXY(a.getX(), a.getY());
a.colLinks.add(l);
b.colLinks.add(l);
