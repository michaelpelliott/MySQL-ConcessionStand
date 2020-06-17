use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure CreatePurchase (in code int, in qty int)
BEGIN
set @itemId = (select ID from Item where ItemCode = code);
insert into Purchase (ItemID, Quantity)
select @itemId, qty;
END;
$$