# --- First database schema

# --- !Ups

create table dept (
  id                        bigint not null,
  name                      varchar(255),
  constraint pk_dept primary key (id))
;

create table vendor (
  id                        bigint not null,
  name                      varchar(255),
  url                       varchar(255),
  constraint pk_vendor primary key (id))
;

create table entry (
  id                        bigint not null,
  name                      varchar(255),
  dept_id                   bigint,
  request_date              timestamp,
  start_date                timestamp,
  end_date                  timestamp,
  vendor_id                 bigint,
  constraint pk_entry primary key (id))
;

create sequence vendor_seq start with 1000;

create sequence entry_seq start with 1000;

create sequence dept_seq start with 1000;

alter table entry add constraint fk_entry_vendor_1 foreign key (vendor_id) references vendor (id) on delete restrict on update restrict;
create index ix_entry_vendor_1 on entry (vendor_id);

alter table entry add constraint fk_entry_dept_1 foreign key (dept_id) references dept (id) on delete restrict on update restrict;
create index ix_entry_dept_1 on entry (dept_id);

# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists vendor;

drop table if exists entry;

drop table if exists dept;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists vendor_seq;

drop sequence if exists entry_seq;

drop sequence if exists dept_seq;