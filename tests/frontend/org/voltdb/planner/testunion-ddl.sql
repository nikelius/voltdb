CREATE TABLE T1 (
	A INTEGER NOT NULL,
	DESC VARCHAR(300) NOT NULL,
	F FLOAT
);
PARTITION TABLE T1 ON COLUMN A;

CREATE TABLE T2 (
	B INTEGER NOT NULL
);

CREATE TABLE T3 (
	C INTEGER NOT NULL
);

CREATE TABLE T4 (
	D INTEGER NOT NULL
);
PARTITION TABLE T4 ON COLUMN D;

CREATE TABLE T5 (
	E INTEGER NOT NULL,
	TEXT VARCHAR(5) NOT NULL
);
PARTITION TABLE T5 ON COLUMN E;

CREATE TABLE T6 (
	F INTEGER NOT NULL
);

