use BasicInventorySystem;
create table Item(
ID int auto_increment,
ItemCode varchar(10) not null unique,
ItemDescription varchar(50),
Price decimal(4,2) default 0,
primary key(ID)
);