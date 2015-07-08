# --- !Ups

alter table "DRUGS" add constraint "DRUGS_USER_SESSION_FK" foreign key("userToken") references "USER_SESSIONS"("token") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "DRUGS" drop constraint "DRUGS_USER_SESSION_FK";
