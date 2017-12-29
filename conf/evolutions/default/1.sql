# --- First database schema

# --- !Ups

create table company (
  id                        bigint not null,
  name                      varchar(255),
  constraint pk_company primary key (id))
;

create table entry (
  id                        bigint not null,
  name                      varchar(255),
  start_date                timestamp,
  end_date                  timestamp,
  company_id                bigint,
  constraint pk_entry primary key (id))
;

create sequence company_seq start with 1000;

create sequence entry_seq start with 1000;

alter table entry add constraint fk_entry_company_1 foreign key (company_id) references company (id) on delete restrict on update restrict;
create index ix_entry_company_1 on entry (company_id);


# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists company;

drop table if exists entry;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists company_seq;

drop sequence if exists entry_seq;
