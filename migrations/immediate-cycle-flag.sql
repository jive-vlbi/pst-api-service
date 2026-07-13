alter table if exists "pdm"."ProposalCycle" add column "isImmediate" boolean not null default false;
update "pdm"."ProposalCycle" set "isImmediate" = ("submissionDeadline" is null);
alter table "pdm"."ProposalCycle" alter column "isImmediate" drop default;
