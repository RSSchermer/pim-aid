# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "DRUGS" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"userInput" VARCHAR(254) NOT NULL,"userToken" VARCHAR(254) NOT NULL,"resolved_medication_product_id" BIGINT);
create table "DRUG_GROUPS" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL);
create unique index "DRUG_GROUPS_NAME_INDEX" on "DRUG_GROUPS" ("name");
create table "DRUG_GROUPS_GENERIC_TYPES" ("drug_group_id" BIGINT NOT NULL,"generic_type_id" BIGINT NOT NULL);
alter table "DRUG_GROUPS_GENERIC_TYPES" add constraint "DRUG_GROUPS_GENERIC_TYPES_PK" primary key("drug_group_id","generic_type_id");
create table "EXPRESSION_TERMS" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"label" VARCHAR(254) NOT NULL,"drug_type_id" BIGINT,"drug_group_id" BIGINT,"statement_template" VARCHAR(254),"display_condition" VARCHAR(254),"comparison_operator" VARCHAR(254),"age" INTEGER);
create unique index "EXPRESSION_TERMS_LABEL_INDEX" on "EXPRESSION_TERMS" ("label");
create table "EXPRESSION_TERMS_RULES" ("expression_term_id" BIGINT NOT NULL,"rule_id" BIGINT NOT NULL);
alter table "EXPRESSION_TERMS_RULES" add constraint "EXPRESSION_TERMS_RULES_PK" primary key("expression_term_id","rule_id");
create table "EXPRESSION_TERMS_STATEMENT_TERMS" ("expression_term_id" BIGINT NOT NULL,"statement_term_id" BIGINT NOT NULL);
alter table "EXPRESSION_TERMS_STATEMENT_TERMS" add constraint "EXPRESSION_TERMS_STATEMENT_TERMS_PK" primary key("expression_term_id","statement_term_id");
create table "GENERIC_TYPES" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL);
create unique index "GENERIC_TYPES_NAME_INDEX" on "GENERIC_TYPES" ("name");
create table "GENERIC_TYPES_MEDICATION_PRODUCT" ("generic_type_id" BIGINT NOT NULL,"medication_product_id" BIGINT NOT NULL);
alter table "GENERIC_TYPES_MEDICATION_PRODUCT" add constraint "GENERIC_TYPES_MEDICATION_PRODUCT_PK" primary key("medication_product_id","generic_type_id");
create table "MEDICATION_PRODUCTS" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL);
create unique index "MEDICATION_PRODUCTS_NAME_INDEX" on "MEDICATION_PRODUCTS" ("name");
create table "RULES" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL,"condition_expression" VARCHAR(254) NOT NULL,"source" VARCHAR(254),"formalization_reference" VARCHAR(254),"note" VARCHAR(254));
create unique index "RULES_NAME_INDEX" on "RULES" ("name");
create table "RULES_SUGGESTION_TEMPLATES" ("rule_id" BIGINT NOT NULL,"suggestion_id" BIGINT NOT NULL);
alter table "RULES_SUGGESTION_TEMPLATES" add constraint "RULES_SUGGESTION_TEMPLATES_PK" primary key("rule_id","suggestion_id");
create table "STATEMENT_TERMS_USER_SESSIONS" ("user_session_token" VARCHAR(254) NOT NULL,"statement_term_id" BIGINT NOT NULL,"text" VARCHAR(254) NOT NULL,"conditional" BOOLEAN DEFAULT false NOT NULL);
alter table "STATEMENT_TERMS_USER_SESSIONS" add constraint "STATEMENT_TERMS_USER_SESSIONS_PK" primary key("user_session_token","statement_term_id","text","conditional");
create table "SUGGESTION_TEMPLATES" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR(254) NOT NULL,"text" VARCHAR(254) NOT NULL,"explanatory_note" VARCHAR(254));
create unique index "SUGGESTION_TEMPLATES_NAME_INDEX" on "SUGGESTION_TEMPLATES" ("name");
create table "USER_SESSIONS" ("token" VARCHAR(254) NOT NULL PRIMARY KEY,"age" INTEGER);
alter table "DRUGS" add constraint "DRUGS_RESOLVED_MEDICATION_PRODUCT_FK" foreign key("resolved_medication_product_id") references "MEDICATION_PRODUCTS"("id") on update NO ACTION on delete NO ACTION;
alter table "DRUG_GROUPS_GENERIC_TYPES" add constraint "DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK" foreign key("drug_group_id") references "DRUG_GROUPS"("id") on update NO ACTION on delete CASCADE;
alter table "DRUG_GROUPS_GENERIC_TYPES" add constraint "DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK" foreign key("generic_type_id") references "GENERIC_TYPES"("id") on update NO ACTION on delete CASCADE;
alter table "EXPRESSION_TERMS" add constraint "EXPRESSION_TERMS_DRUG_GROUP_FK" foreign key("drug_group_id") references "DRUG_GROUPS"("id") on update NO ACTION on delete NO ACTION;
alter table "EXPRESSION_TERMS" add constraint "EXPRESSION_TERMS_DRUG_TYPE_FK" foreign key("drug_type_id") references "GENERIC_TYPES"("id") on update NO ACTION on delete NO ACTION;
alter table "EXPRESSION_TERMS_RULES" add constraint "EXPRESSION_TERMS_RULES_EXPRESSION_TERM_FK" foreign key("expression_term_id") references "EXPRESSION_TERMS"("id") on update NO ACTION on delete NO ACTION;
alter table "EXPRESSION_TERMS_RULES" add constraint "EXPRESSION_TERMS_RULES_RULE_FK" foreign key("rule_id") references "RULES"("id") on update NO ACTION on delete CASCADE;
alter table "EXPRESSION_TERMS_STATEMENT_TERMS" add constraint "EXPRESSION_TERMS_STATEMENT_TERMS_EXPRESSION_TERM_FK" foreign key("expression_term_id") references "EXPRESSION_TERMS"("id") on update NO ACTION on delete NO ACTION;
alter table "EXPRESSION_TERMS_STATEMENT_TERMS" add constraint "EXPRESSION_TERMS_STATEMENT_TERMS_STATEMENT_TERM_FK" foreign key("statement_term_id") references "EXPRESSION_TERMS"("id") on update NO ACTION on delete CASCADE;
alter table "GENERIC_TYPES_MEDICATION_PRODUCT" add constraint "GENERIC_TYPES_MEDICATION_PRODUCT_GENERIC_TYPE_FK" foreign key("generic_type_id") references "GENERIC_TYPES"("id") on update NO ACTION on delete CASCADE;
alter table "GENERIC_TYPES_MEDICATION_PRODUCT" add constraint "GENERIC_TYPES_MEDICATION_PRODUCT_MEDICATION_PRODUCT_FK" foreign key("medication_product_id") references "MEDICATION_PRODUCTS"("id") on update NO ACTION on delete CASCADE;
alter table "RULES_SUGGESTION_TEMPLATES" add constraint "RULES_SUGGESTION_TEMPLATES_RULE_FK" foreign key("rule_id") references "RULES"("id") on update NO ACTION on delete CASCADE;
alter table "RULES_SUGGESTION_TEMPLATES" add constraint "RULES_SUGGESTION_TEMPLATES_SUGGESTION_TEMPLATE_FK" foreign key("suggestion_id") references "SUGGESTION_TEMPLATES"("id") on update NO ACTION on delete NO ACTION;
alter table "STATEMENT_TERMS_USER_SESSIONS" add constraint "STATEMENT_TERMS_USER_SESSIONS_STATEMENT_TERM_FK" foreign key("statement_term_id") references "EXPRESSION_TERMS"("id") on update NO ACTION on delete NO ACTION;
alter table "STATEMENT_TERMS_USER_SESSIONS" add constraint "STATEMENT_TERMS_USER_SESSIONS_USER_SESSION_FK" foreign key("user_session_token") references "USER_SESSIONS"("token") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "STATEMENT_TERMS_USER_SESSIONS" drop constraint "STATEMENT_TERMS_USER_SESSIONS_STATEMENT_TERM_FK";
alter table "STATEMENT_TERMS_USER_SESSIONS" drop constraint "STATEMENT_TERMS_USER_SESSIONS_USER_SESSION_FK";
alter table "RULES_SUGGESTION_TEMPLATES" drop constraint "RULES_SUGGESTION_TEMPLATES_RULE_FK";
alter table "RULES_SUGGESTION_TEMPLATES" drop constraint "RULES_SUGGESTION_TEMPLATES_SUGGESTION_TEMPLATE_FK";
alter table "GENERIC_TYPES_MEDICATION_PRODUCT" drop constraint "GENERIC_TYPES_MEDICATION_PRODUCT_GENERIC_TYPE_FK";
alter table "GENERIC_TYPES_MEDICATION_PRODUCT" drop constraint "GENERIC_TYPES_MEDICATION_PRODUCT_MEDICATION_PRODUCT_FK";
alter table "EXPRESSION_TERMS_STATEMENT_TERMS" drop constraint "EXPRESSION_TERMS_STATEMENT_TERMS_EXPRESSION_TERM_FK";
alter table "EXPRESSION_TERMS_STATEMENT_TERMS" drop constraint "EXPRESSION_TERMS_STATEMENT_TERMS_STATEMENT_TERM_FK";
alter table "EXPRESSION_TERMS_RULES" drop constraint "EXPRESSION_TERMS_RULES_EXPRESSION_TERM_FK";
alter table "EXPRESSION_TERMS_RULES" drop constraint "EXPRESSION_TERMS_RULES_RULE_FK";
alter table "EXPRESSION_TERMS" drop constraint "EXPRESSION_TERMS_DRUG_GROUP_FK";
alter table "EXPRESSION_TERMS" drop constraint "EXPRESSION_TERMS_DRUG_TYPE_FK";
alter table "DRUG_GROUPS_GENERIC_TYPES" drop constraint "DRUG_GROUPS_GENERIC_TYPES_DRUG_GROUP_FK";
alter table "DRUG_GROUPS_GENERIC_TYPES" drop constraint "DRUG_GROUPS_GENERIC_TYPES_GENERIC_TYPE_FK";
alter table "DRUGS" drop constraint "DRUGS_RESOLVED_MEDICATION_PRODUCT_FK";
drop table "USER_SESSIONS";
drop table "SUGGESTION_TEMPLATES";
alter table "STATEMENT_TERMS_USER_SESSIONS" drop constraint "STATEMENT_TERMS_USER_SESSIONS_PK";
drop table "STATEMENT_TERMS_USER_SESSIONS";
alter table "RULES_SUGGESTION_TEMPLATES" drop constraint "RULES_SUGGESTION_TEMPLATES_PK";
drop table "RULES_SUGGESTION_TEMPLATES";
drop table "RULES";
drop table "MEDICATION_PRODUCTS";
alter table "GENERIC_TYPES_MEDICATION_PRODUCT" drop constraint "GENERIC_TYPES_MEDICATION_PRODUCT_PK";
drop table "GENERIC_TYPES_MEDICATION_PRODUCT";
drop table "GENERIC_TYPES";
alter table "EXPRESSION_TERMS_STATEMENT_TERMS" drop constraint "EXPRESSION_TERMS_STATEMENT_TERMS_PK";
drop table "EXPRESSION_TERMS_STATEMENT_TERMS";
alter table "EXPRESSION_TERMS_RULES" drop constraint "EXPRESSION_TERMS_RULES_PK";
drop table "EXPRESSION_TERMS_RULES";
drop table "EXPRESSION_TERMS";
alter table "DRUG_GROUPS_GENERIC_TYPES" drop constraint "DRUG_GROUPS_GENERIC_TYPES_PK";
drop table "DRUG_GROUPS_GENERIC_TYPES";
drop table "DRUG_GROUPS";
drop table "DRUGS";

