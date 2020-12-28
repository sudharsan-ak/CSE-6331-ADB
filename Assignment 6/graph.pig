A= LOAD '$G' USING PigStorage(',') AS (x:int, y:int);
B= group A by x;
C= foreach B generate group,COUNT(A) as new;
D = group C by new;
Final= foreach D generate group,COUNT(C);
STORE Final INTO '$O' USING PigStorage (',');