//Factor for discouring links forming further away from city centre, using a bell-curve function

double mx = (a.parXIndex + b.parXIndex)/2;
double my = (a.parYIndex + b.parYIndex)/2;
double dx = mx - parCols/2;
double dy = my - parCols/2;
double stepsFromCentre = sqrt(dx*dx + dy*dy);
double z = stepsFromCentre * parRadialRoadDispersion ;
return exp(-0.5 * z * z);
