-- [N]:flyway - Data file

INSERT INTO PERSON (EMAIL, NAME) VALUES ('info@hjolystratos.net', 'Duke');
INSERT INTO PERSON (EMAIL, NAME) VALUES ('tom@hjolystratos.net', 'Tom');
INSERT INTO PERSON (EMAIL, NAME) VALUES ('bjoern@hjolystratos.net', 'Bjoern');
INSERT INTO PERSON (EMAIL, NAME) VALUES ('philip@hjolystratos.net', 'Philip');

INSERT INTO TODO (TITLE, DUE_DATE, STATUS, PRIORITY, OWNER_ID) VALUES ('Setup infrastructure in AWS', '2020-12-31', 'OPEN', 1, 1);
INSERT INTO TODO (TITLE, DUE_DATE, STATUS, PRIORITY, OWNER_ID) VALUES ('Secure application', '2020-12-31', 'OPEN', 1, 1);
INSERT INTO TODO (TITLE, DUE_DATE, STATUS, PRIORITY, OWNER_ID) VALUES ('Write book', '2020-12-31', 'OPEN', 1, 1);
INSERT INTO TODO (TITLE, DUE_DATE, STATUS, PRIORITY, OWNER_ID) VALUES ('Release it', '2020-12-31', 'OPEN', 1, 1);
