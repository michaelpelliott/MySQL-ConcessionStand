use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure DeletePurchase (in itemCode int)
BEGIN
Set @itemID = (Select Item.ID from Item where Item.ItemCode = itemCode);
Set @maxDate = (Select Max(Purchase.PurchaseDate) from Purchase where Purchase.ItemID = @itemID);
delete from Purchase
where Purchase.ItemID = @itemID and Purchase.PurchaseDate = @maxDate;
END;
$$