create table "pdm"."Monitoring" ("ID" bigint not null, "cadenceDescription" varchar(255), "numberOfEpochs" integer, primary key ("ID"));
alter table if exists "pdm"."Observation" add column "monitoring_ID" bigint;
alter table if exists "pdm"."Observation" drop constraint if exists "UK8t5lfwvoctdshbr065imkjnub";
alter table if exists "pdm"."Observation" add constraint "UK8t5lfwvoctdshbr065imkjnub" unique ("monitoring_ID");
create sequence "Monitoring_SEQ" start with 1 increment by 50;
alter table if exists "pdm"."Observation" add constraint "FKq72g71r0dr2ici50atj7xcaxs" foreign key ("monitoring_ID") references "pdm"."Monitoring";
