Archival Entity Locking
========================

Archival entity has to be explicitly locked before any change. 
Lockes are acquired before any change happen at the beginning
of the transaction. Object ApAccessPoint is used for locking.



TODO - refactorize:

synchronizeAccessPointsForExternalSystem 
 - first all modified APs have to be locked
