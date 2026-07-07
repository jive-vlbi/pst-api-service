alter table if exists "pdm"."AbstractProposal" add column "aiAcknowledged" boolean not null default true;

alter table if exists "pdm"."AbstractProposal" alter column "aiAcknowledged" drop default;
