-- Musi se pustit nad konkretni databazi.

-- Zalozi v DB historizacni (hist) schema.
CREATE SCHEMA hist AUTHORIZATION postgres;
GRANT ALL ON SCHEMA hist TO postgres;
