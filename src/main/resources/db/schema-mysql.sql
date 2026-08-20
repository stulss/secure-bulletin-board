-- MySQL / MariaDB 초기 스키마
--
-- 운영 프로파일은 ddl-auto: validate 이므로 테이블을 자동으로 만들지 않는다.
-- 배포 전에 이 스크립트를 DB에 한 번 실행할 것. (docs/05_배포.md 참고)
--
-- 이 파일은 손으로 쓴 것이 아니라 엔티티에서 Hibernate가 생성한 DDL이다.
-- 엔티티를 고쳤다면 이 파일도 다시 생성해야 validate 가 통과한다. (생성 방법은 docs/05_배포.md)

create table users (
    id bigint not null auto_increment,
    username varchar(50) not null,
    email varchar(100) not null,
    password varchar(100) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create table posts (
    id bigint not null auto_increment,
    user_id bigint not null,
    title varchar(200) not null,
    content TEXT not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

alter table users add constraint idx_username unique (username);
alter table users add constraint idx_email unique (email);

create index idx_post_user on posts (user_id);
create index idx_post_created_at on posts (created_at);

-- 존재하지 않는 사용자의 글이 생기지 않도록 DB 레벨에서도 막는다
alter table posts
    add constraint fk_posts_user
    foreign key (user_id)
    references users (id);
