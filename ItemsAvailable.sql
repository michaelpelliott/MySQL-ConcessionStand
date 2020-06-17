use BasicInventorySystem;
Delimiter $$                                                 
Create Procedure ItemsAvailable (in itemCode int, in bool TinyInt)
BEGIN
Select Item.ItemCode, Item.ItemDescription, 
(ifnull((Select Sum(Shipment.Quantity) from Shipment where Shipment.ItemID = Item.ID),0) -
ifnull((Select Sum(Purchase.Quantity) from Purchase where Purchase.ItemID = Item.ID),0)) as Quantity
From Item
where Item.ItemCode = itemCode or bool != 0;
END;
$$