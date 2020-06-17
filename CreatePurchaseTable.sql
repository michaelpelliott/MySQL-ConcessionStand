use BasicInventorySystem;
create table Purchase(
ID int auto_increment,
ItemID int not null,
Quantity int not null,
PurchaseDate datetime default current_timestamp,
primary key(ID),
foreign key (ItemID) references Item (ID)
);