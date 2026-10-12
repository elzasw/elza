Outputs
=======

The finding aid
---------------

A finding aid is an **output** of the fonds: a selection of its units with
their description, generated into a file. The rule set defines one kind
of output, *Finding aid*, with its own items filled on the output before
it is generated:

- *Title of the finding aid*, *Kind of finding aid* (inventory,
  catalogue, guide, list) and *Date of the finding aid* - required;
- *Language of the finding aid* - required, several possible;
- *Compiled by* - recommended;
- *Note to the finding aid* and *Attachment* - possible.

Formats
-------

Version 1 generates the finding aid as **XML in the data exchange format
of ELZA**, the format the import of a fonds reads, so that a description
can move between installations.

.. todo::

   The EAD3 finding aid (the international exchange format of archival
   description, validated against the EAD3 schema) and the PDF finding
   aid are the next steps of the rule set. This chapter will describe
   their layout, the statistics of the finding aid computed from the
   quantities of the units, and the index built from the creators and
   the access points.

Bulk actions
------------

The actions of the rule set run on a selected branch or on the whole
fonds:

- *Validation of the description* (:doc:`08-validation`);
- *Generator of reference codes* (:doc:`03-levels`);
- *Persistent sort* - sorts the units of a branch by a chosen element,
  permanently, for example the files of a series by their dates; also in
  the context menu of a unit in the tree.
