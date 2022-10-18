// Gramatika pro 'datace.js'. Slouzi pro validaci datace.

Expression = IntervalEstimate / Interval / InstantEstimate

IntervalEstimate = a:Instant "/" b:Instant { return {from: {...a, estimate: true}, to: {...b, estimate: true}}}

Interval = a:InstantEstimate [-] b:InstantEstimate { return {from: {...a}, to: {...b}}}

InstantEstimate = Estimate / Instant

Estimate = EstimateParen / EstimateBracket

EstimateParen = "(" instant:Instant ")" { return {...instant, estimate: true}}

EstimateBracket = "[" instant:Instant "]"  { return {...instant, estimate: true}}

Instant = BcRawInstant / NoBcRawInstant

BcRawInstant = "bc " r:RawInstant { return {...r, bc: true}}

NoBcRawInstant = r:RawInstant { return {...r, bc: false}}

RawInstant = DateTime / Date / MonthYear / Century / Year

Century = century:Number ( ". st." / ".st." / "st") { return {c: century}}

Year = year:Number { return {y: year} }

MonthYear = month:Month "." year:Year { return {...month, ...year} }

Date = day:Day "." month:Month "." year:Year { return {...day, ...month, ...year} }

DateTime = date:Date " " time:Time { return {...date, ...time} }

Time =  LongTime / ShortTime

LongTime = hour:Hour ":" minute:Minute ":" second:Second { return {...hour, ...minute, ...second} }

ShortTime = hour:Hour ":" minute:Minute { return {...hour, ...minute} }

Number = UnsignedInteger

Month = month:UnsignedInteger { return {M: month} }

Day = day:UnsignedInteger { return {d: day} }

Hour = hour:UnsignedInteger { return {h: hour} }

Minute = minute:UnsignedInteger { return {m: minute} }

Second = second:UnsignedInteger { return {s: second} }

UnsignedInteger = [0-9]+ { return parseInt(text(), 10); }
