use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure DeleteShipment (in itemCode int)
BEGIN
Set @itemID = (Select Item.ID from Item where Item.ItemCode = itemCode);
Set @maxDate = (Select Max(Shipment.ShipmentDate) from Shipment where Shipment.ItemID = @itemID);
delete from Shipment
where Shipment.ItemID = @itemID and Shipment.ShipmentDate = @maxDate;
END;
$$