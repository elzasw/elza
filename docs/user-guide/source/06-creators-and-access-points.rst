Creators and access points
==========================

Entities instead of names
-------------------------

ISAD(G) names the creator of a unit (3.2.1) and expects the description to
be indexed by names, places and subjects. In ELZA these are not texts
typed into the description but **archival entities**: persons, families,
corporate bodies, places and concepts described once, in a scope of
entities, with ISAAR(CPF) (part II of this guide). A unit of description
refers to them; the finding aid prints their authorized names and the
index is built from them.

The entities come from scopes that use the rule set *ISAAR(CPF) entity
description*. When you fill a creator or an access point, you search the
entities of the scopes your fonds can use and pick one, or create a new
entity on the spot.

Creators
--------

*Creator* (3.2.1) refers to a person, a family or a corporate body. It is
required on the fonds and possible, and repeatable, at every level below
it - a series created by a department of the corporate body names that
department as its creator. The history of the creator belongs to the
entity (its description with ISAAR(CPF)); *Administrative / biographical
history* on the unit is for the cases where the history is kept with the
description.

Archivist
---------

*Archivist* (3.7.1) refers to the person who prepared the description,
also an entity; the text of the archivist's note stays in
*Archivist's note*.

Access points
-------------

*Access point* refers to any entity and says in which **role** the unit is
indexed by it:

===================  ==============================================================
Role                 Entity
===================  ==============================================================
Subject              a concept
Place                a place
Person               a person the unit is about or relates to
Family               a family the unit is about or relates to
Corporate body       a corporate body the unit is about or relates to
Author               a person, family or corporate body that authored the
                     material without being its creator
Other role           any entity in another role
===================  ==============================================================

Access points are repeatable and possible at every level. The creator is
not an access point: the finding aid indexes the creators by themselves.

Digital objects
---------------

*Digital object* holds a link to a digital copy or a born-digital object
of the unit. It is a plain link; the integration of ELZA with digital
archives (AIPs, digital repositories) is described in the administration
guide and is independent of the rule set.
