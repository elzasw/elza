-- Musi se spustit nad vychozi databazi postgres.
-- Ocekava se existence uzivatelu #1 a #2.

-- Parametry:
-- <1> - nazev DB
-- <2> - uzivatel #1 (aplikacni schema DB)
-- <3> - uzivatel #2 (historizacni schema DB)

-- Zalozi databazi.
CREATE DATABASE <1>
  WITH OWNER = DEFAULT
       ENCODING = 'UTF8'
       TABLESPACE = DEFAULT
       LC_COLLATE = 'Czech_Czech Republic.1250'
       LC_CTYPE = 'Czech_Czech Republic.1250'
       CONNECTION LIMIT = -1;

-- Prida uzivateli #1 pravo na aplikacni (public) schema DB.
ALTER ROLE <2> IN DATABASE <1>
  SET search_path = public;

-- Prida uzivateli #2 pravo na historizacni (hist) schema DB.
ALTER ROLE <3> IN DATABASE <1>
  SET search_path = hist;
