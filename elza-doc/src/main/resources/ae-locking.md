Archival Entity Locking
========================

Archival entity has to be explicitly locked to be modified. 
Lockes are acquired before any change happen at the beginning
of the transaction. Object ApAccessPoint is used for locking.

To lock an Access Point use method in AccessPointService:
void lockWrite(ApAccessPoint ap);

