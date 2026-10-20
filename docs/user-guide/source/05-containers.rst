Containers
==========

What a container is
-------------------

A container is the storage unit the material is kept in: a box, a folder,
a volume, an envelope, a tube. The rule set records containers as
**structured objects** of the fonds: each container exists once, carries
its kind, its number and where it is kept, and can be assigned to any
number of units of description. The element *Container* on a unit refers
to one of them; a unit kept in several containers has the element several
times.

A unit inherits the container of the units above it: a file in box 12
does not need the element on each of its items, and a one-box collection
carries its container on the root. ISAD(G) has no container element and
many archives keep the location outside the finding aid, so containers
are not demanded. The validation reports only a file or an item at the
end of a branch that has no container, own or inherited, while a
neighbouring unit has one (:doc:`08-validation`).

The value of a container
------------------------

A container is shown by its kind and number, with an optional prefix and
postfix: *box 12*, *folder 3a*, *vol. II/7*. The kind is a vocabulary
(box, folder, volume, envelope, tube, other); the number is a whole
number; the prefix and the postfix are free text. Containers sort by
their kind, prefix and number, so *box 2* comes before *box 12*.

*Location* records where the container is kept - the depot, the row, the
shelf - as text; it is shown next to the value.

Working with containers
-----------------------

Containers are managed in the panel of structured objects of the fonds,
where they are created, edited, searched and exported to CSV. When you
add the element *Container* to a unit, you choose an existing container
or type a new value: a typed value such as ``A12b`` is split into the
prefix ``A``, the number ``12`` and the postfix ``b``, and the container
is created with it; choose its kind afterwards.

The kinds of container and the rules of their numbering are a fixed part
of the rule set; an installation that needs other kinds extends the rule
set with an addon package (implementation guide).

.. todo::

   The generator of containers - a bulk action that creates a container
   of a chosen kind for every file or item of a branch - and the labels
   printed for containers belong to a later step of the rule set.
