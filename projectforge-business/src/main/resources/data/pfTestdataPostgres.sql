-- We should increment the start values of the sequences, otherwise unique constraint violations will occur
-- after starting new databases pre-filled with test data.

SELECT pg_catalog.setval('hibernate_sequence', 1000, TRUE);
