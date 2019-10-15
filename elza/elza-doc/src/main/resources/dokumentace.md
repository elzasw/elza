Formát datace
Uživatel do textového pole zadává

století ve formátu X.st.
rok ve formátu YYYY
měsíc ve formátu YYYY/MM
den ve formátu DD.MM.YYYY
den včetně času ve formátu DD.MM.YYYY HH:mm
Je umožněno zadat

jednu hodnotu
dvě hodnoty oddělené pomlčkou v případě intervalu
pokud před pomlčkou nebo za pomlčkou není zadaná hodnota, pak je období bráno jako neuzavřené, např. do roku 1956
V případě, že se jedná o odhad, je hodnota uzavřena v kulatých závorkách.

Příklady:
"19.st." -> v systému se uloží jako 1801-01-01T00:00:00 - 1900-12-31T23:59:59
"1956" -> v systému se uloží jako 1956-01-01T00:00:00 - 1956-12-31T23:59:59
"1956/07" -> v systému se uloží jako 1956-07-01T00:00:00 - 1956-07-31T23:59:59
"1.7.1956" -> v systému se uloží jako 1956-07-01T00:00:00 - 1956-07-01T23:59:59
"1.7.1956 12:20" -> v systému se uloží jako 1956-07-01T12:20:00 - 1956-07-01T12:20:59
"1956-1960/08" -> v systému se uloží jako 1956-01-01T00:00:00 - 1960-08-31T23:59:59
"1.7.1956-" -> v systému se uloží jako 1956-07-01T00:00:00 - null
"-1.7.1956" -> v systému se uloží jako null - 1956-07-01T23:59:59
"(1.7.1956)" -> v systému se uloží jako 1956-07-01T00:00:00 - 1956-07-01T23:59:59 a označí se, že se jedná o přibližnou hodnotu pro celý interval
"(1.7.1956)-1.8.1956" -> v systému se uloží jako 1956-07-01T00:00:00 - 1956-08-01T00:00:00 a první hodnota se označí jako přibližná
