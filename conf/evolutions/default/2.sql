# --- Sample dataset

# --- !Ups

insert into dept (id, name) values (1,'Research');
insert into dept (id, name) values (2,'Analytical');
insert into dept (id, name) values (3,'PD');

insert into vendor (id,name,url) values (  1,'Apple Inc.','apple.com');
insert into vendor (id,name,url) values (  2,'Dell','dell.com');
insert into vendor (id,name,url) values (  3,'Cornell University','cornell.edu');
insert into vendor (id,name,url) values (  4,'Google Inc','google.com');
insert into vendor (id,name,url) values (  5,'King Corporation','poodlesrus.org');

insert into entry (id,name,request_date,dept_id,start_date,end_date,vendor_id) values (  1,'Order One',CURRENT_TIMESTAMP,1,null,null,1);
insert into entry (id,name,request_date,dept_id,start_date,end_date,vendor_id) values (  2,'Order Two',CURRENT_TIMESTAMP,2,null,null,2);
insert into entry (id,name,request_date,dept_id,start_date,end_date,vendor_id) values (  3,'Order Three',CURRENT_TIMESTAMP,3,null,null,2);
insert into entry (id,name,request_date,dept_id,start_date,end_date,vendor_id) values (  4,'Order Four',CURRENT_TIMESTAMP,1,null,null,2);
insert into entry (id,name,request_date,dept_id,start_date,end_date,vendor_id) values (  5,'Order Five',CURRENT_TIMESTAMP,2,'1991-01-01',null,2);

# --- !Downs

delete from entry;
delete from vendor;
delete from dept;
