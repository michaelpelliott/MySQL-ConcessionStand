use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure DeleteItem (in itemCode int)
BEGIN
delete from Item
where Item.ItemCode = itemCode;
END;
$$