use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure UpdateItem (in itemCode int, in itemPrice Decimal(4,2))
BEGIN
update Item
set Item.Price = itemPrice
where Item.ItemCode = itemCode;
END;
$$