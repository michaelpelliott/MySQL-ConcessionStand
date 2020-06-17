use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure CreateShipment (in code varchar(10), in qty int, in date date)
BEGIN
set @itemId = ifnull((select ID from Item where ItemCode = code), -1);
insert into Shipment (ItemID, Quantity, ShipmentDate)
select @itemId, qty, date;
END;
$$