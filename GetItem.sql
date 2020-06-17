use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure GetItem(in code int)
BEGIN
select * from Item
where ItemCode = code;
END;
$$