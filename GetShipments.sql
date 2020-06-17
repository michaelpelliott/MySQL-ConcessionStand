use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure GetShipments(in code int)
BEGIN
set @itemId = (select ID from Item where code = ItemCode);
select * from Shipment
where ItemID = @itemId;
END;
$$