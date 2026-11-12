// Function in Main. Just action (returns nothing). Arguments: l (Link)
// Unregisters link l from both its nodes and deletes it (the reverse of funAddLink)
l.parNodeA.colLinks.remove(l);
l.parNodeB.colLinks.remove(l);
remove_links(l);
