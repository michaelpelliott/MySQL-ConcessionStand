use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure CreateItem (in code varchar(10), in description varchar(50), in itemPrice decimal(4,2))
BEGIN
insert into Item (ItemCode, ItemDescription, Price)
select code, description, itemPrice;
END;
$$