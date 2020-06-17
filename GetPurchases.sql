use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure GetPurchases(in code int)
BEGIN
set @itemId = ifnull((select ID from Item where code = ItemCode), -1);
select * from Purchase
where ItemID = @itemId;
END;
$$