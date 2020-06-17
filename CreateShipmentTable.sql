use BasicInventorySystem;
create table Shipment(
ID int auto_increment,
ItemID int not null,
Quantity int not null,
ShipmentDate date not null unique,
primary key(ID),
foreign key (ItemID) references Item (ID)
);