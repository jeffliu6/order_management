# --- Sample dataset

# --- !Ups

insert into vendor (id,name,url) values (  1,'Apple Inc.','apple.com');
insert into vendor (id,name,url) values (  2,'Dell','dell.com');
insert into vendor (id,name,url) values (  3,'Cornell University','cornell.edu');
insert into vendor (id,name,url) values (  4,'Google Inc','google.com');
insert into vendor (id,name,url) values (  5,'King Corporation','poodlesrus.org');


insert into entry (id,name,start_date,end_date,vendor_id) values (  1,'Order One',null,null,1);
insert into entry (id,name,start_date,end_date,vendor_id) values (  2,'Order Two',null,null,2);
insert into entry (id,name,start_date,end_date,vendor_id) values (  3,'Order Three',null,null,2);
insert into entry (id,name,start_date,end_date,vendor_id) values (  4,'Order Four',null,null,2);
insert into entry (id,name,start_date,end_date,vendor_id) values (  5,'Order Five','1991-01-01',null,2);
insert into entry (id,name,start_date,end_date,vendor_id) values (  6,'Order Six','2006-01-10',null,3);
insert into entry (id,name,start_date,end_date,vendor_id) values (  7,'Order Seven',null,null,4);
insert into entry (id,name,start_date,end_date,vendor_id) values (  8,'Order Eight',null,null,5);
insert into entry (id,name,start_date,end_date,vendor_id) values (  9,'Order 0',null,null,1);

# --- !Downs

delete from entry;
delete from vendor;
