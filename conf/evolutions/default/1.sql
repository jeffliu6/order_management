# --- First database schema

# --- !Ups

create table vendor (
  id                        bigint not null,
  name                      varchar(255),
  constraint pk_vendor primary key (id))
;

create table entry (
  id                        bigint not null,
  name                      varchar(255),
  start_date                timestamp,
  end_date                  timestamp,
  vendor_id                 bigint,
  constraint pk_entry primary key (id))
;

create sequence vendor_seq start with 1000;

create sequence entry_seq start with 1000;

alter table entry add constraint fk_entry_vendor_1 foreign key (vendor_id) references vendor (id) on delete restrict on update restrict;
create index ix_entry_vendor_1 on entry (vendor_id);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists vendor;

drop table if exists entry;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists vendor_seq;

drop sequence if exists entry_seq;
